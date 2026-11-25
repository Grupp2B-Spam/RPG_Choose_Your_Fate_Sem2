package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Integer> {
    Inventory findByCharacter_Id(Integer characterId);
}
