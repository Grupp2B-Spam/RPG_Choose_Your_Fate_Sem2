package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

/**
 * MIGRATED to StoryContentManager: {@code npc} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>Only {@code npcIds} travels in the scene payload there, so an NPC's name is not
 * something any consumer receives.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/npcs} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
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
