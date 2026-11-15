package dk.ek.gruppe2.chooseyourfate.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "character_avatar")
public class CharacterAvatar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column
    private Integer accountId;

    @Column
    private Integer chapterId;

    @Column
    private Integer sceneId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "race_detail_id", nullable = false)
    private RaceDetails raceDetails;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, columnDefinition = "json")
    private String flag;

    public CharacterAvatar() {}

    public Integer getAccount() { return accountId; }
    public void setAccount(Integer accountId) { this.accountId = accountId; }

    public Integer getChapter() { return chapterId; }
    public void setChapter(Integer chapterId) { this.chapterId = chapterId; }

    public Integer getScene() { return sceneId; }
    public void setScene(Integer sceneId) { this.sceneId = sceneId; }

    public RaceDetails getRaceDetails() { return raceDetails; }
    public void setRaceDetails(RaceDetails raceDetails) { this.raceDetails = raceDetails; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }
}
