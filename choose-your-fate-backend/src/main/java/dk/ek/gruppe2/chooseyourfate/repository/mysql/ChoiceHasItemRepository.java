package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.ChoiceHasItem;
import dk.ek.gruppe2.chooseyourfate.model.mysql.ChoiceHasItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChoiceHasItemRepository extends JpaRepository<ChoiceHasItem, ChoiceHasItemId> {
    List<ChoiceHasItem> findByChoice_Id(Integer choiceId);
    List<ChoiceHasItem> findByItem_Id(Integer itemId);
}