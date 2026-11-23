package dk.ek.gruppe2.storycontent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A non-player character, placed in scenes.
 *
 * <p>Content, and mandatorily dependent on {@link Race}. That is the second, independent
 * reason races belong to this service: with no players in the system at all, content
 * already cannot be authored without them.
 *
 * <p>Only {@code npcIds} travels in the scene payload, so renaming an NPC changes nothing
 * a consumer can observe and must <em>not</em> bump the scene version. Placing an NPC in a
 * scene, or removing it, does change the payload and must. ADR-006 says "every change
 * consumers can observe", and this is where that phrasing earns its keep.
 */
@Entity
@Table(name = "npc")
public class Npc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    protected Npc() {
        // for JPA
    }

    public Npc(String name, Race race) {
        this.name = name;
        this.race = race;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Race getRace() {
        return race;
    }

    public void setRace(Race race) {
        this.race = race;
    }
}
