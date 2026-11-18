package dk.ek.gruppe2.recapmanager.model;

import dk.ek.gruppe2.recapmanager.enums.AiRequestType;

public record SummarySnapshot(Integer id, int characterId, AiRequestType requestType,
                              String summary, long lastProcessedSequence, Long version) {
    public static SummarySnapshot empty(int characterId, AiRequestType type) {
        return new SummarySnapshot(null, characterId, type, null, 0, null);
    }
}
