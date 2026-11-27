package dk.ek.gruppe2.chooseyourfate.repository;

import dk.ek.gruppe2.chooseyourfate.model.CharacterAvatar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterAvatarRepository extends JpaRepository<CharacterAvatar, Integer> {
    List<CharacterAvatar> findByAccountId(Integer accountId);

    boolean existsByRaceDetails_Id(Integer raceDetailsId);

    // Counts how many characters an account already has.
    long countByAccountId(Integer accountId);
}
