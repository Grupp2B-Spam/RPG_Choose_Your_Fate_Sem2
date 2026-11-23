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
 * The authoring API's view of a scene.
 *
 * <p>Separate from {@code messaging.payload}, which is the wire format of the published
 * event. They overlap today, and keeping them as one type would be less code — but they
 * change for different reasons and are owned by different people. The event shape belongs
 * to the contract and cannot be altered without telling the other teams; this shape
 * belongs to whoever writes the admin UI. Merging them would mean a convenience field
 * added for an author silently becomes part of a published contract.
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
     * A choice is written through its scene, never on its own.
     *
     * <p>That is not an accident of the URL layout. A choice is part of the scene
     * aggregate and cannot change without changing the scene's published event, so routing
     * every edit through the scene is what makes it impossible to change a choice and
     * forget to bump {@code sceneVersion}.
     */
    public record ChoiceRequest(
            @NotNull(message = "destinationSceneId is required") Integer destinationSceneId,
            @NotBlank(message = "label is required") @Size(max = 255) String label,

            // Mandatory, and the API is the right place to insist on it. ADR-007 calls the
            // beat a workflow requirement on the writing team rather than a schema detail:
            // it cannot be backfilled, because reconstructing it later would need to know
            // what the scene said at the time. A beat that is optional at the point of
            // authoring is a beat that is null for everything written before someone
            // remembered to fill it in.
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
