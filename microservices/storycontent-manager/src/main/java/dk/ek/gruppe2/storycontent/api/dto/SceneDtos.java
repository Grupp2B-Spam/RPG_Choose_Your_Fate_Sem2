package dk.ek.gruppe2.storycontent.api.dto;

import java.util.List;

import dk.ek.gruppe2.storycontent.domain.Enums.CharacterStat;
import dk.ek.gruppe2.storycontent.domain.Enums.EffectKind;
import dk.ek.gruppe2.storycontent.domain.Enums.RequirementKind;
import dk.ek.gruppe2.storycontent.domain.Enums.SceneStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The authoring API's view of a scene. Kept separate from messaging.payload, which is the
 * published wire format: that one belongs to the contract and cannot change without
 * telling the other teams.
 */
public final class SceneDtos {

    private SceneDtos() {
    }

    public record SceneRequest(
            @NotNull(message = "chapterId is required") Integer chapterId,
            @NotBlank(message = "name is required") @Size(max = 100) String name,
            String description,
            SceneStatus status) {
    }

    public record SceneResponse(
            Integer id,
            Integer chapterId,
            String name,
            String description,
            SceneStatus status,
            int version,
            List<ChoiceResponse> choices,
            List<Integer> npcIds,
            List<Integer> questIds) {
    }

    /**
     * A choice is written through its scene, never on its own, so an edit cannot miss the
     * sceneVersion bump.
     */
    public record ChoiceRequest(
            @NotNull(message = "destinationSceneId is required") Integer destinationSceneId,
            @NotBlank(message = "label is required") @Size(max = 255) String label,

            // Mandatory. A beat cannot be filled in later: that would mean knowing what
            // the scene said at the time.
            @NotBlank(message = "beat is required: one sentence describing what happens if "
                    + "this choice is taken. It is copied into the player's history and "
                    + "cannot be added afterwards")
            @Size(max = 500) String beat,

            Short weight,
            Integer ordinal,
            @Valid List<RequirementRequest> requirements,
            @Valid List<EffectRequest> effects) {
    }

    public record ChoiceResponse(
            Integer id,
            Integer destinationSceneId,
            String label,
            String beat,
            short weight,
            int ordinal,
            List<RequirementResponse> requirements,
            List<EffectResponse> effects) {
    }

    public record RequirementRequest(
            @NotNull(message = "kind is required") RequirementKind kind,
            String flag,
            Integer itemId) {
    }

    public record RequirementResponse(
            RequirementKind kind,
            String flag,
            Integer itemId) {
    }

    public record EffectRequest(
            @NotNull(message = "kind is required") EffectKind kind,
            Integer itemId,
            Integer questId,
            CharacterStat stat,
            Integer amount,
            String flag) {
    }

    public record EffectResponse(
            EffectKind kind,
            Integer itemId,
            Integer questId,
            CharacterStat stat,
            Integer amount,
            String flag) {
    }

    /** Replaces the whole set of NPCs standing in a scene. */
    public record SceneNpcsRequest(
            @NotNull(message = "npcIds is required; send an empty list to clear")
            List<Integer> npcIds) {
    }
}
