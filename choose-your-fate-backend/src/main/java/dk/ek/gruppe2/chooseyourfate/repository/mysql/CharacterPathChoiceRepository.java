package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterPathChoice;
import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterPathChoiceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterPathChoiceRepository extends JpaRepository<CharacterPathChoice, CharacterPathChoiceId> {
    List<CharacterPathChoice> findByCharacterPath_Id(Integer characterPathId);
    List<CharacterPathChoice> findByChoice_Id(Integer choiceId);
}