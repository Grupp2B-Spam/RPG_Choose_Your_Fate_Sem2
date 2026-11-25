package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterAvatar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterAvatarRepository extends JpaRepository<CharacterAvatar, Integer> {
    List<CharacterAvatar> findByAccount_Id(Integer accountId);

    List<CharacterAvatar> findByChapter_Id(Integer chapterId);

    List<CharacterAvatar> findByScene_Id(Integer sceneId);

    boolean existsByRaceDetails_Id(Integer raceDetailsId);

    // Counts how many characters an account already has.
    long countByAccount_Id(Integer accountId);
}
