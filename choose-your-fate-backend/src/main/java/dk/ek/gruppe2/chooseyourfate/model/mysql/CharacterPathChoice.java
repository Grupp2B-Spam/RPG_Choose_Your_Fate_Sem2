package dk.ek.gruppe2.chooseyourfate.model.mysql;

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

    @ManyToOne
    @MapsId("choiceId")
    @JoinColumn(name = "choice_id")
    private Choice choice;

    public CharacterPathChoice() {}

    public CharacterPathChoice(CharacterPath characterPath, Choice choice) {
        this.characterPath = characterPath;
        this.choice = choice;
        this.id = new CharacterPathChoiceId(characterPath.getId(), choice.getId());
    }

    public CharacterPathChoiceId getId() { return id; }
    public void setId(CharacterPathChoiceId id) { this.id = id; }

    public CharacterPath getCharacterPath() { return characterPath; }
    public void setCharacterPath(CharacterPath characterPath) { this.characterPath = characterPath; }

    public Choice getChoice() { return choice; }
    public void setChoice(Choice choice) { this.choice = choice; }
}
