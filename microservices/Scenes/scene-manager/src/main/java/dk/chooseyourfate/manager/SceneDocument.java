package dk.chooseyourfate.manager;

import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "scenes")
public record SceneDocument(
        @Id String id,
        long version,
        String title,
        String text,
        List<Choice> choices,
        boolean deleted
) {}
