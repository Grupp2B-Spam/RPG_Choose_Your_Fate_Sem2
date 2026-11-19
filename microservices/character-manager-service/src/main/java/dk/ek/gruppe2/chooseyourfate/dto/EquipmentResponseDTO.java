package dk.ek.gruppe2.chooseyourfate.dto;

public record EquipmentResponseDTO(
        Integer characterId,
        ItemResponseDTO headItem,
        ItemResponseDTO chestItem,
        ItemResponseDTO legsItem
) {
}
