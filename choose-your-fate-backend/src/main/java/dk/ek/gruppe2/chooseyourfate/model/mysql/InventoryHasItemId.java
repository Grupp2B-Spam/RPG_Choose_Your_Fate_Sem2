package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class InventoryHasItemId implements Serializable {

    private Integer itemId;
    private Integer inventoryId;

    public InventoryHasItemId() {}

    public InventoryHasItemId(Integer itemId, Integer inventoryId) {
        this.itemId = itemId;
        this.inventoryId = inventoryId;
    }

    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }

    public Integer getInventoryId() { return inventoryId; }
    public void setInventoryId(Integer inventoryId) { this.inventoryId = inventoryId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryHasItemId that)) return false;
        return Objects.equals(itemId, that.itemId) &&
                Objects.equals(inventoryId, that.inventoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, inventoryId);
    }
}
