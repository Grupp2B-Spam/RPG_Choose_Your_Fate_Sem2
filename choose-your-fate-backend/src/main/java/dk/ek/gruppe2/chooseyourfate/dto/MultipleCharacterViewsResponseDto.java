package dk.ek.gruppe2.chooseyourfate.dto;

import java.util.List;

public class MultipleCharacterViewsResponseDto {
    List<CharacterViewResponseDTO> views;
    Boolean canCreateMoreCharacters;
    public MultipleCharacterViewsResponseDto(List<CharacterViewResponseDTO> views, Boolean canCreateMoreCharacters) {
        this.views = views;
        this.canCreateMoreCharacters = canCreateMoreCharacters;
    }
    public List<CharacterViewResponseDTO> getViews() {
        return views;
    }
    public void setViews(List<CharacterViewResponseDTO> views) {
        this.views = views;
    }
    public Boolean getCanCreateMoreCharacters() {
        return canCreateMoreCharacters;
    }
    public void setCanCreateMoreCharacters(Boolean canCreateMoreCharacters) {
        this.canCreateMoreCharacters = canCreateMoreCharacters;
    }
}