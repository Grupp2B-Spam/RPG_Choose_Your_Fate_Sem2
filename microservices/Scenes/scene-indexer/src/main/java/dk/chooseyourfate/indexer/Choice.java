package dk.chooseyourfate.indexer;

import java.util.Map;

public record Choice(String id, String text, String nextSceneId, Map<String, Object> effects) {}
