package dk.ek.gruppe2.storycontent.domain;

import dk.ek.gruppe2.storycontent.domain.Enums.RequirementKind;
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
 * One condition that must hold before a choice is offered.
 *
 * <p>The monolith kept these as JSON on the choice:
 * {@code {"requires": ["festival-access"], "grants": [...]}}, mixed with entries shaped
 * like {@code "item:6"} — two vocabularies in one text field, neither of them checked.
 *
 * <p>A story flag's <em>name</em> is authored here; whether a character holds it is state
 * owned by CharacterManager. Neither side owns a table of flag names, so the string is a
 * vocabulary shared across the boundary. It travels verbatim in the event, which is what
 * lets the state side evaluate the condition without calling back into this service.
 */
@Entity
@Table(name = "choice_requirement")
public class ChoiceRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "choice_id", nullable = false)
    private Choice choice;

    @Column(nullable = false)
    private int ordinal = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RequirementKind kind;

    @Column(length = 64)
    private String flag;

    /**
     * A plain integer, not a {@code @ManyToOne Item}. The database has the foreign key, so
     * the reference is enforced; mapping it as an association here would pull the whole
     * item into memory to publish an id, on every scene that has an item requirement.
     */
    @Column(name = "item_id")
    private Integer itemId;

    protected ChoiceRequirement() {
        // for JPA
    }

    public static ChoiceRequirement storyFlag(String flag, int ordinal) {
        ChoiceRequirement requirement = new ChoiceRequirement();
        requirement.kind = RequirementKind.STORY_FLAG;
        requirement.flag = flag;
        requirement.ordinal = ordinal;
        return requirement;
    }

    public static ChoiceRequirement item(Integer itemId, int ordinal) {
        ChoiceRequirement requirement = new ChoiceRequirement();
        requirement.kind = RequirementKind.ITEM;
        requirement.itemId = itemId;
        requirement.ordinal = ordinal;
        return requirement;
    }

    public Integer getId() {
        return id;
    }

    public Choice getChoice() {
        return choice;
    }

    void setChoice(Choice choice) {
        this.choice = choice;
    }

    public int getOrdinal() {
        return ordinal;
    }

    public RequirementKind getKind() {
        return kind;
    }

    public String getFlag() {
        return flag;
    }

    public Integer getItemId() {
        return itemId;
    }
}
