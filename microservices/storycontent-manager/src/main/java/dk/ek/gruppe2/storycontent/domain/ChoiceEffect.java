package dk.ek.gruppe2.storycontent.domain;

import dk.ek.gruppe2.storycontent.domain.Enums.CharacterStat;
import dk.ek.gruppe2.storycontent.domain.Enums.EffectKind;
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
 * One thing that happens when a choice is taken.
 *
 * <p>This replaces the monolith's {@code consequence} / {@code target_id} /
 * {@code value_int} triple. Inside a single process that triple was merely sloppy — the
 * code that wrote the integer was the code that read it. Across a service boundary it is
 * unusable: a consumer receiving {@code 5} cannot tell a quest from an item from an
 * amount, and the monolith's own data shows the convention was not followed anyway, with
 * {@code travel} rows carrying a {@code target_id} that is neither the destination nor the
 * source scene.
 *
 * <p>Typing it is what lets this service validate an effect before publishing it. That
 * matters beyond tidiness: the messaging convention is that a consumer reacts to an event
 * and never rejects it as invalid, which is only honest if the publisher was in a position
 * to check. See ADR-016.
 *
 * <p>Constructed through the factory methods, never field by field, so that an instance
 * with a kind and the wrong fields set cannot be built. The database carries the same rule
 * as a check constraint, because this entity is not the only thing that will ever write
 * the table.
 */
@Entity
@Table(name = "choice_effect")
public class ChoiceEffect {

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
    private EffectKind kind;

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "quest_id")
    private Integer questId;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private CharacterStat stat;

    /**
     * A delta, which may be negative. Deltas commute, which is what allows the consumer of
     * {@code player-choice.made} to run as competing consumers (ADR-011).
     */
    @Column
    private Integer amount;

    @Column(length = 64)
    private String flag;

    protected ChoiceEffect() {
        // for JPA
    }

    public static ChoiceEffect grantItem(Integer itemId, int ordinal) {
        ChoiceEffect effect = new ChoiceEffect();
        effect.kind = EffectKind.GRANT_ITEM;
        effect.itemId = itemId;
        effect.ordinal = ordinal;
        return effect;
    }

    public static ChoiceEffect grantQuest(Integer questId, int ordinal) {
        ChoiceEffect effect = new ChoiceEffect();
        effect.kind = EffectKind.GRANT_QUEST;
        effect.questId = questId;
        effect.ordinal = ordinal;
        return effect;
    }

    public static ChoiceEffect completeQuest(Integer questId, int ordinal) {
        ChoiceEffect effect = new ChoiceEffect();
        effect.kind = EffectKind.COMPLETE_QUEST;
        effect.questId = questId;
        effect.ordinal = ordinal;
        return effect;
    }

    public static ChoiceEffect modifyStat(CharacterStat stat, int amount, int ordinal) {
        ChoiceEffect effect = new ChoiceEffect();
        effect.kind = EffectKind.MODIFY_STAT;
        effect.stat = stat;
        effect.amount = amount;
        effect.ordinal = ordinal;
        return effect;
    }

    public static ChoiceEffect grantFlag(String flag, int ordinal) {
        ChoiceEffect effect = new ChoiceEffect();
        effect.kind = EffectKind.GRANT_FLAG;
        effect.flag = flag;
        effect.ordinal = ordinal;
        return effect;
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

    public EffectKind getKind() {
        return kind;
    }

    public Integer getItemId() {
        return itemId;
    }

    public Integer getQuestId() {
        return questId;
    }

    public CharacterStat getStat() {
        return stat;
    }

    public Integer getAmount() {
        return amount;
    }

    public String getFlag() {
        return flag;
    }
}
