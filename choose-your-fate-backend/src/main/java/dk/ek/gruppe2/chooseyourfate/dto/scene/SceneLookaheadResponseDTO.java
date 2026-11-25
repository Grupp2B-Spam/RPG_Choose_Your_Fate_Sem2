package dk.ek.gruppe2.chooseyourfate.dto.scene;

import dk.ek.gruppe2.chooseyourfate.dto.choice.ChoiceResponseDTO;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Choice;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Scene;

import java.util.Comparator;

import java.util.List;

public class SceneLookaheadResponseDTO {
    private SceneResponseDTO scene;
    private List<ChoiceResponseDTO> choices;
    private List<SceneResponseDTO> destinationScenes;

    public SceneLookaheadResponseDTO() {
    }

    public SceneLookaheadResponseDTO(
            SceneResponseDTO scene,
            List<ChoiceResponseDTO> choices,
            List<SceneResponseDTO> destinationScenes
    ) {
        this.scene = scene;
        this.choices = choices;
        this.destinationScenes = destinationScenes;
    }


    public SceneLookaheadResponseDTO(Scene scene) {
        this.scene = new SceneResponseDTO(scene);
        this.choices = scene.getChoices()
                .stream()
                .sorted(Comparator.comparing(Choice::getId))
                .map(ChoiceResponseDTO::new)
                .toList();
        this.destinationScenes = scene.getChoices()
                .stream()
                .map((choice) -> {
            return new SceneResponseDTO(choice.getDestinationScene());
        })
                .toList();
    }

    public SceneResponseDTO getScene() {
        return scene;
    }

    public void setScene(SceneResponseDTO scene) {
        this.scene = scene;
    }

    public List<ChoiceResponseDTO> getChoices() {
        return choices;
    }

    public void setChoices(List<ChoiceResponseDTO> choices) {
        this.choices = choices;
    }

    public List<SceneResponseDTO> getDestinationScenes() {
        return destinationScenes;
    }

    public void setDestinationScenes(List<SceneResponseDTO> destinationScenes) {
        this.destinationScenes = destinationScenes;
    }
}
