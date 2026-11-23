package dk.ek.gruppe2.storycontent.api.dto;

import java.util.List;
import java.util.Map;

import dk.ek.gruppe2.storycontent.domain.Enums.CharacterStat;
import dk.ek.gruppe2.storycontent.domain.Enums.DefinitionStatus;
import dk.ek.gruppe2.storycontent.domain.Enums.EquipmentSlot;
import dk.ek.gruppe2.storycontent.domain.Enums.ItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request and response shapes for the authoring API, other than scenes. */
public final class ContentDtos {

    private ContentDtos() {
    }

    // --- chapter -------------------------------------------------------------------

    public record ChapterRequest(
            @NotBlank(message = "name is required") @Size(max = 100) String name) {
    }

    public record ChapterResponse(
            Integer id,
            String name,
            Integer startingSceneId) {
    }

    /**
     * Sets a chapter's starting scene.
     *
     * <p>Its own endpoint rather than a field on {@link ChapterRequest}, because it is not
     * an ordinary edit. The starting scene is denormalised into every race that begins in
     * this chapter, so changing it republishes those races with a new {@code raceVersion}
     * (ADR-017). Having it as a separate, named operation is what stops that becoming a
     * side effect nobody expects from "rename a chapter".
     */
    public record StartingSceneRequest(
            @NotNull(message = "sceneId is required") Integer sceneId) {
    }

    // --- item ----------------------------------------------------------------------

    public record ItemRequest(
            @NotBlank(message = "name is required") @Size(max = 100) String name,
            String description,
            @NotNull(message = "type is required") ItemType type,

            // Validated against the type in the service: equipment needs a slot and
            // nothing else may have one. The database carries the same rule as a check
            // constraint, because the service will not be the only thing that ever writes
            // this table.
            EquipmentSlot slot,

            Map<CharacterStat, Integer> requirements,
            Map<CharacterStat, Integer> modifiers) {
    }

    public record ItemResponse(
            Integer id,
            String name,
            String description,
            ItemType type,
            EquipmentSlot slot,
            DefinitionStatus status,
            int version,
            Map<CharacterStat, Integer> requirements,
            Map<CharacterStat, Integer> modifiers) {
    }

    // --- race ----------------------------------------------------------------------

    public record RaceRequest(
            @NotBlank(message = "name is required") @Size(max = 100) String name,
            @NotNull(message = "startingChapterId is required") Integer startingChapterId) {
    }

    public record RaceResponse(
            Integer id,
            String name,
            Integer startingChapterId,
            Integer startingSceneId,
            DefinitionStatus status,
            int version) {
    }

    // --- npc -----------------------------------------------------------------------

    public record NpcRequest(
            @NotBlank(message = "name is required") @Size(max = 100) String name,
            @NotNull(message = "raceId is required") Integer raceId) {
    }

    public record NpcResponse(
            Integer id,
            String name,
            Integer raceId) {
    }

    // --- quest ---------------------------------------------------------------------

    public record QuestRequest(
            @NotBlank(message = "name is required") @Size(max = 100) String name,
            String description,
            List<Integer> rewardItemIds) {
    }

    public record QuestResponse(
            Integer id,
            Integer sceneId,
            String name,
            String description,
            List<Integer> rewardItemIds) {
    }
}
