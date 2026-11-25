package dk.ek.gruppe2.chooseyourfate.dto.choice;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Choice;

public class ChoiceResponseDTO {
    private String id;
    private String destinationSceneId;
    private String sceneId;
    private String description;
    private String consequence;
    private Integer targetId;
    private Integer valueInt;
    private String requirements;


    public ChoiceResponseDTO(String id, String destinationSceneId, String sceneId, String description,
            String consequence, Integer targetId, Integer valueInt, String requirements) {
        this.id = id;
        this.destinationSceneId = destinationSceneId;
        this.sceneId = sceneId;
        this.description = description;
        this.consequence = consequence;
        this.targetId = targetId;
        this.valueInt = valueInt;
        this.requirements = requirements;
    }

    public ChoiceResponseDTO(Choice choice) {
        this.id = choice.getId().toString();
        this.description = choice.getDescription();
        this.destinationSceneId = choice.getDestinationScene().getId().toString();
        this.sceneId = choice.getScene().getId().toString();
        this.consequence = choice.getConsequence();
        this.targetId = choice.getTargetId();
        this.valueInt = choice.getValueInt();
        this.requirements = choice.getRequirements();
    }

    public ChoiceResponseDTO toDTO(Choice choice) {
        return new ChoiceResponseDTO(
                choice.getId().toString(),
                choice.getDestinationScene().getId().toString(),
                choice.getScene().getId().toString(),
                choice.getDescription(),
                choice.getConsequence(),
                choice.getTargetId(),
                choice.getValueInt(),
                choice.getRequirements()
        );
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDestinationSceneId() {
        return destinationSceneId;
    }

    public void setDestinationSceneId(String destinationSceneId) {
        this.destinationSceneId = destinationSceneId;
    }

    public String getConsequence() {
        return consequence;
    }

    public void setConsequence(String consequence) {
        this.consequence = consequence;
    }

    public Integer getTargetId() {
        return targetId;
    }

    public void setTargetId(Integer targetId) {
        this.targetId = targetId;
    }

    public Integer getValueInt() {
        return valueInt;
    }

    public void setValueInt(Integer valueInt) {
        this.valueInt = valueInt;
    }

    
    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String requirements) {
        this.requirements = requirements;
    }

    public String getSceneId() {
        return sceneId;
    }

    public void setSceneId(String sceneId) {
        this.sceneId = sceneId;
    }


}
