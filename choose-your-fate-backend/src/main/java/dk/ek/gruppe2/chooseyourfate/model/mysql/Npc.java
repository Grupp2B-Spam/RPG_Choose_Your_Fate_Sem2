package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

/**
 * MOVED: {@code npc} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>Only NPC ids reach consumers, inside the scene message — a name is not published at
 * all.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — {@code /admin/npcs}.
 */
@Entity
@Table(name = "npc")
public class Npc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 45)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "race_details_id", nullable = false)
    private RaceDetails raceDetails;

    public Npc() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public RaceDetails getRaceDetails() { return raceDetails; }
    public void setRaceDetails(RaceDetails raceDetails) { this.raceDetails = raceDetails; }
}
