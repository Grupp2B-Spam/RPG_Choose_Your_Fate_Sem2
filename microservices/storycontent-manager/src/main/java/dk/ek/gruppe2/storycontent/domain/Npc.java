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
 * <p>Only NPC <em>ids</em> go out in the scene message, so renaming one changes nothing a
 * consumer can see and must <em>not</em> bump the scene version. Putting an NPC into a
 * scene, or taking it out, does change the message and must.
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
