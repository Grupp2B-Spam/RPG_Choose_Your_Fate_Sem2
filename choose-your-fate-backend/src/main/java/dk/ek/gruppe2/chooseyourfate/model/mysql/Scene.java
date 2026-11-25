package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scene")
public class Scene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(length = 100)
    private String name;

    @OneToMany(mappedBy = "scene")
    private List<CharacterAvatar> characters = new ArrayList<>();

    @OneToMany(mappedBy = "scene")
    private List<Quest> quests = new ArrayList<>();

    @OneToMany(mappedBy = "scene")
    private List<Choice> choices = new ArrayList<>();

    public Scene() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Chapter getChapter() { return chapter; }
    public void setChapter(Chapter chapter) { this.chapter = chapter; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<CharacterAvatar> getCharacters() { return characters; }
    public void setCharacters(List<CharacterAvatar> characters) { this.characters = characters; }

    public List<Quest> getQuests() { return quests; }
    public void setQuests(List<Quest> quests) { this.quests = quests; }

    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }
}
