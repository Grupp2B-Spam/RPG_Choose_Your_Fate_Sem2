package dk.ek.gruppe2.storycontent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A chapter groups scenes and names where a race's characters begin.
 *
 * <p>A chapter is not published as an event of its own. It reaches consumers twice over:
 * {@code chapterId} inside the scene payload, and {@code startingChapterId} plus
 * {@code startingSceneId} inside the race payload. The second is why changing
 * {@link #startingSceneId} is not a local edit — see
 * {@code RaceService#republishRacesStartingIn}.
 *
 * <p>{@link #startingSceneId} is a plain integer rather than a {@code @ManyToOne Scene}.
 * Chapter and scene reference each other, and mapping both directions as associations
 * makes it impossible to persist the first chapter: the scene needs a chapter and the
 * chapter wants the scene. The monolith solved it by making the column nullable, and the
 * database here has no foreign key on it for the same reason. Keeping it as an integer in
 * the entity makes that honest rather than hiding a half-mapped association, and the
 * service validates the reference where it can produce a readable error.
 */
@Entity
@Table(name = "chapter")
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "starting_scene_id")
    private Integer startingSceneId;

    protected Chapter() {
        // for JPA
    }

    public Chapter(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStartingSceneId() {
        return startingSceneId;
    }

    public void setStartingSceneId(Integer startingSceneId) {
        this.startingSceneId = startingSceneId;
    }
}
