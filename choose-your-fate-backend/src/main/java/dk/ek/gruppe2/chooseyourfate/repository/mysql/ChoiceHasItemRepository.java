package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.ChoiceHasItem;
import dk.ek.gruppe2.chooseyourfate.model.mysql.ChoiceHasItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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
public interface ChoiceHasItemRepository extends JpaRepository<ChoiceHasItem, ChoiceHasItemId> {
    List<ChoiceHasItem> findByChoice_Id(Integer choiceId);
    List<ChoiceHasItem> findByItem_Id(Integer itemId);
}