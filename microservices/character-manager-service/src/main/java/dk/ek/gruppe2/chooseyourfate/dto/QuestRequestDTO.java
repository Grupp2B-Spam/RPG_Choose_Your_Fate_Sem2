package dk.ek.gruppe2.chooseyourfate.dto;

import dk.ek.gruppe2.chooseyourfate.model.Quest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class QuestRequestDTO {

    private Integer sceneId;
    private String description;

    public Quest getQuestEntity() {
        Quest quest = new Quest();
        quest.setSceneId(this.sceneId);
        quest.setDescription(this.description);
        return quest;
    }

}
