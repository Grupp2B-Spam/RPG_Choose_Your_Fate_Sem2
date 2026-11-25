package dk.ek.gruppe2.chooseyourfate.dto.chapter;

import java.util.List;

import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterAvatar;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Scene;

public class UpdateChapterRequestDTO {
    
    private String name;
    private List<Scene> scenes;
    private List<CharacterAvatar> characters;
    private Integer startingSceneId;
    
    public String getName() {
        return name;
    }

    public List<Scene> getScenes() { return scenes; }

    public List<CharacterAvatar> getCharacters() { return characters; }

    public void setCharacters(List<CharacterAvatar> characters) {
        this.characters = characters;
    }

    public Integer getStartingSceneId() {
        return startingSceneId;
    }

    public void setStartingSceneId(Integer startingSceneId) {
        this.startingSceneId = startingSceneId;
    }
}
