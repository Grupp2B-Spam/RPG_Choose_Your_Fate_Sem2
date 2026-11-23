package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

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
@Entity
@Table(name = "choice_has_item")
public class ChoiceHasItem {

    @EmbeddedId
    private ChoiceHasItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("choiceId")
    @JoinColumn(name = "choice_id")
    private Choice choice;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("itemId")
    @JoinColumn(name = "item_id")
    private Item item;

    public ChoiceHasItem() {}

    public ChoiceHasItem(Choice choice, Item item) {
        this.choice = choice;
        this.item = item;
        this.id = new ChoiceHasItemId(choice.getId(), item.getId());
    }

    public ChoiceHasItemId getId() { return id; }
    public void setId(ChoiceHasItemId id) { this.id = id; }

    public Choice getChoice() { return choice; }
    public void setChoice(Choice choice) { this.choice = choice; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
}
