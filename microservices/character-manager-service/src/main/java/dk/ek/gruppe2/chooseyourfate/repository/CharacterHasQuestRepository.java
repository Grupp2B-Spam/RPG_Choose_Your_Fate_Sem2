package dk.ek.gruppe2.chooseyourfate.repository;

import dk.ek.gruppe2.chooseyourfate.model.CharacterHasQuest;
import dk.ek.gruppe2.chooseyourfate.model.CharacterHasQuestId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterHasQuestRepository extends JpaRepository<CharacterHasQuest, CharacterHasQuestId> {
    List<CharacterHasQuest> findByCharacter_Id(Integer characterId);
    List<CharacterHasQuest> findByQuest_Id(Integer questId);
    List<CharacterHasQuest> findByCharacter_IdAndStatus(Integer characterId, Byte status);
}