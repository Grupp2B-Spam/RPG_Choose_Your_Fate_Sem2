package dk.ek.gruppe2.recapmanager.messaging;

import java.time.Instant;
import dk.ek.gruppe2.recapmanager.enums.AiRequestType;

/** Versioned JSON contract for the TTS team; contains no user token. */
public record RecapGenerated(String eventId, String eventType, int eventVersion, Instant occurredAt,
                             int characterId, AiRequestType requestType, long throughSequence,
                             Long summaryVersion, String contentHash, String text, String voiceId,
                             String model, String outputFormat) {}
