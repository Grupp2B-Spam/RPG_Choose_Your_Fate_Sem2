package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.dto.NpcRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.NpcResponseDTO;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Npc;
import dk.ek.gruppe2.chooseyourfate.model.mysql.RaceDetails;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.NpcRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.RaceDetailsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * MIGRATED to StoryContentManager: {@code npc} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>Only {@code npcIds} travels in the scene payload there, so an NPC's name is not
 * something any consumer receives.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/npcs} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
@Service
public class NpcService {

    private final NpcRepository npcRepository;
    private final RaceDetailsRepository raceDetailsRepository;

    public NpcService(NpcRepository npcRepository, RaceDetailsRepository raceDetailsRepository) {
        this.npcRepository = npcRepository;
        this.raceDetailsRepository = raceDetailsRepository;
    }

    public List<NpcResponseDTO> getAllNpcs() {
        List<Npc> npcs = npcRepository.findAll();
        List<NpcResponseDTO> response = npcs.stream().map((npc -> new NpcResponseDTO(npc))).toList();
        return response;
    }

    public NpcResponseDTO getNpcById(Integer id) {
        Npc npc = npcRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new NpcResponseDTO(npc);
    }

    public ResponseEntity<NpcResponseDTO> createNpc(NpcRequestDTO npcRequestDTO) {
        RaceDetails raceDetails = raceDetailsRepository.findById(npcRequestDTO.getRaceDetailsId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Npc npc = npcRequestDTO.getNpcEntity(raceDetails);
        Npc savedNpc = npcRepository.save(npc);
        return ResponseEntity.status(HttpStatus.CREATED).body(new NpcResponseDTO(savedNpc));
    }

    public NpcResponseDTO updateNpc(Integer id, NpcRequestDTO npcRequestDTO) {
        Npc npc = npcRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (npcRequestDTO.getRaceDetailsId() != null) {
            RaceDetails raceDetails = raceDetailsRepository.findById(npcRequestDTO.getRaceDetailsId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            npc.setRaceDetails(raceDetails);
        }
        npc.setName(npcRequestDTO.getName());
        npcRepository.save(npc);
        return new NpcResponseDTO(npc);
    }

    public void deleteNpc(Integer id) {
        if (!npcRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        npcRepository.deleteById(id);
    }
}
