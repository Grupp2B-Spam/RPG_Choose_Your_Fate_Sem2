package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

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
@Entity
@Table(name = "scene_has_npc")
public class SceneHasNpc {

    @EmbeddedId
    private SceneHasNpcId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sceneId")
    @JoinColumn(name = "scene_id")
    private Scene scene;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("npcId")
    @JoinColumn(name = "npc_id")
    private Npc npc;

    public SceneHasNpc() {}

    public SceneHasNpc(Scene scene, Npc npc) {
        this.scene = scene;
        this.npc = npc;
        this.id = new SceneHasNpcId(scene.getId(), npc.getId());
    }

    public SceneHasNpcId getId() { return id; }
    public void setId(SceneHasNpcId id) { this.id = id; }

    public Scene getScene() { return scene; }
    public void setScene(Scene scene) { this.scene = scene; }

    public Npc getNpc() { return npc; }
    public void setNpc(Npc npc) { this.npc = npc; }
}
