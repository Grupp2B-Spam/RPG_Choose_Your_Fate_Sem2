package dk.chooseyourfate.indexer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.client.result.UpdateResult;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class SceneIndexListenerTest {
    @Test
    void upsertUsesVersionGuardedMongoUpdateWithCurrentDocumentFields() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(SceneDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        SceneIndexListener listener = new SceneIndexListener(mongo, JsonMapper.builder().build());
        String event = """
                {"sceneId":"start","version":2,"operation":"UPSERT",
                 "title":"Start","text":"A fork","choices":[]}
                """;

        listener.onSceneChanged(new Message(event.getBytes(StandardCharsets.UTF_8), new MessageProperties()));

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateFirst(any(Query.class), updateCaptor.capture(), eq(SceneDocument.class));
        Map<?, ?> fields = (Map<?, ?>) updateCaptor.getValue().getUpdateObject().get("$set");
        Assertions.assertEquals("start", fields.get("sceneId"));
        Assertions.assertEquals(2L, fields.get("version"));
        Assertions.assertEquals("PUBLISHED", fields.get("status"));
        Assertions.assertEquals("Start", fields.get("name"));
        Assertions.assertEquals("A fork", fields.get("description"));
        Assertions.assertFalse(fields.containsKey("title"));
        Assertions.assertFalse(fields.containsKey("text"));
    }

    @Test
    void newSceneIsInsertedUsingCurrentDocumentShape() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(SceneDocument.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, null));
        SceneIndexListener listener = new SceneIndexListener(mongo, JsonMapper.builder().build());
        String event = """
                {"sceneId":"start","version":2,"operation":"UPSERT",
                 "chapterId":"chapter-1","status":"PUBLISHED",
                 "name":"Start","description":"A fork","choices":[],
                 "npcIds":["npc-1"],"questIds":["quest-1"]}
                """;

        listener.onSceneChanged(new Message(event.getBytes(StandardCharsets.UTF_8), new MessageProperties()));

        ArgumentCaptor<SceneDocument> documentCaptor = ArgumentCaptor.forClass(SceneDocument.class);
        verify(mongo).insert(documentCaptor.capture());
        SceneDocument document = documentCaptor.getValue();
        Assertions.assertEquals("start", document.id());
        Assertions.assertEquals("start", document.sceneId());
        Assertions.assertEquals(2L, document.version());
        Assertions.assertEquals("chapter-1", document.chapterId());
        Assertions.assertEquals("PUBLISHED", document.status());
        Assertions.assertEquals("Start", document.name());
        Assertions.assertEquals("A fork", document.description());
        Assertions.assertEquals(java.util.List.of("npc-1"), document.npcIds());
        Assertions.assertEquals(java.util.List.of("quest-1"), document.questIds());
    }
}
