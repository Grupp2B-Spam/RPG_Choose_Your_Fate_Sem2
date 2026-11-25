package dk.ek.gruppe2.chooseyourfate.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The type of AI request to perform")
public enum AiRequestType {

    @Schema(description = "Generates a narrative summary of the character's journey and saves it to the database")
    PATH_SUMMARY,
    @Schema(description = "Generates an immersive second-person recap of the character's current state")
    CHARACTER_RECAP
}