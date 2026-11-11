package dk.chooseyourfate.indexer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.client.result.UpdateResult;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import tools.jackson.databind.json.JsonMapper;

class SceneIndexListenerTest {
    @Test
    void upsertUsesVersionGuardedMongoUpdate() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(SceneDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        SceneIndexListener listener = new SceneIndexListener(mongo, JsonMapper.builder().build());
        String event = """
                {"sceneId":"start","version":2,"operation":"UPSERT",
                 "title":"Start","text":"A fork","choices":[]}
                """;

        listener.onSceneChanged(new Message(event.getBytes(StandardCharsets.UTF_8), new MessageProperties()));

        verify(mongo).updateFirst(any(Query.class), any(Update.class), eq(SceneDocument.class));
    }
}
