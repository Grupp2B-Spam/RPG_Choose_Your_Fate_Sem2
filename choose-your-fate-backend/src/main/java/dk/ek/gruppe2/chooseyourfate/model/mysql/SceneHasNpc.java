package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

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
