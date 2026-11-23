package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * MIGRATED to StoryContentManager: {@code race_details} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>It is called {@code race} there, and is published as
 * {@code race-definition.updated}. It went to StoryContentManager rather than
 * CharacterManager because {@code starting_chapter_id} is a mandatory reference into
 * story content: the other way round, CharacterManager would hold a foreign key into
 * another service's database. CharacterManager is expected to cache the event
 * instead, and {@code character_avatar.race_detail_id} becomes a plain integer.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/races} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
@Entity
@Table(name = "race_details")
public class RaceDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nullable
    private String name;

    @OneToMany(mappedBy = "raceDetails")
    private List<CharacterAvatar> characters = new ArrayList<>();

    @OneToMany(mappedBy = "raceDetails")
    private List<Npc> npcs = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "starting_chapter_id", nullable = false)
    private Chapter startingChapter;

    public RaceDetails() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public List<CharacterAvatar> getCharacters() { return characters; }
    public void setCharacters(List<CharacterAvatar> characters) { this.characters = characters; }

    public List<Npc> getNpcs() { return npcs; }
    public void setNpcs(List<Npc> npcs) { this.npcs = npcs; }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Chapter getStartingChapter() {
        return startingChapter;
    }

    public void setStartingChapter(Chapter startingChapter) {
        this.startingChapter = startingChapter;
    }
}
