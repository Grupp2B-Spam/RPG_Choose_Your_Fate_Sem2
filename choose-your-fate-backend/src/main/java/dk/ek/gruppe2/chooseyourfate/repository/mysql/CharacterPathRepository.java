package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterPath;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterPathRepository extends JpaRepository<CharacterPath, Integer> {
    CharacterPath findByCharacter_Id(Integer characterId);
}
