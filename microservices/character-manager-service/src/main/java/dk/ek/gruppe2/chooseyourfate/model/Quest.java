package dk.ek.gruppe2.chooseyourfate.model;

import jakarta.persistence.*;

@Entity
@Table(name = "quest")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "scene_id", nullable = false)
    private Integer sceneId;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    public Quest() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getSceneId() { return sceneId; }
    public void setSceneId(Integer sceneId) { this.sceneId = sceneId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
