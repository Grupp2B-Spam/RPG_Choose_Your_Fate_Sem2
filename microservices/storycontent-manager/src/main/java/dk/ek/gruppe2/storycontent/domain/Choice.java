package dk.ek.gruppe2.storycontent.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * One option a player can take in a scene.
 *
 * <p>Part of the scene aggregate: never published on its own, always inside its scene.
 *
 * <p>{@link #beat} is one authored sentence saying what happens if this choice is taken.
 * It is copied into the player's history when the choice is made, so that history records
 * a fact rather than a pointer to content an author can later rewrite or delete.
 * Mandatory, here and in the database, because it cannot be filled in afterwards: that
 * would mean knowing what the scene said at the time, and nobody does.
 *
 * <p>It is also what makes the service split safe. A player's history keeps a choice id
 * with nothing enforcing it any more, and that is only affordable because reading the
 * history no longer requires looking the choice up.
 */
@Entity
@Table(name = "choice")
public class Choice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_id", nullable = false)
    private Scene scene;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_scene_id", nullable = false)
    private Scene destinationScene;

    /** What the player reads on the choice. The monolith called this {@code description}. */
    @Column(nullable = false, length = 255)
    private String label;

    @Column(nullable = false, length = 500)
    private String beat;

    /** Authoring weight, used when summarising a path. {@code story_weight} before. */
    @Column(nullable = false)
    private short weight = 1;

    /**
     * Presentation order within the scene. The monolith had none and sorted by primary
     * key, which means inserting a choice between two others was not expressible.
     */
    @Column(nullable = false)
    private int ordinal = 0;

    @OneToMany(mappedBy = "choice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordinal ASC, id ASC")
    private List<ChoiceRequirement> requirements = new ArrayList<>();

    @OneToMany(mappedBy = "choice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordinal ASC, id ASC")
    private List<ChoiceEffect> effects = new ArrayList<>();

    protected Choice() {
        // for JPA
    }

    public Choice(Scene destinationScene, String label, String beat, short weight, int ordinal) {
        this.destinationScene = destinationScene;
        this.label = label;
        this.beat = beat;
        this.weight = weight;
        this.ordinal = ordinal;
    }

    public void addRequirement(ChoiceRequirement requirement) {
        requirement.setChoice(this);
        requirements.add(requirement);
    }

    public void addEffect(ChoiceEffect effect) {
        effect.setChoice(this);
        effects.add(effect);
    }

    public void clearRequirements() {
        requirements.clear();
    }

    public void clearEffects() {
        effects.clear();
    }

    public Integer getId() {
        return id;
    }

    public Scene getScene() {
        return scene;
    }

    void setScene(Scene scene) {
        this.scene = scene;
    }

    public Scene getDestinationScene() {
        return destinationScene;
    }

    public void setDestinationScene(Scene destinationScene) {
        this.destinationScene = destinationScene;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getBeat() {
        return beat;
    }

    public void setBeat(String beat) {
        this.beat = beat;
    }

    public short getWeight() {
        return weight;
    }

    public void setWeight(short weight) {
        this.weight = weight;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public List<ChoiceRequirement> getRequirements() {
        return requirements;
    }

    public List<ChoiceEffect> getEffects() {
        return effects;
    }
}
