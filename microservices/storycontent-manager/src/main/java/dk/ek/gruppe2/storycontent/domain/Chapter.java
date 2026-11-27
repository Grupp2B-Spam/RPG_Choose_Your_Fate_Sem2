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
 * Not published as an event of its own. It reaches consumers as chapterId in the scene
 * payload and as startingChapterId plus startingSceneId in the race payload. The second is
 * why changing startingSceneId is not a local edit — see RaceService.
 *
 * startingSceneId is a plain integer, not a @ManyToOne Scene. Chapter and scene reference
 * each other, and mapping both directions makes the first chapter impossible to persist.
 * The database has no foreign key on the column for the same reason, so the service
 * validates the reference instead.
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
