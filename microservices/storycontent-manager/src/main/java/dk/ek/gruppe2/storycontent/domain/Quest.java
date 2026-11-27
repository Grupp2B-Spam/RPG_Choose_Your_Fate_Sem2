package dk.ek.gruppe2.storycontent.domain;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A quest offered in a scene, and the items an author promises for completing it.
 *
 * scene_id is mandatory, so a quest cannot exist without the scene that offers it. What is
 * state is the tracking of a quest per character, which lives in CharacterManager.
 *
 * rewardItems is the reward definition, not what a player holds.
 */
@Entity
@Table(name = "quest")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_id", nullable = false)
    private Scene scene;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "quest_reward_item",
            joinColumns = @JoinColumn(name = "quest_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id"))
    private Set<Item> rewardItems = new LinkedHashSet<>();

    protected Quest() {
        // for JPA
    }

    public Quest(Scene scene, String name, String description) {
        this.scene = scene;
        this.name = name;
        this.description = description;
    }

    public Integer getId() {
        return id;
    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
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

    public Set<Item> getRewardItems() {
        return rewardItems;
    }
}
