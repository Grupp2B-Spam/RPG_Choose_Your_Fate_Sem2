package dk.ek.gruppe2.chooseyourfate.dto;

import dk.ek.gruppe2.chooseyourfate.enums.AiRequestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request body for AI endpoints")
public class AiRequestDTO {

    @Schema(
        description = "The type of AI operation to perform",
        allowableValues = {"CHARACTER_RECAP", "PATH_SUMMARY"},
        example = "CHARACTER_RECAP"
    )
    @NotNull(message = "requestType is required")
    private AiRequestType requestType;

    @Schema(description = "The ID of the character to process", example = "1")
    @NotNull(message = "characterId is required")
    private Integer characterId;
}
