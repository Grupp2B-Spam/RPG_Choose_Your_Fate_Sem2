package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterDetails;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CharacterDetailsRepository extends JpaRepository<CharacterDetails, Integer> {
    // Loads character details together with the character data needed for the combined view.
    @EntityGraph(attributePaths = {"character", "character.account", "character.chapter", "character.raceDetails"})
    @Query("select cd from CharacterDetails cd where cd.characterId = :characterId")
    Optional<CharacterDetails> findByIdWithCharacterView(@Param("characterId") Integer characterId);
}
