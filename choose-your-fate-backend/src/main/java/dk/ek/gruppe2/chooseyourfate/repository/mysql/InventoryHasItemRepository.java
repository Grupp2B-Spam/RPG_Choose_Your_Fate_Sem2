package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.InventoryHasItem;
import dk.ek.gruppe2.chooseyourfate.model.mysql.InventoryHasItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryHasItemRepository extends JpaRepository<InventoryHasItem, InventoryHasItemId> {
    List<InventoryHasItem> findByInventory_Id(Integer inventoryId);
    List<InventoryHasItem> findByItem_Id(Integer itemId);

    boolean existsByInventoryIdAndItemId(Integer inventoryId, Integer itemId);
}