package dk.ek.gruppe2.chooseyourfate.interfaces;

import dk.ek.gruppe2.chooseyourfate.dto.CharacterResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.CreateCharacterRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.MultipleCharacterViewsResponseDto;

import java.util.List;

public interface CharacterDataAccess<T> {

    List<CharacterResponseDTO> getAllCharacters();

    CharacterResponseDTO getCharacterById(T id);

    CharacterResponseDTO createCharacter(T accountId, CreateCharacterRequestDTO request);

    void deleteCharacter(T id);

    List<CharacterResponseDTO> getCharactersByAccountId(T id);

    MultipleCharacterViewsResponseDto getCharactersViewBy_AccountId(T id);
}
