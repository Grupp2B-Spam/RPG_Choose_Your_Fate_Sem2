package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.QuestRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.QuestResponseDTO;
import dk.ek.gruppe2.chooseyourfate.service.QuestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MIGRATED to StoryContentManager: {@code quest} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>A quest cannot exist without the scene that offers it, which is what settles it as
 * content rather than as the authored half of {@code character_has_quest} — that
 * table stays here, because it records what a player has done.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/scenes/{sceneId}/quests} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
@RestController
@RequestMapping("/choose-your-fate/quests")
public class QuestController {

    private final QuestService questService;

    public QuestController(QuestService questService) {
        this.questService = questService;
    }

    @GetMapping
    public List<QuestResponseDTO> getAllQuests() {
        return questService.getAllQuests();
    }

    @GetMapping("/{id}")
    public QuestResponseDTO getQuest(@PathVariable("id") Integer id) {
        return questService.getQuestById(id);
    }

    @PostMapping
    public ResponseEntity<QuestResponseDTO> createQuest(@RequestBody QuestRequestDTO questRequestDTO) {
        return questService.createQuest(questRequestDTO);
    }

    @PutMapping("/{id}")
    public QuestResponseDTO updateQuest(@PathVariable("id") Integer id, @RequestBody QuestRequestDTO questRequestDTO) {
        return questService.updateQuest(id, questRequestDTO);
    }

    @DeleteMapping("/{id}")
    public void deleteQuest(@PathVariable("id") Integer id) {
        questService.deleteQuest(id);
    }
}
