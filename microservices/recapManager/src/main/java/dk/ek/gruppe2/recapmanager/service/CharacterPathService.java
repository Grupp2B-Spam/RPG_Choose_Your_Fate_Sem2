package dk.ek.gruppe2.recapmanager.service;

import dk.ek.gruppe2.recapmanager.enums.AiRequestType;
import dk.ek.gruppe2.recapmanager.dto.CharacterPathResponseDTO;
import dk.ek.gruppe2.recapmanager.dto.UpdateCharacterPathRequestDTO;
import dk.ek.gruppe2.recapmanager.exception.ResourceNotFoundException;
import dk.ek.gruppe2.recapmanager.exception.RecapConflictException;
import dk.ek.gruppe2.recapmanager.model.CharacterPath;
import dk.ek.gruppe2.recapmanager.model.SummarySnapshot;
import dk.ek.gruppe2.recapmanager.repository.CharacterPathRepository;
import dk.ek.gruppe2.recapmanager.messaging.RecapEventOutbox;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.List;

@Service
public class CharacterPathService {
    private final CharacterPathRepository repository;
    private final RecapEventOutbox events;

    public CharacterPathService(CharacterPathRepository repository, RecapEventOutbox events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<CharacterPathResponseDTO> getAllCharacterPaths() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CharacterPathResponseDTO getCharacterPathByCharacterId(Integer characterId, AiRequestType format) {
        CharacterPath path = repository.findByCharacterIdAndRequestType(characterId, format);
        if (path == null) throw new ResourceNotFoundException("Character path not found for character id: " + characterId);
        return toDto(path);
    }

    @Transactional
    public CharacterPathResponseDTO updateCharacterPath(SummarySnapshot expected, UpdateCharacterPathRequestDTO request,
                                                       long throughSequence) {
        var saved = saveUpdatedSummary(expected, request.getSummary(), throughSequence);
        return new CharacterPathResponseDTO(saved.id(), saved.characterId(), saved.summary());
    }

    @Transactional(readOnly = true)
    public SummarySnapshot load(int characterId, AiRequestType format) {
        CharacterPath path = repository.findByCharacterIdAndRequestType(characterId, format);
        return path == null ? SummarySnapshot.empty(characterId, format) : snapshot(path);
    }

    /** A short transaction after all remote calls: summary and checkpoint commit together. */
    @Transactional
    public SummarySnapshot saveUpdatedSummary(SummarySnapshot expected, String summary, long throughSequence) {
        if (summary == null || summary.isBlank() || throughSequence <= expected.lastProcessedSequence()) {
            throw new IllegalArgumentException("An update requires a nonempty summary and a newer checkpoint");
        }
        CharacterPath path = repository.findByCharacterIdAndRequestType(expected.characterId(), expected.requestType());
        if (expected.id() == null) {
            if (path != null) throw new RecapConflictException();
            path = new CharacterPath();
            path.setCharacterId(expected.characterId());
            path.setRequestType(expected.requestType());
        } else if (path == null || !Objects.equals(path.getId(), expected.id())
                || !Objects.equals(path.getVersion(), expected.version())
                || path.getLastProcessedSequence() != expected.lastProcessedSequence()) {
            throw new RecapConflictException();
        }
        path.setSummary(summary);
        path.setSummaryUpdatedAt(LocalDateTime.now());
        path.setLastProcessedSequence(throughSequence);
        // Audio for the old text must not be served as if it matches the new narrative.
        path.setAudioBlob(null);
        path.setAudioBlobUpdatedAt(null);
        var saved = snapshot(repository.saveAndFlush(path));
        events.enqueue(saved);
        return saved;
    }

    private SummarySnapshot snapshot(CharacterPath path) {
        return new SummarySnapshot(path.getId(), path.getCharacterId(), path.getRequestType(), path.getSummary(),
                path.getLastProcessedSequence(), path.getVersion());
    }

    private CharacterPathResponseDTO toDto(CharacterPath path) {
        return new CharacterPathResponseDTO(path.getId(), path.getCharacterId(), path.getSummary());
    }
}
