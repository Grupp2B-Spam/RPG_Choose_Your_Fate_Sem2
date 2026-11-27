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
 * Typed rather than an untyped target id, so a malformed effect can be rejected before it
 * is published. A consumer receiving a bare 5 cannot tell a quest id from an item id from
 * an amount.
 *
 * Built through the factory methods, so an effect with the wrong fields for its kind cannot
 * be constructed. The database repeats the rule as a check constraint.
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
     * A delta, which may be negative. Deltas can be applied in any order, which is what lets
     * the consumer run as several instances.
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
