package dk.ek.gruppe2.storycontent.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import dk.ek.gruppe2.storycontent.domain.Choice;
import dk.ek.gruppe2.storycontent.domain.ChoiceEffect;
import dk.ek.gruppe2.storycontent.domain.ChoiceRequirement;
import dk.ek.gruppe2.storycontent.domain.Enums.CharacterStat;
import dk.ek.gruppe2.storycontent.domain.Enums.EffectKind;
import dk.ek.gruppe2.storycontent.domain.Item;
import dk.ek.gruppe2.storycontent.domain.Npc;
import dk.ek.gruppe2.storycontent.domain.Quest;
import dk.ek.gruppe2.storycontent.domain.Race;
import dk.ek.gruppe2.storycontent.domain.Scene;
import dk.ek.gruppe2.storycontent.messaging.payload.ChoicePayload;
import dk.ek.gruppe2.storycontent.messaging.payload.EffectPayload;
import dk.ek.gruppe2.storycontent.messaging.payload.ItemDefinitionPayload;
import dk.ek.gruppe2.storycontent.messaging.payload.RaceDefinitionPayload;
import dk.ek.gruppe2.storycontent.messaging.payload.RequirementPayload;
import dk.ek.gruppe2.storycontent.messaging.payload.ScenePayload;

/**
 * Turns entities into the exact shapes the published contract describes.
 *
 * <p>Kept separate from both the entities and the services so that there is one place
 * where the wire format is produced. The contract is a document other teams build
 * against; scattering the mapping across whichever service happens to publish would mean
 * the shape is defined in several places and can drift between them.
 */
@Component
public class PayloadAssembler {

    public ScenePayload scene(Scene scene) {
        List<ChoicePayload> choices = new ArrayList<>();
        for (Choice choice : scene.getChoices()) {
            choices.add(choice(choice));
        }

        List<Integer> npcIds = new ArrayList<>();
        for (Npc npc : scene.getNpcs()) {
            npcIds.add(npc.getId());
        }
        npcIds.sort(Integer::compareTo);

        List<Integer> questIds = new ArrayList<>();
        for (Quest quest : scene.getQuests()) {
            questIds.add(quest.getId());
        }
        questIds.sort(Integer::compareTo);

        return new ScenePayload(
                scene.getId(),
                scene.getVersion(),
                scene.getStatus().name(),
                scene.getChapter().getId(),
                scene.getName(),
                scene.getDescription(),
                choices,
                npcIds,
                questIds);
    }

    private ChoicePayload choice(Choice choice) {
        List<RequirementPayload> requirements = new ArrayList<>();
        for (ChoiceRequirement requirement : choice.getRequirements()) {
            requirements.add(new RequirementPayload(
                    requirement.getKind().name(),
                    requirement.getFlag(),
                    requirement.getItemId()));
        }

        List<EffectPayload> effects = new ArrayList<>();
        List<Integer> grantedItemIds = new ArrayList<>();
        for (ChoiceEffect effect : choice.getEffects()) {
            effects.add(new EffectPayload(
                    effect.getKind().name(),
                    effect.getItemId(),
                    effect.getQuestId(),
                    statName(effect.getStat()),
                    effect.getAmount(),
                    effect.getFlag()));

            // grantedItemIds is the pre-1.1.0 spelling of the GRANT_ITEM effects. Derived
            // here rather than stored, so the two cannot disagree: a consumer reading the
            // old field and one reading the new one see the same thing by construction.
            if (effect.getKind() == EffectKind.GRANT_ITEM) {
                grantedItemIds.add(effect.getItemId());
            }
        }

        return new ChoicePayload(
                choice.getId(),
                choice.getLabel(),
                choice.getBeat(),
                choice.getDestinationScene().getId(),
                choice.getWeight(),
                grantedItemIds,
                requirements,
                effects);
    }

    public ItemDefinitionPayload item(Item item) {
        return new ItemDefinitionPayload(
                item.getId(),
                item.getVersion(),
                item.getStatus().name(),
                item.getType().name(),
                item.getSlot() == null ? null : item.getSlot().name(),
                stats(item.getRequirements()),
                stats(item.getModifiers()));
    }

    public RaceDefinitionPayload race(Race race) {
        return new RaceDefinitionPayload(
                race.getId(),
                race.getVersion(),
                race.getStatus().name(),
                race.getName(),
                race.getStartingChapter().getId(),
                // May legitimately be null while a chapter has no scenes yet. That is an
                // unfinished-content state, and the contract says so rather than this
                // service inventing a value a consumer would then act on.
                race.getStartingChapter().getStartingSceneId());
    }

    private Map<String, Integer> stats(Map<CharacterStat, Integer> source) {
        Map<String, Integer> result = new LinkedHashMap<>();
        source.forEach((stat, amount) -> result.put(statName(stat), amount));
        return result;
    }

    /**
     * Statistic names go on the wire in lower case, unlike every other enumeration in the
     * contract.
     *
     * <p>Not an inconsistency we introduced and not one worth fixing. Version 1.0.0 of the
     * contract already published statistic names in lower case, as the keys of an item's
     * requirements and modifiers. When the same vocabulary was needed for a choice's
     * {@code MODIFY_STAT} effect, there were two options: spell the new field like the
     * other enumerations and have the document name statistics two ways, or spell it like
     * the statistics already published and have one enumeration differ from the rest.
     *
     * <p>The second is the smaller inconsistency and, more to the point, the only one of
     * the two that is free: normalising the older field would break every consumer already
     * parsing it, for a cosmetic gain.
     */
    private String statName(CharacterStat stat) {
        return stat == null ? null : stat.name().toLowerCase(Locale.ROOT);
    }
}
