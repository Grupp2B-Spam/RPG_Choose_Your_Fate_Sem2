package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
/**
 * MOVED: {@code item} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>Published as {@code item-definition.updated}. Its schema also splits what an item is
 * from where it is worn, which this {@code type} column mixes together.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — {@code /admin/v1/items}.
 */
public interface ItemRepository extends JpaRepository<Item, Integer> {
    List<Item> findByType(String type);
}