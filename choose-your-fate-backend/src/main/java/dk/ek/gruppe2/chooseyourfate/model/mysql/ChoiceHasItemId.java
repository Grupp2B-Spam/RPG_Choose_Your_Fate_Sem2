package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * MIGRATED to StoryContentManager: {@code choice_has_item} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>It has no direct counterpart there. A granted item is one authored effect among
 * five, so it lives in {@code choice_effect} with the others as a
 * {@code GRANT_ITEM} row rather than in a table that can only express one kind.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * the {@code effects} list on a choice — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
@Embeddable
public class ChoiceHasItemId implements Serializable {

    private Integer choiceId;
    private Integer itemId;

    public ChoiceHasItemId() {}

    public ChoiceHasItemId(Integer choiceId, Integer itemId) {
        this.choiceId = choiceId;
        this.itemId = itemId;
    }

    public Integer getChoiceId() { return choiceId; }
    public void setChoiceId(Integer choiceId) { this.choiceId = choiceId; }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChoiceHasItemId that)) return false;
        return Objects.equals(choiceId, that.choiceId) &&
                Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(choiceId, itemId);
    }
}
