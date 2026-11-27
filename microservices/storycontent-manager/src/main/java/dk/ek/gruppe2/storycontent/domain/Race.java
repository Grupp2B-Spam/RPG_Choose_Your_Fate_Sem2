package dk.ek.gruppe2.storycontent.domain;

import dk.ek.gruppe2.storycontent.domain.Enums.DefinitionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A playable race, and the chapter its characters start in.
 *
 * It lives here because startingChapter is a mandatory reference into story content. Owning
 * races on the state side would mean a foreign key into this database.
 *
 * Published as race-definition.updated, which CharacterManager caches.
 */
@Entity
@Table(name = "race")
public class Race {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "starting_chapter_id", nullable = false)
    private Chapter startingChapter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DefinitionStatus status = DefinitionStatus.ACTIVE;

    /**
     * Bumped when the race changes, and also when its chapter's starting scene changes: the
     * published message carries that scene id. Forgetting it puts new characters in a scene
     * the author moved away from.
     */
    @Column(nullable = false)
    private int version = 1;

    protected Race() {
        // for JPA
    }

    public Race(String name, Chapter startingChapter) {
        this.name = name;
        this.startingChapter = startingChapter;
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

    public Chapter getStartingChapter() {
        return startingChapter;
    }

    public void setStartingChapter(Chapter startingChapter) {
        this.startingChapter = startingChapter;
    }

    public DefinitionStatus getStatus() {
        return status;
    }

    public void setStatus(DefinitionStatus status) {
        this.status = status;
    }

    public int getVersion() {
        return version;
    }
}
