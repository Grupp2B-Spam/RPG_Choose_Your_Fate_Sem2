package dk.ek.gruppe2.recapmanager.model;

import jakarta.persistence.*;

@Entity
@Table(name = "character_path_choice")
public class CharacterPathChoice {

    @EmbeddedId
    private CharacterPathChoiceId id;

    @ManyToOne
    @MapsId("characterPathId")
    @JoinColumn(name = "character_path_id")
    private CharacterPath characterPath;

    public CharacterPathChoice() {}

    public CharacterPathChoice(CharacterPath characterPath, Integer choiceId) {
        this.characterPath = characterPath;
        this.id = new CharacterPathChoiceId(characterPath.getId(), choiceId);
    }

    public CharacterPathChoiceId getId() { return id; }
    public void setId(CharacterPathChoiceId id) { this.id = id; }

    public CharacterPath getCharacterPath() { return characterPath; }
    public void setCharacterPath(CharacterPath characterPath) { this.characterPath = characterPath; }

    public Integer getChoice() { return id == null ? null : id.getChoiceId(); }
    public void setChoice(Integer choiceId) {
        if (id == null) {
            id = new CharacterPathChoiceId();
        }
        id.setChoiceId(choiceId);
    }
}
