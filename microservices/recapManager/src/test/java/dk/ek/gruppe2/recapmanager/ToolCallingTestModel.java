package dk.ek.gruppe2.recapmanager;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import tools.jackson.databind.json.JsonMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** In-process model substitute that executes the actual callbacks registered with ChatClient.tools(). */
class ToolCallingTestModel implements ChatModel {
    int calls;
    Function<Tools, String> behavior = tools -> "Final response";
    final List<String> histories = new ArrayList<>();

    @Override
    public ChatResponse call(Prompt prompt) {
        calls++;
        var options = (ToolCallingChatOptions) prompt.getOptions();
        var tools = new Tools(options.getToolCallbacks(), new ToolContext(options.getToolContext()));
        return new ChatResponse(List.of(new Generation(new AssistantMessage(behavior.apply(tools)))));
    }

    final class Tools {
        final Map<String, ToolCallback> callbacks;
        final ToolContext context;
        Tools(List<ToolCallback> callbacks, ToolContext context) {
            this.callbacks = callbacks.stream().collect(Collectors.toMap(callback -> callback.getToolDefinition().name(),
                    Function.identity()));
            this.context = context;
        }
        String history() {
            String result = callbacks.get("getCharacterPathHistory").call("{\"characterId\":42}", context);
            histories.add(result);
            return result;
        }
        void update(String summary) {
            String arguments = JsonMapper.builder().build().writeValueAsString(Map.of("characterId", 42, "summary", summary));
            callbacks.get("updateCharacterPathSummary").call(arguments, context);
        }
        String generate(String summary) {
            history();
            update(summary);
            return "Final response (the staged narrative is the value to persist)";
        }
    }
}
