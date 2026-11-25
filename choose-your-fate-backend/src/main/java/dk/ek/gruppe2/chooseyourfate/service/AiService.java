package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.ai.AiTools;
import dk.ek.gruppe2.chooseyourfate.config.AiDeploymentProperties;
import dk.ek.gruppe2.chooseyourfate.dto.AiRequestDTO;
import dk.ek.gruppe2.chooseyourfate.enums.AiRequestType;
import dk.ek.gruppe2.chooseyourfate.exception.AiServiceException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiService {

    private final ChatClient chatClient;
    private final AiTools aiTools;
    private final AiDeploymentProperties aiDeploymentProperties;

    public AiService(
        ChatClient.Builder chatClientBuilder,
        AiTools aiTools,
        AiDeploymentProperties aiDeploymentProperties
    ) {
        this.chatClient = chatClientBuilder.build();
        this.aiTools = aiTools;
        this.aiDeploymentProperties = aiDeploymentProperties;
    }

    public String handleRequest(AiRequestDTO request) {
        validateRequest(request);

        if (!aiDeploymentProperties.isEnabled()) {
            throw new AiServiceException("AI is currently disabled for this deployment.");
        }

        try {
            String raw = chatClient.prompt()
                .system(resolveSystemPrompt(request.getRequestType()))
                .user(buildUserMessage(request))
                .tools(aiTools)
                .call()
                .content();

            if (raw == null) {
                return "";
            }

            return aiDeploymentProperties.isStripMarkdown() ? stripMarkdown(raw) : raw.trim();
        } catch (Exception ex) {
            throw new AiServiceException(
                "AI provider request failed. Check AI deployment configuration and provider availability.",
                ex
            );
        }
    }

    private String stripMarkdown(String text) {
        return text
            .replaceAll("\\*\\*([^*]+)\\*\\*", "$1")
            .replaceAll("\\*([^*]+)\\*", "$1")
            .replaceAll("(?m)^#{1,6}\\s*", "")
            .trim();
    }

    private void validateRequest(AiRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        if (request.getRequestType() == null) {
            throw new IllegalArgumentException("requestType is required.");
        }
        if (request.getCharacterId() == null) {
            throw new IllegalArgumentException("characterId is required.");
        }
    }

    private String resolveSystemPrompt(AiRequestType type) {
        return switch (type) {
            case CHARACTER_RECAP -> """
                You are a narrator in a fantasy RPG called "Choose Your Fate".
                Your task is to write a brief, immersive character recap in second person (e.g., "You are...").

                Rules:
                - Call getCharacterInfo to retrieve character data before answering.
                - Only state facts from the tool result. Do not invent stats or lore.
                - Keep the recap to 3-5 sentences.
                - Write in an engaging, fantasy-RPG tone.
                """;
            case PATH_SUMMARY -> """
                You are a chronicler in a fantasy RPG called "Choose Your Fate".
                Your task is to write a narrative summary of the character's journey and save it.

                Rules:
                - Call getCharacterPathHistory to read the character's journey before doing anything else.
                - Write a compelling, lore-appropriate narrative summary (2-4 sentences, past tense, third person).
                - Only describe events that appear in the tool result. Do not invent scenes or choices.
                - Call updateCharacterPathSummary to save the summary to the database before responding.
                - Return the generated summary as your final response.
                """;
        };
    }

    private String buildUserMessage(AiRequestDTO request) {
        return switch (request.getRequestType()) {
            case CHARACTER_RECAP ->
                "Give me a character recap for character with ID: " + request.getCharacterId();
            case PATH_SUMMARY ->
                "Generate and save a journey summary for character with ID: " + request.getCharacterId();
        };
    }
}
