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
 * <p>Called {@code race_details} in the monolith. Three services could have claimed it,
 * and it lives here because {@link #startingChapter} is a mandatory reference into story
 * content: owning races anywhere else would mean owning a foreign key into this database,
 * which is exactly the coupling we split the system up to remove. The reverse reference —
 * a character pointing at its race — is just an integer on the other side, which costs
 * nothing.
 *
 * <p>Published as {@code race-definition.updated}, which CharacterManager caches instead
 * of asking us per character creation.
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
     * Bumped when the race changes, and also when its chapter's starting scene changes —
     * because the published message carries that scene id, so editing the chapter changes
     * the right answer for every race starting there.
     *
     * <p>Easy to forget, and forgetting it puts new characters in a scene the author moved
     * away from.
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
