package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

/**
 * MIGRATED to StoryContentManager: {@code quest} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>A quest cannot exist without the scene that offers it, which is what settles it as
 * content rather than as the authored half of {@code character_has_quest} — that
 * table stays here, because it records what a player has done.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/scenes/{sceneId}/quests} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
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
