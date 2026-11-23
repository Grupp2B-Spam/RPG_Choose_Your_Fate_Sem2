package dk.ek.gruppe2.storycontent.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.ek.gruppe2.storycontent.api.ApiExceptions.InvalidContentException;
import dk.ek.gruppe2.storycontent.api.ApiExceptions.NotFoundException;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.ItemRequest;
import dk.ek.gruppe2.storycontent.api.dto.ContentDtos.ItemResponse;
import dk.ek.gruppe2.storycontent.domain.Enums.CharacterStat;
import dk.ek.gruppe2.storycontent.domain.Enums.DefinitionStatus;
import dk.ek.gruppe2.storycontent.domain.Enums.EquipmentSlot;
import dk.ek.gruppe2.storycontent.domain.Enums.ItemType;
import dk.ek.gruppe2.storycontent.domain.Item;
import dk.ek.gruppe2.storycontent.repository.ItemRepository;

@Service
public class ItemService {

    private final ItemRepository items;
    private final ContentEventDispatcher dispatcher;

    public ItemService(ItemRepository items, ContentEventDispatcher dispatcher) {
        this.items = items;
        this.dispatcher = dispatcher;
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> findAll() {
        List<ItemResponse> result = new ArrayList<>();
        for (Item item : items.findAll()) {
            result.add(toResponse(item));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ItemResponse findById(Integer id) {
        return toResponse(require(id));
    }

    @Transactional
    public ItemResponse create(ItemRequest request) {
        validateSlot(request.type(), request.slot());
        Item item = new Item(request.name(), request.description(), request.type(), request.slot());
        copyStats(request.requirements(), item.getRequirements());
        copyStats(request.modifiers(), item.getModifiers());
        items.save(item);

        dispatcher.itemChanged(item.getId());
        return toResponse(item);
    }

    @Transactional
    public ItemResponse update(Integer id, ItemRequest request) {
        validateSlot(request.type(), request.slot());
        Item item = require(id);
        item.setName(request.name());
        item.setDescription(request.description());
        item.setType(request.type());
        item.setSlot(request.slot());
        item.getRequirements().clear();
        item.getModifiers().clear();
        copyStats(request.requirements(), item.getRequirements());
        copyStats(request.modifiers(), item.getModifiers());
        items.save(item);
        return bumpAndPublish(id);
    }

    /**
     * Withdraws an item from being granted or acquired.
     *
     * <p>Not a delete, and the contract says so explicitly: a player who already owns a
     * retired item must still be able to see and use it, so the definition has to keep
     * resolving on the consumer's side. Deleting it would leave CharacterManager holding
     * inventory rows it cannot explain — and with the foreign key gone, nothing in the
     * database would have stopped it.
     */
    @Transactional
    public ItemResponse retire(Integer id) {
        Item item = require(id);
        item.setStatus(DefinitionStatus.RETIRED);
        items.save(item);
        return bumpAndPublish(id);
    }

    private ItemResponse bumpAndPublish(Integer id) {
        items.incrementVersion(id);
        dispatcher.itemChanged(id);
        return toResponse(require(id));
    }

    /**
     * Equipment is worn somewhere; nothing else is.
     *
     * <p>The monolith could not state this, because its {@code type} column conflated what
     * a thing is with where it goes — {@code ARMOR_HEAD} is both at once. Separating them
     * is what makes the rule expressible, and the rule is worth having because the slot is
     * published: an equipment item with no slot is one CharacterManager cannot place.
     */
    private void validateSlot(ItemType type, EquipmentSlot slot) {
        if (type == ItemType.EQUIPMENT && slot == null) {
            throw new InvalidContentException("An EQUIPMENT item needs a slot");
        }
        if (type != ItemType.EQUIPMENT && slot != null) {
            throw new InvalidContentException("Only an EQUIPMENT item may have a slot");
        }
    }

    private void copyStats(Map<CharacterStat, Integer> source, Map<CharacterStat, Integer> target) {
        if (source == null) {
            return;
        }
        source.forEach((stat, amount) -> {
            if (amount != null) {
                target.put(stat, amount);
            }
        });
    }

    private Item require(Integer id) {
        return items.findById(id)
                .orElseThrow(() -> new NotFoundException("Item " + id + " does not exist"));
    }

    private ItemResponse toResponse(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getType(),
                item.getSlot(),
                item.getStatus(),
                item.getVersion(),
                // Copied into a plain map rather than an EnumMap: Hibernate replaces the
                // field with its own collection wrapper, and EnumMap's copy constructor
                // rejects an empty non-EnumMap source.
                new LinkedHashMap<>(item.getRequirements()),
                new LinkedHashMap<>(item.getModifiers()));
    }
}
