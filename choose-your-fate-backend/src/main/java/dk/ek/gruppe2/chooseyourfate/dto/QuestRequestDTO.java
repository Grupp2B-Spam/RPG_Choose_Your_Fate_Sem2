package dk.ek.gruppe2.chooseyourfate.dto;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Quest;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Scene;
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

    public Quest getQuestEntity(Scene scene) {
        Quest quest = new Quest();
        quest.setScene(scene);
        quest.setDescription(this.description);
        return quest;
    }

}
