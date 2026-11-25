package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
