package dk.ek.gruppe2.storycontent.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ek.gruppe2.storycontent.api.ApiExceptions.NotFoundException;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.NpcRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.NpcResponse;
import dk.ek.gruppe2.storycontent.domain.Npc;
import dk.ek.gruppe2.storycontent.domain.Race;
import dk.ek.gruppe2.storycontent.domain.Scene;
import dk.ek.gruppe2.storycontent.repository.NpcRepository;
import dk.ek.gruppe2.storycontent.repository.RaceRepository;
import dk.ek.gruppe2.storycontent.repository.SceneRepository;

/**
 * Authoring operations on NPCs.
 *
 * <p>This is where "publish every change a consumer can see" stops being a slogan. Only
 * NPC ids go out in the scene message, so:
 *
 * <ul>
 *   <li>Renaming an NPC changes nothing on the wire and publishes nothing.</li>
 *   <li>Deleting one removes an entry from every scene that contained it, which every
 *       consumer sees, so each of those scenes is republished with a new version.</li>
 * </ul>
 *
 * <p>Placing an NPC in a scene is the scene's operation, not this one, and lives in
 * {@code SceneService#replaceNpcs} for the same reason choices do: the scene is the
 * aggregate that gets published.
 */
@Service
public class NpcService {

    private final NpcRepository npcs;
    private final RaceRepository races;
    private final SceneRepository scenes;
    private final ContentEventDispatcher dispatcher;

    public NpcService(
            NpcRepository npcs,
            RaceRepository races,
            SceneRepository scenes,
            ContentEventDispatcher dispatcher) {
        this.npcs = npcs;
        this.races = races;
        this.scenes = scenes;
        this.dispatcher = dispatcher;
    }

    @Transactional(readOnly = true)
    public List<NpcResponse> findAll() {
        List<NpcResponse> result = new ArrayList<>();
        for (Npc npc : npcs.findAll()) {
            result.add(toResponse(npc));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public NpcResponse findById(Integer id) {
        return toResponse(require(id));
    }

    @Transactional
    public NpcResponse create(NpcRequest request) {
        Npc npc = new Npc(request.name(), requireRace(request.raceId()));
        npcs.save(npc);
        return toResponse(npc);
    }

    /** Publishes nothing: the name is not part of any payload. */
    @Transactional
    public NpcResponse update(Integer id, NpcRequest request) {
        Npc npc = require(id);
        npc.setName(request.name());
        npc.setRace(requireRace(request.raceId()));
        npcs.save(npc);
        return toResponse(npc);
    }

    @Transactional
    public void delete(Integer id) {
        Npc npc = require(id);

        // Collected before anything is removed: once the join rows are gone there is no
        // way to find out which scenes have to be told.
        List<Integer> affectedScenes = npcs.findSceneIdsContaining(id);

        // The scene owns the association, so the join rows have to be removed through the
        // scenes rather than by deleting the NPC and hoping. The flush is explicit because
        // the delete below has to reach the database after them, or the foreign key on
        // scene_npc rejects it.
        for (Integer sceneId : affectedScenes) {
            Scene scene = scenes.findById(sceneId).orElseThrow();
            scene.getNpcs().removeIf(placed -> placed.getId().equals(id));
            scenes.save(scene);
        }
        scenes.flush();

        npcs.delete(npc);

        for (Integer sceneId : affectedScenes) {
            scenes.incrementVersion(sceneId);
            dispatcher.sceneChanged(sceneId);
        }
    }

    private Npc require(Integer id) {
        return npcs.findById(id)
                .orElseThrow(() -> new NotFoundException("NPC " + id + " does not exist"));
    }

    private Race requireRace(Integer id) {
        return races.findById(id)
                .orElseThrow(() -> new NotFoundException("Race " + id + " does not exist"));
    }

    private NpcResponse toResponse(Npc npc) {
        return new NpcResponse(npc.getId(), npc.getName(), npc.getRace().getId());
    }
}
