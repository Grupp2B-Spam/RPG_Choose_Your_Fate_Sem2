package dk.chooseyourfate.indexer;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "scenes")
public record SceneDocument(
        @Id String id,
        String sceneId,
        long version,
        String chapterId,
        String status,
        String name,
        String description,
        List<Choice> choices,
        List<String> npcIds,
        List<String> questIds,
        boolean deleted
) {}
