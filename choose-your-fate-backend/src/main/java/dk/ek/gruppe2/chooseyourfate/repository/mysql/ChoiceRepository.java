package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.Choice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChoiceRepository extends JpaRepository<Choice, Integer> {
    List<Choice> findByScene_Id(Integer sceneId);
    List<Choice> findByDestinationScene_Id(Integer destinationSceneId);
}
