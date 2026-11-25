package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.SceneHasNpc;
import dk.ek.gruppe2.chooseyourfate.model.mysql.SceneHasNpcId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MOVED: {@code scene_has_npc} now belongs to StoryContentManager, in {@code
 * microservices/storycontent-manager}.
 *
 * <p>Called {@code scene_npc} there, and edited through the scene because it changes what
 * the scene publishes.
 *
 * <p>This code is left here deliberately and still works: the monolith runs the game, and
 * nothing consumes the new events yet. Where the two disagree StoryContentManager is the
 * one that is right, and new content belongs there — {@code PUT
 * /admin/v1/scenes/{id}/npcs}.
 */
public interface SceneHasNpcRepository extends JpaRepository<SceneHasNpc, SceneHasNpcId> {
    List<SceneHasNpc> findBySceneId(Integer sceneId);
    List<SceneHasNpc> findByNpcId(Integer npcId);
}