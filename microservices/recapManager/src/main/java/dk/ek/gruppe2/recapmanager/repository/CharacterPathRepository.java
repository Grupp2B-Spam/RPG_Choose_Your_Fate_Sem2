package dk.ek.gruppe2.recapmanager.repository;

import dk.ek.gruppe2.recapmanager.model.CharacterPath;
import dk.ek.gruppe2.recapmanager.enums.AiRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterPathRepository extends JpaRepository<CharacterPath, Integer> {
    CharacterPath findByCharacterIdAndRequestType(Integer characterId, AiRequestType requestType);
}
