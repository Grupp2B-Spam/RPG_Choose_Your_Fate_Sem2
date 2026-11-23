package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.ChoiceHasItem;
import dk.ek.gruppe2.chooseyourfate.model.mysql.ChoiceHasItemId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MIGRATED to StoryContentManager: {@code choice_has_item} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>It has no direct counterpart there. A granted item is one authored effect among
 * five, so it lives in {@code choice_effect} with the others as a
 * {@code GRANT_ITEM} row rather than in a table that can only express one kind.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * the {@code effects} list on a choice — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
public interface ChoiceHasItemRepository extends JpaRepository<ChoiceHasItem, ChoiceHasItemId> {
    List<ChoiceHasItem> findByChoice_Id(Integer choiceId);
    List<ChoiceHasItem> findByItem_Id(Integer itemId);
}