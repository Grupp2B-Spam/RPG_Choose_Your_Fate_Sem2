package dk.ek.gruppe2.storycontent.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import dk.ek.gruppe2.storycontent.domain.Enums.SceneStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * A scene: one moment of the story, its choices, the NPCs standing in it and the quests it
 * offers. The aggregate root published as scene-content.updated — everything reachable from
 * here goes out in one message.
 *
 * version is the sceneVersion consumers compare against. Not a JPA @Version annotation:
 * that counts changes to this row, and this has to count changes to the whole published
 * message, child rows included. It is incremented by a SQL statement, not read-then-write,
 * so two authors saving at once cannot be handed the same number.
 */
@Entity
@Table(name = "scene")
public class Scene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false, length = 100)
    private String name;

    /** The prose shown to the player. */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SceneStatus status = SceneStatus.PUBLISHED;

    @Column(nullable = false)
    private int version = 1;

    @OneToMany(mappedBy = "scene", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordinal ASC, id ASC")
    private List<Choice> choices = new ArrayList<>();

    /**
     * Quests offered in this scene. Read-only here; a quest is created against its scene.
     * Mapped so the payload's questIds can be built without a second query.
     */
    @OneToMany(mappedBy = "scene")
    @OrderBy("id ASC")
    private List<Quest> quests = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "scene_npc",
            joinColumns = @JoinColumn(name = "scene_id"),
            inverseJoinColumns = @JoinColumn(name = "npc_id"))
    private Set<Npc> npcs = new LinkedHashSet<>();

    protected Scene() {
        // for JPA
    }

    public Scene(Chapter chapter, String name, String description) {
        this.chapter = chapter;
        this.name = name;
        this.description = description;
    }

    public void addChoice(Choice choice) {
        choice.setScene(this);
        choices.add(choice);
    }

    public void removeChoice(Choice choice) {
        choices.remove(choice);
        choice.setScene(null);
    }

    public Integer getId() {
        return id;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SceneStatus getStatus() {
        return status;
    }

    public void setStatus(SceneStatus status) {
        this.status = status;
    }

    public int getVersion() {
        return version;
    }

    public List<Choice> getChoices() {
        return choices;
    }

    public List<Quest> getQuests() {
        return quests;
    }

    public Set<Npc> getNpcs() {
        return npcs;
    }
}
