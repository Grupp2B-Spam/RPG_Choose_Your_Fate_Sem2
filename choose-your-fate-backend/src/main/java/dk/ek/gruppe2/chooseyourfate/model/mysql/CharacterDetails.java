package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
@Entity
@Table(name = "character_details")
public class CharacterDetails {

    @Id
    @Column(name = "character_id")
    private Integer characterId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "character_id")
    private CharacterAvatar character;

    private Integer intelligence;
    private Integer charisma;
    private Integer fashion;

    public CharacterDetails() {}

    public Integer getCharacterId() { return characterId; }
    public void setCharacterId(Integer characterId) { this.characterId = characterId; }

    public CharacterAvatar getCharacter() { return character; }
    public void setCharacter(CharacterAvatar character) { this.character = character; }

    public Integer getIntelligence() { return intelligence; }
    public void setIntelligence(Integer intelligence) { this.intelligence = intelligence; }

    public Integer getCharisma() { return charisma; }
    public void setCharisma(Integer charisma) { this.charisma = charisma; }

    public Integer getFashion() { return fashion; }
    public void setFashion(Integer fashion) { this.fashion = fashion; }
}
