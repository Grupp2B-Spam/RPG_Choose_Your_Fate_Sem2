package dk.ek.gruppe2.chooseyourfate.model;

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

    @Column
    private Integer startingChapter;

    public RaceDetails() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public List<CharacterAvatar> getCharacters() { return characters; }
    public void setCharacters(List<CharacterAvatar> characters) { this.characters = characters; }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStartingChapter() {
        return startingChapter;
    }

    public void setStartingChapter(Integer startingChapter) {
        this.startingChapter = startingChapter;
    }

    public Integer getStartingScene() { return startingChapter; }
    public void setStartingScene(Integer startingScene) { this.startingChapter = startingScene; }
}
