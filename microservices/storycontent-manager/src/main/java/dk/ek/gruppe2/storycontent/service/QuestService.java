package dk.ek.gruppe2.storycontent.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ek.gruppe2.storycontent.api.ApiExceptions.NotFoundException;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.QuestRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.QuestResponse;
import dk.ek.gruppe2.storycontent.domain.Item;
import dk.ek.gruppe2.storycontent.domain.Quest;
import dk.ek.gruppe2.storycontent.domain.Scene;
import dk.ek.gruppe2.storycontent.repository.ItemRepository;
import dk.ek.gruppe2.storycontent.repository.QuestRepository;
import dk.ek.gruppe2.storycontent.repository.SceneRepository;

/**
 * Authoring operations on quests.
 *
 * <p>A quest is written against the scene that offers it, and cannot exist without one.
 * That is what makes a quest content, rather than the authored half of a player's quest
 * progress — the progress is state and lives in CharacterManager.
 *
 * <p>The scene message carries quest <em>ids</em> and nothing else about a quest, so:
 *
 * <ul>
 *   <li>Creating or deleting one changes the scene's quest ids, so the scene is
 *       republished.</li>
 *   <li>Editing its text or reward items changes nothing any consumer receives, so
 *       nothing is published.</li>
 * </ul>
 *
 * <p>The second point is a known hole, not a quirk of this class: CharacterManager needs
 * a quest's reward items in order to grant them and currently has no way to get them. It
 * read the table directly when everything was one database. Fixing it means adding to the
 * published contract, which other teams build against, so it needs agreeing rather than
 * committing.
 */
@Service
public class QuestService {

    private final QuestRepository quests;
    private final SceneRepository scenes;
    private final ItemRepository items;
    private final ContentEventDispatcher dispatcher;

    public QuestService(
            QuestRepository quests,
            SceneRepository scenes,
            ItemRepository items,
            ContentEventDispatcher dispatcher) {
        this.quests = quests;
        this.scenes = scenes;
        this.items = items;
        this.dispatcher = dispatcher;
    }

    @Transactional(readOnly = true)
    public List<QuestResponse> findAllInScene(Integer sceneId) {
        requireScene(sceneId);
        List<QuestResponse> result = new ArrayList<>();
        for (Quest quest : quests.findAllBySceneId(sceneId)) {
            result.add(toResponse(quest));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public QuestResponse findById(Integer id) {
        return toResponse(require(id));
    }

    @Transactional
    public QuestResponse create(Integer sceneId, QuestRequest request) {
        Scene scene = requireScene(sceneId);
        Quest quest = new Quest(scene, request.name(), request.description());
        quest.getRewardItems().addAll(resolveRewards(request.rewardItemIds()));
        quests.save(quest);

        // Built before the version is incremented, not after. incrementVersion runs as a
        // bulk update and clears the persistence context, which detaches this quest along
        // with everything else — and reading its lazy scene or reward items afterwards
        // would fail rather than simply be stale.
        QuestResponse response = toResponse(quest);

        // questIds changes, so the scene's consumers can see this.
        scenes.incrementVersion(sceneId);
        dispatcher.sceneChanged(sceneId);
        return response;
    }

    /** Publishes nothing: no part of a quest other than its id reaches a consumer today. */
    @Transactional
    public QuestResponse update(Integer id, QuestRequest request) {
        Quest quest = require(id);
        quest.setName(request.name());
        quest.setDescription(request.description());
        quest.getRewardItems().clear();
        quest.getRewardItems().addAll(resolveRewards(request.rewardItemIds()));
        quests.save(quest);
        return toResponse(quest);
    }

    @Transactional
    public void delete(Integer id) {
        Quest quest = require(id);
        Integer sceneId = quest.getScene().getId();
        quests.delete(quest);

        scenes.incrementVersion(sceneId);
        dispatcher.sceneChanged(sceneId);
    }

    private Set<Item> resolveRewards(List<Integer> itemIds) {
        Set<Item> rewards = new LinkedHashSet<>();
        if (itemIds == null) {
            return rewards;
        }
        for (Integer itemId : itemIds) {
            rewards.add(items.findById(itemId)
                    .orElseThrow(() -> new NotFoundException("Item " + itemId + " does not exist")));
        }
        return rewards;
    }

    private Quest require(Integer id) {
        return quests.findById(id)
                .orElseThrow(() -> new NotFoundException("Quest " + id + " does not exist"));
    }

    private Scene requireScene(Integer id) {
        return scenes.findById(id)
                .orElseThrow(() -> new NotFoundException("Scene " + id + " does not exist"));
    }

    private QuestResponse toResponse(Quest quest) {
        List<Integer> rewardItemIds = new ArrayList<>();
        for (Item item : quest.getRewardItems()) {
            rewardItemIds.add(item.getId());
        }
        rewardItemIds.sort(Integer::compareTo);
        return new QuestResponse(
                quest.getId(),
                quest.getScene().getId(),
                quest.getName(),
                quest.getDescription(),
                rewardItemIds);
    }
}
