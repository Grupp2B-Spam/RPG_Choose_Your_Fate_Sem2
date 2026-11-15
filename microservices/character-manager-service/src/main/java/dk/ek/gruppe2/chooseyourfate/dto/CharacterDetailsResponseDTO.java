package dk.ek.gruppe2.chooseyourfate.dto;

public record CharacterDetailsResponseDTO(
        Integer characterId,
        Integer intelligence,
        Integer charisma,
        Integer fashion
) {
}
