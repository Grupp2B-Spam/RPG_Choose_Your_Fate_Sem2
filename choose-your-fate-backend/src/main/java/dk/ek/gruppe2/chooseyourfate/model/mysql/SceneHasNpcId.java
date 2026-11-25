package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

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
