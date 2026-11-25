package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.QuestRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.QuestResponseDTO;
import dk.ek.gruppe2.chooseyourfate.service.QuestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * MOVED: {@code quest} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>A quest cannot exist without the scene offering it, which is what makes it content.
 * Tracking a quest per character stays on this side.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — {@code
 * /admin/v1/scenes/{sceneId}/quests}.
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
