package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CharacterHasQuestId implements Serializable {

    private Integer characterId;
    private Integer questId;

    public CharacterHasQuestId() {}

    public CharacterHasQuestId(Integer characterId, Integer questId) {
        this.characterId = characterId;
        this.questId = questId;
    }

    public Integer getCharacterId() { return characterId; }
    public void setCharacterId(Integer characterId) { this.characterId = characterId; }

    public Integer getQuestId() { return questId; }
    public void setQuestId(Integer questId) { this.questId = questId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CharacterHasQuestId that)) return false;
        return Objects.equals(characterId, that.characterId) &&
                Objects.equals(questId, that.questId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(characterId, questId);
    }
}
