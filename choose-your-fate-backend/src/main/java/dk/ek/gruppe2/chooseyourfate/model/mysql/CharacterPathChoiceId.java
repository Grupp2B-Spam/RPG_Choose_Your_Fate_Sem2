package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CharacterPathChoiceId implements Serializable {

    private Integer characterPathId;
    private Integer choiceId;

    public CharacterPathChoiceId() {}

    public CharacterPathChoiceId(Integer characterPathId, Integer choiceId) {
        this.characterPathId = characterPathId;
        this.choiceId = choiceId;
    }

    public Integer getCharacterPathId() { return characterPathId; }
    public void setCharacterPathId(Integer characterPathId) { this.characterPathId = characterPathId; }

    public Integer getChoiceId() { return choiceId; }
    public void setChoiceId(Integer choiceId) { this.choiceId = choiceId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CharacterPathChoiceId that)) return false;
        return Objects.equals(characterPathId, that.characterPathId) &&
                Objects.equals(choiceId, that.choiceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(characterPathId, choiceId);
    }
}
