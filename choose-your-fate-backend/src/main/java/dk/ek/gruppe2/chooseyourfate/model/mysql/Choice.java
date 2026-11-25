package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

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
