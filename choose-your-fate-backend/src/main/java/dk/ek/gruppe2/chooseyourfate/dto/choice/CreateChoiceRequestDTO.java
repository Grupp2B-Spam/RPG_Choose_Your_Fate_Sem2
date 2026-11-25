package dk.ek.gruppe2.chooseyourfate.dto.choice;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Choice;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Scene;

public class CreateChoiceRequestDTO {
    private String description;
    private Integer sceneId;
    private Integer destinationSceneId;
    private String consequence;
    private Integer targetId;
    private Integer valueInt;
    private String requirements;
    private Short storyWeight;

    public Choice toEntity(Scene currentScene, Scene destinationScene) {
        Choice choice = new Choice();

        choice.setDescription(description);
        choice.setScene(currentScene);
        choice.setDestinationScene(destinationScene);
        choice.setConsequence(consequence);
        choice.setTargetId(targetId);
        choice.setValueInt(valueInt);
        choice.setRequirements(requirements);
        choice.setStoryWeight(storyWeight);

        return choice;
    }

    public Integer getSceneId() {
        return sceneId;
    }

    public void setSceneId(Integer sceneId) {
        this.sceneId = sceneId;
    }

    public Integer getDestinationSceneId() {
        return destinationSceneId;
    }

    public void setDestinationSceneId(Integer destinationSceneId) {
        this.destinationSceneId = destinationSceneId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Short getStoryWeight() {
        return storyWeight;
    }

    public void setStoryWeight(Short storyWeight) {
        this.storyWeight = storyWeight;
    }
}
