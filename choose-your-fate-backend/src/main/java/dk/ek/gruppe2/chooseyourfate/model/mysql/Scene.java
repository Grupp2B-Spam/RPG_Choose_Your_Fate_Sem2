package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MIGRATED to StoryContentManager: {@code scene} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>It is the aggregate root StoryContentManager publishes as
 * {@code scene-content.updated}, carrying the whole scene — its choices, the NPCs
 * standing in it and the quests it offers — rather than a notification to go and
 * fetch it.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/scenes} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
@Entity
@Table(name = "scene")
public class Scene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(length = 100)
    private String name;

    @OneToMany(mappedBy = "scene")
    private List<CharacterAvatar> characters = new ArrayList<>();

    @OneToMany(mappedBy = "scene")
    private List<Quest> quests = new ArrayList<>();

    @OneToMany(mappedBy = "scene")
    private List<Choice> choices = new ArrayList<>();

    public Scene() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Chapter getChapter() { return chapter; }
    public void setChapter(Chapter chapter) { this.chapter = chapter; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<CharacterAvatar> getCharacters() { return characters; }
    public void setCharacters(List<CharacterAvatar> characters) { this.characters = characters; }

    public List<Quest> getQuests() { return quests; }
    public void setQuests(List<Quest> quests) { this.quests = quests; }

    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }
}
