package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

/**
 * MOVED: {@code quest} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>A quest cannot exist without the scene offering it, which is what makes it content.
 * Tracking a quest per character stays on this side.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — {@code
 * /admin/v1/scenes/{sceneId}/quests}.
 */
@Entity
@Table(name = "quest")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "scene_id", nullable = false)
    private Scene scene;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    public Quest() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Scene getScene() { return scene; }
    public void setScene(Scene scene) { this.scene = scene; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
