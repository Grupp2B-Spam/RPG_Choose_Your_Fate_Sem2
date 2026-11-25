package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @Column(name = "character_id")
    private Integer characterId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "character_id")
    private CharacterAvatar character;

    @ManyToOne
    @JoinColumn(name = "head")
    private Item head;

    @ManyToOne
    @JoinColumn(name = "legs")
    private Item legs;

    @ManyToOne
    @JoinColumn(name = "chest")
    private Item chest;

    public Equipment() {}

    public Integer getCharacterId() { return characterId; }
    public void setCharacterId(Integer characterId) { this.characterId = characterId; }

    public CharacterAvatar getCharacter() { return character; }
    public void setCharacter(CharacterAvatar character) { this.character = character; }

    public Item getHead() { return head; }
    public void setHead(Item head) { this.head = head; }

    public Item getLegs() { return legs; }
    public void setLegs(Item legs) { this.legs = legs; }

    public Item getChest() { return chest; }
    public void setChest(Item chest) { this.chest = chest; }
}