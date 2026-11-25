package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.SceneHasNpc;
import dk.ek.gruppe2.chooseyourfate.model.mysql.SceneHasNpcId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SceneHasNpcRepository extends JpaRepository<SceneHasNpc, SceneHasNpcId> {
    List<SceneHasNpc> findBySceneId(Integer sceneId);
    List<SceneHasNpc> findByNpcId(Integer npcId);
}