package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

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
