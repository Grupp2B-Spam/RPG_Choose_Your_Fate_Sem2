package dk.ek.gruppe2.chooseyourfate.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ItemTypeConverter implements AttributeConverter<ItemType, String> {

    @Override
    public String convertToDatabaseColumn(ItemType type) {
        if (type == null) return null;
        return type.name();
    }

    @Override
    public ItemType convertToEntityAttribute(String value) {
        if (value == null) return null;
        for (ItemType type : ItemType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown item type: " + value);
    }
}