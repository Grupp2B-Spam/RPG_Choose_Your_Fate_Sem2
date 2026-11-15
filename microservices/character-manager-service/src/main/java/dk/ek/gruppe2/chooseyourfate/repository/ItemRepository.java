package dk.ek.gruppe2.chooseyourfate.repository;

import dk.ek.gruppe2.chooseyourfate.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface ItemRepository extends JpaRepository<Item, Integer> {
    List<Item> findByType(String type);
}