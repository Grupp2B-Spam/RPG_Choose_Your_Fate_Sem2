package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

/**
 * MIGRATED to StoryContentManager: {@code choice} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>A choice is part of the scene aggregate there and is never published on its own, so
 * it is edited through its scene. Two columns have no counterpart here:
 * {@code beat}, the authored sentence copied into a player's history, and the typed
 * effects that replaced {@code consequence}, {@code target_id} and
 * {@code value_int}.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code /admin/v1/scenes/{id}/choices} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
@Entity
@Table(name = "choice")
public class Choice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "scene_id", nullable = false)
    private Scene scene;

    @ManyToOne
    @JoinColumn(name = "destination_scene_id")
    private Scene destinationScene;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    @Column(length = 50)
    private String consequence;

    @Column(name = "target_id")
    private Integer targetId;

    @Column(name = "value_int")
    private Integer valueInt;

    @Column(name = "story_weight", nullable = false)
    private Short storyWeight;

    @Column(columnDefinition = "json")
    private String requirements;

    public Choice() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Scene getScene() { return scene; }
    public void setScene(Scene scene) { this.scene = scene; }

    public Scene getDestinationScene() { return destinationScene; }
    public void setDestinationScene(Scene destinationScene) { this.destinationScene = destinationScene; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getConsequence() { return consequence; }
    public void setConsequence(String consequence) { this.consequence = consequence; }

    public Integer getTargetId() { return targetId; }
    public void setTargetId(Integer targetId) { this.targetId = targetId; }

    public Integer getValueInt() { return valueInt; }
    public void setValueInt(Integer valueInt) { this.valueInt = valueInt; }

    public Short getStoryWeight() { return storyWeight; }
    public void setStoryWeight(Short storyWeight) { this.storyWeight = storyWeight; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
}
