package dk.ek.gruppe2.chooseyourfate.model.mysql;

import jakarta.persistence.*;

@Entity
@Table(name = "character_has_quest")
public class CharacterHasQuest {

    @EmbeddedId
    private CharacterHasQuestId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("characterId")
    @JoinColumn(name = "character_id")
    private CharacterAvatar character;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questId")
    @JoinColumn(name = "quest_id")
    private Quest quest;

    @Column(nullable = false)
    private Byte status;

    public CharacterHasQuest() {}

    public CharacterHasQuest(CharacterAvatar character, Quest quest, Byte status) {
        this.character = character;
        this.quest = quest;
        this.status = status;
        this.id = new CharacterHasQuestId(character.getId(), quest.getId());
    }

    public CharacterHasQuestId getId() { return id; }
    public void setId(CharacterHasQuestId id) { this.id = id; }

    public CharacterAvatar getCharacter() { return character; }
    public void setCharacter(CharacterAvatar character) { this.character = character; }

    public Quest getQuest() { return quest; }
    public void setQuest(Quest quest) { this.quest = quest; }

    public Byte getStatus() { return status; }
    public void setStatus(Byte status) { this.status = status; }
}