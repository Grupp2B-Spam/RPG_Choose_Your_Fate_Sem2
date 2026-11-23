package dk.ek.gruppe2.chooseyourfate.repository.mysql;

import dk.ek.gruppe2.chooseyourfate.model.mysql.SceneHasNpc;
import dk.ek.gruppe2.chooseyourfate.model.mysql.SceneHasNpcId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * MIGRATED to StoryContentManager: {@code scene_has_npc} is owned by
 * {@code microservices/storycontent-manager}.
 *
 * <p>It is called {@code scene_npc} there. Placing an NPC in a scene changes the scene's
 * published payload, so it is edited through the scene.
 *
 * <p><b>This code is deliberately left in place and still works.</b> The monolith is
 * what runs the game today, and nothing consumes StoryContentManager's events yet, so
 * this content exists in two places for now. Where the two disagree,
 * StoryContentManager holds the truth, and new content belongs there —
 * {@code PUT /admin/v1/scenes/{id}/npcs} — rather than here.
 *
 * <p>The whole list of what moved, and how the new schema differs from this one, is in
 * the header of {@code microservices/storycontent-manager/db-init/01_schema.sql}.
 */
public interface SceneHasNpcRepository extends JpaRepository<SceneHasNpc, SceneHasNpcId> {
    List<SceneHasNpc> findBySceneId(Integer sceneId);
    List<SceneHasNpc> findByNpcId(Integer npcId);
}