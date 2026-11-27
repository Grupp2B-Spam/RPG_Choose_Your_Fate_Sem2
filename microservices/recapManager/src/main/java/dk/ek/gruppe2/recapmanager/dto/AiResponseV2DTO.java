package dk.ek.gruppe2.recapmanager.dto;

public record AiResponseV2DTO(String response, AudioStatus audioStatus, String audioUrl, String contentHash) {
    public enum AudioStatus { NOT_REQUESTED, NO_TEXT, READY, PENDING, FAILED, UNAVAILABLE }
}
