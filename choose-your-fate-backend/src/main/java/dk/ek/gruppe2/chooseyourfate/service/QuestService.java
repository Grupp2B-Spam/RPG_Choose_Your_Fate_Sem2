package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.dto.QuestRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.QuestResponseDTO;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Quest;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Scene;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.QuestRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.SceneRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class QuestService {

    private final QuestRepository questRepository;
    private final SceneRepository sceneRepository;

    public QuestService(QuestRepository questRepository, SceneRepository sceneRepository) {
        this.questRepository = questRepository;
        this.sceneRepository = sceneRepository;
    }

    public List<QuestResponseDTO> getAllQuests() {
        List<Quest> quests = questRepository.findAll();
        List<QuestResponseDTO> response = quests.stream().map((quest -> new QuestResponseDTO(quest))).toList();
        return response;
    }

    public QuestResponseDTO getQuestById(Integer id) {
        Quest quest = questRepository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        QuestResponseDTO response = new QuestResponseDTO(quest);
        return response;
    }

    public ResponseEntity<QuestResponseDTO> createQuest(QuestRequestDTO questRequestDTO) {
        Scene scene = sceneRepository.findById(questRequestDTO.getSceneId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        Quest quest = questRequestDTO.getQuestEntity(scene);
        Quest savedQuest = questRepository.save(quest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new QuestResponseDTO(savedQuest));
    }

    public QuestResponseDTO updateQuest(Integer id, QuestRequestDTO questRequestDTO) {
        Quest quest = questRepository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        Scene scene = sceneRepository.findById(questRequestDTO.getSceneId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        quest.setDescription(questRequestDTO.getDescription());
        quest.setScene(scene);
        questRepository.save(quest);
        return new QuestResponseDTO(quest);

    }

    public void deleteQuest(Integer id) {
        if (!questRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        questRepository.deleteById(id);
    }
}
