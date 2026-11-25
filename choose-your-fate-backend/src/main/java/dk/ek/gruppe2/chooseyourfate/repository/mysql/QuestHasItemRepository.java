package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.QuestHasItem;
import dk.ek.gruppe2.chooseyourfate.model.mysql.QuestHasItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestHasItemRepository extends JpaRepository<QuestHasItem, QuestHasItemId> {
    List<QuestHasItem> findByQuest_Id(Integer questId);
    List<QuestHasItem> findByItem_Id(Integer itemId);
}