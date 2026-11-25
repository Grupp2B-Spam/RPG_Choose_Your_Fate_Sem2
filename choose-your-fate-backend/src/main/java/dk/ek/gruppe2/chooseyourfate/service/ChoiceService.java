package dk.ek.gruppe2.chooseyourfate.service;

import java.util.List;

import org.springframework.stereotype.Service;

import dk.ek.gruppe2.chooseyourfate.dto.choice.ChoiceResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.choice.CreateChoiceRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.choice.UpdateChoiceRequestDTO;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Choice;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Scene;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.ChoiceRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.SceneRepository;

@Service
public class ChoiceService {
    private final ChoiceRepository choiceRepository;
    private final SceneRepository sceneRepository;

    public ChoiceService(ChoiceRepository choiceRepository, SceneRepository sceneRepository) {
        this.choiceRepository = choiceRepository;
        this.sceneRepository = sceneRepository;
    }

    public List<ChoiceResponseDTO> getAllChoices() {
        return choiceRepository.findAll()
                .stream()
                .map(ChoiceResponseDTO::new)
                .toList();
    }

    public ChoiceResponseDTO getChoiceById(Integer id) {
        return new ChoiceResponseDTO(getChoiceEntity(id));
    }

    public ChoiceResponseDTO createChoice(CreateChoiceRequestDTO request) {
        Choice choice = request.toEntity(getSceneById(request.getSceneId()), getSceneById(request.getDestinationSceneId()));
        return new ChoiceResponseDTO(choiceRepository.save(choice));
    }


    public ChoiceResponseDTO updateChoice(Integer id, UpdateChoiceRequestDTO request) {
        Choice choice = getChoiceEntity(id);

        choice.setDescription(request.getDescription());
        choice.setScene(getSceneById(request.getSceneId()));
        choice.setDestinationScene(getSceneById(request.getDestinationSceneId()));
        choice.setConsequence(request.getConsequence());
        choice.setTargetId(request.getTargetId());
        choice.setValueInt(request.getValueInt());
        choice.setRequirements(request.getRequirements());
        choice.setStoryWeight(request.getStoryWeight());
        return new ChoiceResponseDTO(choiceRepository.save(choice));
    }

    public void deleteChoice(Integer id) {
        if (!choiceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Choice not found with id: " + id);
        }
        choiceRepository.deleteById(id);
    }

    private Choice getChoiceEntity(Integer id) {
        return choiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Choice not found with id: " + id));
    }

    private Scene getSceneById(Integer sceneId){
        return sceneRepository.findById(sceneId)
            .orElseThrow(() -> new ResourceNotFoundException("Scene not found with id: " + sceneId));
    }
}
