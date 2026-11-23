package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MIGRATED to StoryContentManager: {@code chapter} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>It is one of the ten tables the content/state split assigned to that service. A
 * chapter is not published as an event of its own: it reaches consumers as
 * {@code chapterId} inside a scene payload and as {@code startingChapterId} inside a
 * race payload.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/chapters} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
@Entity
@Table(name = "chapter")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100)
    private String name;

    @OneToMany(mappedBy = "chapter")
    private List<Scene> scenes = new ArrayList<>();

    @OneToMany(mappedBy = "chapter")
    private List<CharacterAvatar> characters = new ArrayList<>();

    @ManyToOne(optional = true)
    @JoinColumn(name = "starting_scene_id", nullable = true)
    private Scene startingScene;

    public Chapter() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Scene> getScenes() { return scenes; }
    public void setScenes(List<Scene> scenes) { this.scenes = scenes; }

    public List<CharacterAvatar> getCharacters() { return characters; }
    public void setCharacters(List<CharacterAvatar> characters) { this.characters = characters; }

    public Scene getStartingScene() { return startingScene; }

    public void setStartingScene(Scene startingScene) { this.startingScene = startingScene; }
}
