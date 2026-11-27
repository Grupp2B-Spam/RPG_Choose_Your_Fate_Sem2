package dk.ek.gruppe2.chooseyourfate.controller;

import dk.ek.gruppe2.chooseyourfate.dto.QuestRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.QuestResponseDTO;
import dk.ek.gruppe2.chooseyourfate.service.QuestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
