package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

@Entity
@Table(name = "quest")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "scene_id", nullable = false)
    private Scene scene;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;

    public Quest() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Scene getScene() { return scene; }
    public void setScene(Scene scene) { this.scene = scene; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
