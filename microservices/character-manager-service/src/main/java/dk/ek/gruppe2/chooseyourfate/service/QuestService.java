package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.dto.QuestRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.QuestResponseDTO;
import dk.ek.gruppe2.chooseyourfate.model.Quest;
import dk.ek.gruppe2.chooseyourfate.repository.QuestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class QuestService {

    private final QuestRepository questRepository;

    public QuestService(QuestRepository questRepository) {
        this.questRepository = questRepository;
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
        Quest quest = questRequestDTO.getQuestEntity();
        Quest savedQuest = questRepository.save(quest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new QuestResponseDTO(savedQuest));
    }

    public QuestResponseDTO updateQuest(Integer id, QuestRequestDTO questRequestDTO) {
        Quest quest = questRepository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
        quest.setDescription(questRequestDTO.getDescription());
        quest.setSceneId(questRequestDTO.getSceneId());
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
