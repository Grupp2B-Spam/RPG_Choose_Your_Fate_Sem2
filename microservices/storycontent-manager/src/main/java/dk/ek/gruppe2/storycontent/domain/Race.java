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
 * <p>{@code race_details} in the monolith. It sits between three claimants: it points
 * into {@link Chapter} (content), {@code npc} points at it (content), and
 * {@code character_avatar} points at it (state). It belongs here because
 * {@link #startingChapter} is a mandatory reference into story content — putting races in
 * CharacterManager would give CharacterManager a foreign key into this service's database,
 * which is the shared-database coupling the decomposition exists to remove. The reference
 * pointing the other way becomes a plain integer over there, which costs nothing because
 * CharacterManager already treats content as external. See ADR-015.
 *
 * <p>Published as {@code race-definition.updated}, which CharacterManager is expected to
 * cache rather than query per character creation (ADR-017).
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
     * Bumped when the race changes <em>and</em> when the starting scene of its chapter
     * changes, because the published payload denormalises that scene id. See ADR-017 —
     * this is the part of the design that is easy to forget and puts new characters in the
     * wrong scene when it is forgotten.
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
