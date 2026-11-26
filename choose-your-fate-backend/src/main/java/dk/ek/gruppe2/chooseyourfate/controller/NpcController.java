package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.NpcRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.NpcResponseDTO;
import dk.ek.gruppe2.chooseyourfate.service.NpcService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MOVED: {@code npc} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>Only NPC ids reach consumers, inside the scene message — a name is not published at
 * all.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — {@code /admin/npcs}.
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
