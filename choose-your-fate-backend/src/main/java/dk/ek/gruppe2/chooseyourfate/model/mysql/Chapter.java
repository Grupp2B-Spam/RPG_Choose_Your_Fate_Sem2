package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MOVED: {@code chapter} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>It has no event of its own; it reaches consumers inside scene and race messages.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — {@code /admin/v1/chapters}.
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
