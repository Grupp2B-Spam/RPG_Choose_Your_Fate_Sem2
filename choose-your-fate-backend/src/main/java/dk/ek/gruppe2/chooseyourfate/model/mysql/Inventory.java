package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "character_id", nullable = false)
    private CharacterAvatar character;

    public Inventory() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public CharacterAvatar getCharacter() { return character; }
    public void setCharacter(CharacterAvatar character) { this.character = character; }
}
