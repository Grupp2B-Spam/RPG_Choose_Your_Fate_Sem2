package dk.ek.gruppe2.storycontent.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ek.gruppe2.storycontent.api.ApiExceptions.InvalidContentException;
import dk.ek.gruppe2.storycontent.api.ApiExceptions.NotFoundException;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.ChoiceRequest;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.ChoiceResponse;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.EffectRequest;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.EffectResponse;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.RequirementRequest;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.RequirementResponse;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.SceneRequest;
import dk.ek.gruppe2.storycontent.api.dto.SceneDtos.SceneResponse;
import dk.ek.gruppe2.storycontent.domain.Chapter;
import dk.ek.gruppe2.storycontent.domain.Choice;
import dk.ek.gruppe2.storycontent.domain.ChoiceEffect;
import dk.ek.gruppe2.storycontent.domain.ChoiceRequirement;
import dk.ek.gruppe2.storycontent.domain.Enums.SceneStatus;
import dk.ek.gruppe2.storycontent.domain.Npc;
import dk.ek.gruppe2.storycontent.domain.Quest;
import dk.ek.gruppe2.storycontent.domain.Scene;
import dk.ek.gruppe2.storycontent.repository.ChapterRepository;
import dk.ek.gruppe2.storycontent.repository.ItemRepository;
import dk.ek.gruppe2.storycontent.repository.NpcRepository;
import dk.ek.gruppe2.storycontent.repository.QuestRepository;
import dk.ek.gruppe2.storycontent.repository.SceneRepository;

/**
 * Authoring operations on the scene aggregate.
 *
 * <p>Every write here ends with the same two steps, and the order matters: increment the
 * scene's version, then say that the scene changed. The version is what a consumer uses to
 * discard a stale or redelivered message (ADR-006), and it is also what makes the
 * consumers of this event safe to run as competing consumers, which is what makes them
 * scalable (ADR-011). A write that mutates the payload without incrementing the version
 * does not merely lose an update — it produces a message that looks older than it is and
 * is therefore thrown away.
 *
 * <p>The counterpart is just as important and easier to get wrong in the other direction:
 * a change a consumer cannot observe must <em>not</em> increment it. Renaming an NPC is
 * the example. Only {@code npcIds} travels in the payload, so a rename changes nothing on
 * the wire, and bumping the version for it would mean republishing a byte-identical
 * message and inflating the number that consumers compare against.
 */
@Service
public class SceneService {

    private final SceneRepository scenes;
    private final ChapterRepository chapters;
    private final NpcRepository npcs;
    private final QuestRepository quests;
    private final ItemRepository items;
    private final ContentEventDispatcher dispatcher;

    public SceneService(
            SceneRepository scenes,
            ChapterRepository chapters,
            NpcRepository npcs,
            QuestRepository quests,
            ItemRepository items,
            ContentEventDispatcher dispatcher) {
        this.scenes = scenes;
        this.chapters = chapters;
        this.npcs = npcs;
        this.quests = quests;
        this.items = items;
        this.dispatcher = dispatcher;
    }

    @Transactional(readOnly = true)
    public List<SceneResponse> findAll() {
        List<SceneResponse> result = new ArrayList<>();
        for (Scene scene : scenes.findAll()) {
            result.add(toResponse(scene));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public SceneResponse findById(Integer id) {
        return toResponse(require(id));
    }

    @Transactional
    public SceneResponse create(SceneRequest request) {
        Chapter chapter = requireChapter(request.chapterId());
        Scene scene = new Scene(chapter, request.name(), request.description());
        if (request.status() != null) {
            scene.setStatus(request.status());
        }
        scenes.save(scene);

        // A new scene is published too. A consumer that only hears about scenes once they
        // are edited would never learn of one that is authored correctly the first time.
        dispatcher.sceneChanged(scene.getId());
        return toResponse(scene);
    }

    @Transactional
    public SceneResponse update(Integer id, SceneRequest request) {
        Scene scene = require(id);
        scene.setChapter(requireChapter(request.chapterId()));
        scene.setName(request.name());
        scene.setDescription(request.description());
        if (request.status() != null) {
            scene.setStatus(request.status());
        }
        scenes.save(scene);
        return bumpAndPublish(scene.getId());
    }

    /**
     * Withdraws a scene from delivery.
     *
     * <p>Deliberately not a delete. Recorded paths reference scenes the player has already
     * been through, and those references have to keep resolving — the state side no longer
     * has a foreign key to stop a delete, so this is the only thing standing between an
     * author and a history that cannot be read.
     */
    @Transactional
    public SceneResponse retire(Integer id) {
        Scene scene = require(id);
        scene.setStatus(SceneStatus.RETIRED);
        scenes.save(scene);
        return bumpAndPublish(scene.getId());
    }

    // --- choices -------------------------------------------------------------------

    @Transactional
    public SceneResponse addChoice(Integer sceneId, ChoiceRequest request) {
        Scene scene = require(sceneId);
        scene.addChoice(buildChoice(request));
        scenes.save(scene);
        return bumpAndPublish(sceneId);
    }

    @Transactional
    public SceneResponse replaceChoice(Integer sceneId, Integer choiceId, ChoiceRequest request) {
        Scene scene = require(sceneId);
        Choice existing = scene.getChoices().stream()
                .filter(choice -> choice.getId().equals(choiceId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Choice " + choiceId + " is not part of scene " + sceneId));

        existing.setDestinationScene(requireScene(request.destinationSceneId()));
        existing.setLabel(request.label());
        existing.setBeat(request.beat());
        existing.setWeight(request.weight() == null ? (short) 1 : request.weight());
        existing.setOrdinal(request.ordinal() == null ? 0 : request.ordinal());

        existing.clearRequirements();
        existing.clearEffects();
        applyRequirements(existing, request.requirements());
        applyEffects(existing, request.effects());

        scenes.save(scene);
        return bumpAndPublish(sceneId);
    }

    @Transactional
    public SceneResponse deleteChoice(Integer sceneId, Integer choiceId) {
        Scene scene = require(sceneId);
        Choice existing = scene.getChoices().stream()
                .filter(choice -> choice.getId().equals(choiceId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Choice " + choiceId + " is not part of scene " + sceneId));
        scene.removeChoice(existing);
        scenes.save(scene);
        return bumpAndPublish(sceneId);
    }

    // --- npcs ----------------------------------------------------------------------

    @Transactional
    public SceneResponse replaceNpcs(Integer sceneId, List<Integer> npcIds) {
        Scene scene = require(sceneId);
        Set<Npc> replacement = new LinkedHashSet<>();
        for (Integer npcId : npcIds) {
            replacement.add(npcs.findById(npcId)
                    .orElseThrow(() -> new NotFoundException("NPC " + npcId + " does not exist")));
        }
        scene.getNpcs().clear();
        scene.getNpcs().addAll(replacement);
        scenes.save(scene);
        return bumpAndPublish(sceneId);
    }

    // --- internals -----------------------------------------------------------------

    /**
     * Increments the version and schedules the publication, then re-reads the scene so the
     * caller's response carries the number that was actually published.
     *
     * <p>The increment is a statement rather than a field assignment, so concurrent authors
     * cannot be given the same version. {@code clearAutomatically} on that query is what
     * makes the re-read see the new value rather than the stale one in the persistence
     * context.
     */
    private SceneResponse bumpAndPublish(Integer sceneId) {
        scenes.incrementVersion(sceneId);
        dispatcher.sceneChanged(sceneId);
        return toResponse(require(sceneId));
    }

    private Choice buildChoice(ChoiceRequest request) {
        Choice choice = new Choice(
                requireScene(request.destinationSceneId()),
                request.label(),
                request.beat(),
                request.weight() == null ? (short) 1 : request.weight(),
                request.ordinal() == null ? 0 : request.ordinal());
        applyRequirements(choice, request.requirements());
        applyEffects(choice, request.effects());
        return choice;
    }

    /**
     * Builds the requirements, refusing any whose fields do not match its kind.
     *
     * <p>This is the check the monolith could not perform. Its {@code target_id} was a
     * bare integer that meant a different thing per {@code consequence} value, so there
     * was nothing to validate against — and its own seed data contains rows where the
     * convention is not followed.
     */
    private void applyRequirements(Choice choice, List<RequirementRequest> requested) {
        if (requested == null) {
            return;
        }
        int ordinal = 0;
        for (RequirementRequest requirement : requested) {
            switch (requirement.kind()) {
                case STORY_FLAG -> {
                    requireBlankSafe(requirement.flag(), "A STORY_FLAG requirement needs a flag");
                    requireAbsent(requirement.itemId(), "A STORY_FLAG requirement must not carry an itemId");
                    choice.addRequirement(ChoiceRequirement.storyFlag(requirement.flag(), ordinal));
                }
                case ITEM -> {
                    requirePresent(requirement.itemId(), "An ITEM requirement needs an itemId");
                    requireItemExists(requirement.itemId());
                    requireAbsent(requirement.flag(), "An ITEM requirement must not carry a flag");
                    choice.addRequirement(ChoiceRequirement.item(requirement.itemId(), ordinal));
                }
            }
            ordinal++;
        }
    }

    private void applyEffects(Choice choice, List<EffectRequest> requested) {
        if (requested == null) {
            return;
        }
        int ordinal = 0;
        for (EffectRequest effect : requested) {
            switch (effect.kind()) {
                case GRANT_ITEM -> {
                    requirePresent(effect.itemId(), "A GRANT_ITEM effect needs an itemId");
                    requireItemExists(effect.itemId());
                    choice.addEffect(ChoiceEffect.grantItem(effect.itemId(), ordinal));
                }
                case GRANT_QUEST -> {
                    requirePresent(effect.questId(), "A GRANT_QUEST effect needs a questId");
                    requireQuestExists(effect.questId());
                    choice.addEffect(ChoiceEffect.grantQuest(effect.questId(), ordinal));
                }
                case COMPLETE_QUEST -> {
                    requirePresent(effect.questId(), "A COMPLETE_QUEST effect needs a questId");
                    requireQuestExists(effect.questId());
                    choice.addEffect(ChoiceEffect.completeQuest(effect.questId(), ordinal));
                }
                case MODIFY_STAT -> {
                    // The statistic is an enum, so a value outside the three a character
                    // actually has never reaches this point — it fails deserialisation.
                    // That is the whole argument of ADR-016 in one line: the monolith's
                    // authored data contains `lose_hp`, and its schema has no hp.
                    requirePresent(effect.stat(), "A MODIFY_STAT effect needs a stat");
                    requirePresent(effect.amount(), "A MODIFY_STAT effect needs an amount");
                    choice.addEffect(ChoiceEffect.modifyStat(effect.stat(), effect.amount(), ordinal));
                }
                case GRANT_FLAG -> {
                    requireBlankSafe(effect.flag(), "A GRANT_FLAG effect needs a flag");
                    choice.addEffect(ChoiceEffect.grantFlag(effect.flag(), ordinal));
                }
            }
            ordinal++;
        }
    }

    private void requireItemExists(Integer itemId) {
        if (!items.existsById(itemId)) {
            throw new NotFoundException("Item " + itemId + " does not exist");
        }
    }

    private void requireQuestExists(Integer questId) {
        if (!quests.existsById(questId)) {
            throw new NotFoundException("Quest " + questId + " does not exist");
        }
    }

    private void requirePresent(Object value, String message) {
        if (value == null) {
            throw new InvalidContentException(message);
        }
    }

    private void requireAbsent(Object value, String message) {
        if (value != null) {
            throw new InvalidContentException(message);
        }
    }

    private void requireBlankSafe(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidContentException(message);
        }
    }

    private Scene require(Integer id) {
        return scenes.findById(id)
                .orElseThrow(() -> new NotFoundException("Scene " + id + " does not exist"));
    }

    private Scene requireScene(Integer id) {
        return require(id);
    }

    private Chapter requireChapter(Integer id) {
        return chapters.findById(id)
                .orElseThrow(() -> new NotFoundException("Chapter " + id + " does not exist"));
    }

    private SceneResponse toResponse(Scene scene) {
        List<ChoiceResponse> choices = new ArrayList<>();
        for (Choice choice : scene.getChoices()) {
            List<RequirementResponse> requirements = new ArrayList<>();
            for (ChoiceRequirement requirement : choice.getRequirements()) {
                requirements.add(new RequirementResponse(
                        requirement.getKind(), requirement.getFlag(), requirement.getItemId()));
            }
            List<EffectResponse> effects = new ArrayList<>();
            for (ChoiceEffect effect : choice.getEffects()) {
                effects.add(new EffectResponse(
                        effect.getKind(), effect.getItemId(), effect.getQuestId(),
                        effect.getStat(), effect.getAmount(), effect.getFlag()));
            }
            choices.add(new ChoiceResponse(
                    choice.getId(),
                    choice.getDestinationScene().getId(),
                    choice.getLabel(),
                    choice.getBeat(),
                    choice.getWeight(),
                    choice.getOrdinal(),
                    requirements,
                    effects));
        }

        List<Integer> npcIds = new ArrayList<>();
        for (Npc npc : scene.getNpcs()) {
            npcIds.add(npc.getId());
        }
        npcIds.sort(Integer::compareTo);

        List<Integer> questIds = new ArrayList<>();
        for (Quest quest : scene.getQuests()) {
            questIds.add(quest.getId());
        }
        questIds.sort(Integer::compareTo);

        return new SceneResponse(
                scene.getId(),
                scene.getChapter().getId(),
                scene.getName(),
                scene.getDescription(),
                scene.getStatus(),
                scene.getVersion(),
                choices,
                npcIds,
                questIds);
    }
}
