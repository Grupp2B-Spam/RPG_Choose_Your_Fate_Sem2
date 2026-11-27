package dk.ek.gruppe2.recapmanager.config;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.context.annotation.*;
import tools.jackson.databind.json.JsonMapper;

/** Fixture model only: keep real AiService/tools/persistence while avoiding an external LLM locally. */
@Configuration
@Profile("local & local-integration")
public class LocalIntegrationAiConfiguration {
    @Bean @Primary ChatClient.Builder localIntegrationChatClient() {
        ChatModel fixture = prompt -> {
            var match = Pattern.compile("character with ID: (\\d+)").matcher(prompt.getUserMessage().getText());
            if (!match.find()) throw new IllegalArgumentException("Local fixture needs the requested character ID");
            int id = Integer.parseInt(match.group(1));
            var options = (ToolCallingChatOptions) prompt.getOptions();
            var context = new ToolContext(options.getToolContext());
            var json = JsonMapper.builder().build();
            String input = json.writeValueAsString(Map.of("characterId", id));
            var tools = options.getToolCallbacks();
            tools.stream().filter(tool -> tool.getToolDefinition().name().equals("getCharacterInfo"))
                    .findFirst().orElseThrow().call(input, context);
            String history = tools.stream().filter(tool -> tool.getToolDefinition().name().equals("getCharacterPathHistory"))
                    .findFirst().orElseThrow().call(input, context);
            String summary = "[Local integration fixture] " + history;
            tools.stream().filter(tool -> tool.getToolDefinition().name().equals("updateCharacterPathSummary"))
                    .findFirst().orElseThrow().call(json.writeValueAsString(Map.of("characterId", id, "summary", summary)), context);
            return new ChatResponse(List.of(new Generation(new AssistantMessage(summary))));
        };
        return ChatClient.builder(fixture);
    }
}
