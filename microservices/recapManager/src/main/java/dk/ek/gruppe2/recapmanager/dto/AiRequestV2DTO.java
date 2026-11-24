package dk.ek.gruppe2.recapmanager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "AI request with optional narration")
public class AiRequestV2DTO extends AiRequestDTO {
    @Schema(description = "Retrieve or generate audio for the returned narrative", defaultValue = "false")
    private boolean audio;
}
