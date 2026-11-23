package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.NpcRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.NpcResponseDTO;
import dk.ek.gruppe2.chooseyourfate.service.NpcService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
@RestController
@RequestMapping("/choose-your-fate/npcs")
public class NpcController {

    private NpcService npcService;

    public NpcController(NpcService npcService) {
        this.npcService = npcService;
    }

    @GetMapping
    public List<NpcResponseDTO> getNpcs() {
        return npcService.getAllNpcs();
    }

    @GetMapping("/{id}")
    public NpcResponseDTO getNpcById(@PathVariable("id") Integer id) {
        return npcService.getNpcById(id);
    }

    @PostMapping
    public ResponseEntity<NpcResponseDTO> createNpc(@RequestBody NpcRequestDTO requestDTO) {
        return npcService.createNpc(requestDTO);
    }

    @PutMapping("/{id}")
    public NpcResponseDTO updateNpc(@PathVariable("id") Integer id, @RequestBody NpcRequestDTO requestDTO) {
        return npcService.updateNpc(id, requestDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteNpc(@PathVariable("id") Integer id) {
        npcService.deleteNpc(id);
    }

}
