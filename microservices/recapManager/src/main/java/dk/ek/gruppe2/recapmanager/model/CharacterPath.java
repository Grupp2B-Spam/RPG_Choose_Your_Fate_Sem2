package dk.ek.gruppe2.recapmanager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@Entity
@Table(name = "character_path")
public class CharacterPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "character_id", nullable = false)
    private Integer characterId;

    @Column(columnDefinition = "LONGTEXT")
    private String summary;

    @Column(columnDefinition = "MEDIUMBLOB")
    @Lob
    private byte[] audioBlob;

    @Column(columnDefinition = "DATETIME", name = "summary_updated_at")
    private LocalDateTime summaryUpdatedAt;


    @Column(columnDefinition = "DATETIME", name = "audio_blob_updated_at")
    private LocalDateTime audioBlobUpdatedAt;

    public CharacterPath() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCharacterId() { return characterId; }
    public void setCharacterId(Integer characterId) { this.characterId = characterId; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public byte[] getAudioBlob() { return audioBlob; }
    public void setAudioBlob(byte[] audioBlob) { this.audioBlob = audioBlob; }

    public LocalDateTime getAudioBlobUpdatedAt() {
        return audioBlobUpdatedAt;
    }

    public LocalDateTime getSummaryUpdatedAt() {
        return summaryUpdatedAt;
    }

    public void setSummaryUpdatedAt(LocalDateTime summaryUpdatedAt) {
        this.summaryUpdatedAt = summaryUpdatedAt;
    }

    public void setAudioBlobUpdatedAt(LocalDateTime audioBlobUpdatedAt) {
        this.audioBlobUpdatedAt = audioBlobUpdatedAt;
    }

}
