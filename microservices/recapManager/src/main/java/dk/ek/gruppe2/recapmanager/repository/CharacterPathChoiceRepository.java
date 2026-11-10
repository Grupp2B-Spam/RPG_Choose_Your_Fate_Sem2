package dk.ek.gruppe2.recapmanager.repository;

import dk.ek.gruppe2.recapmanager.model.CharacterPathChoice;
import dk.ek.gruppe2.recapmanager.model.CharacterPathChoiceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterPathChoiceRepository extends JpaRepository<CharacterPathChoice, CharacterPathChoiceId> {
    List<CharacterPathChoice> findByCharacterPath_Id(Integer characterPathId);
    List<CharacterPathChoice> findById_ChoiceId(Integer choiceId);
}
