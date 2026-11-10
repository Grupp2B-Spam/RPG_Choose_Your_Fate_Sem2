package dk.ek.gruppe2.recapmanager.repository;

import dk.ek.gruppe2.recapmanager.model.CharacterPath;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterPathRepository extends JpaRepository<CharacterPath, Integer> {
    CharacterPath findByCharacterId(Integer characterId);
}
