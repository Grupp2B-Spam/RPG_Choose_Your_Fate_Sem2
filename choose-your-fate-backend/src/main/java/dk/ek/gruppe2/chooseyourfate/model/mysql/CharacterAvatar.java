package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "character_avatar")
public class CharacterAvatar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @ManyToOne(optional = false)
    @JoinColumn(name = "scene_id", nullable = false)
    private Scene scene;

    @ManyToOne(optional = false)
    @JoinColumn(name = "race_detail_id", nullable = false)
    private RaceDetails raceDetails;

    @OneToMany(mappedBy = "character", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CharacterHasQuest> characterQuests = new ArrayList<>();

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, columnDefinition = "json")
    private String flag;

    public CharacterAvatar() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public void addQuest(Quest quest, Byte status) {
        CharacterHasQuest relation = new CharacterHasQuest(this, quest, status);
        characterQuests.add(relation);
    }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public Chapter getChapter() { return chapter; }
    public void setChapter(Chapter chapter) { this.chapter = chapter; }

    public Scene getScene() { return scene; }
    public void setScene(Scene scene) { this.scene = scene; }

    public RaceDetails getRaceDetails() { return raceDetails; }
    public void setRaceDetails(RaceDetails raceDetails) { this.raceDetails = raceDetails; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }
}
