package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

/**
 * MOVED: {@code choice_has_item} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>No counterpart: granting an item is one of five kinds of typed choice effect there,
 * rather than a table of its own.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — the effects on a choice.
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
