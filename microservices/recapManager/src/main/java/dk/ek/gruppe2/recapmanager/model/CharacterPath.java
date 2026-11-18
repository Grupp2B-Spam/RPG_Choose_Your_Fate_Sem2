package dk.ek.gruppe2.recapmanager.model;

import dk.ek.gruppe2.recapmanager.enums.AiRequestType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@Entity
@Table(name = "character_path", uniqueConstraints = @UniqueConstraint(
        name = "uk_character_path_format", columnNames = {"character_id", "request_type"}))
public class CharacterPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "character_id", nullable = false)
    private Integer characterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    @Builder.Default
    private AiRequestType requestType = AiRequestType.PATH_SUMMARY;

    @Column(name = "last_processed_sequence", nullable = false)
    private long lastProcessedSequence;

    @Version
    @Column(nullable = false)
    private Long version;

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

    public AiRequestType getRequestType() { return requestType; }
    public void setRequestType(AiRequestType requestType) { this.requestType = requestType; }
    public long getLastProcessedSequence() { return lastProcessedSequence; }
    public void setLastProcessedSequence(long sequence) { this.lastProcessedSequence = sequence; }
    public Long getVersion() { return version; }

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
