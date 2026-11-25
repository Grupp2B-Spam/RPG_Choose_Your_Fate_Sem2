package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

@Entity
@Table(name = "inventory_has_item")
public class InventoryHasItem {

    @EmbeddedId
    private InventoryHasItemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("itemId")
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("inventoryId")
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    @Column(nullable = false)
    private Integer amount;

    public InventoryHasItem() {}

    public InventoryHasItem(Item item, Inventory inventory, Integer amount) {
        this.item = item;
        this.inventory = inventory;
        this.amount = amount;
        this.id = new InventoryHasItemId(item.getId(), inventory.getId());
    }

    public InventoryHasItemId getId() { return id; }
    public void setId(InventoryHasItemId id) { this.id = id; }

    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }

    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
}
