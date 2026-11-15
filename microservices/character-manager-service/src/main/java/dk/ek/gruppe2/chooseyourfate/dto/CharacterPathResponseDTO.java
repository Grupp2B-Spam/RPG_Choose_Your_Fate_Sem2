package dk.ek.gruppe2.chooseyourfate.dto;

public record CharacterPathResponseDTO(
        Integer id,
        Integer characterId,
        String summary,
        byte[] audioBlob
) {
    public CharacterPathResponseDTO(
            Integer id,
            Integer characterId,
            String summary
    ) {
        this(id, characterId, summary, null);
    }
}
