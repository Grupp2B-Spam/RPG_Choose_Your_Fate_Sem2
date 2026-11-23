package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

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
@Embeddable
public class SceneHasNpcId implements Serializable {

    private Integer sceneId;
    private Integer npcId;

    public SceneHasNpcId() {}

    public SceneHasNpcId(Integer sceneId, Integer npcId) {
        this.sceneId = sceneId;
        this.npcId = npcId;
    }

    public Integer getSceneId() { return sceneId; }
    public void setSceneId(Integer sceneId) { this.sceneId = sceneId; }

    public Integer getNpcId() { return npcId; }
    public void setNpcId(Integer npcId) { this.npcId = npcId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SceneHasNpcId that)) return false;
        return Objects.equals(sceneId, that.sceneId) &&
                Objects.equals(npcId, that.npcId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sceneId, npcId);
    }
}
