package dk.ek.gruppe2.chooseyourfate.service;

import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterPath;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.CharacterPathRepository;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.elevenlabs.ElevenLabsTextToSpeechModel;
import org.springframework.ai.elevenlabs.ElevenLabsTextToSpeechOptions;
import org.springframework.ai.elevenlabs.api.ElevenLabsApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TTSService {

    @Value("${elevenlabs.api.key}")
    private String apiKey;

    private final CharacterPathRepository characterPathRepository;

    public TTSService(CharacterPathRepository characterPathRepository) {
        this.characterPathRepository = characterPathRepository;
    }

    public byte[] textToSpeech(Integer characterId) {
        CharacterPath characterPath = characterPathRepository.findByCharacter_Id(characterId);
        if (characterPath == null) {
            throw new ResourceNotFoundException("Character path not found for character id: " + characterId);
        }
        if (characterPath.getAudioBlob() != null && isAudioUpdatedAfterSummary(characterPath.getSummaryUpdatedAt(), characterPath.getAudioBlobUpdatedAt())) {
            return characterPath.getAudioBlob();
        }
        else {
            byte[] audioBlob = createAudioBlob(characterPath.getSummary());
            characterPath.setAudioBlob(audioBlob);
            characterPath.setAudioBlobUpdatedAt(LocalDateTime.now());
            characterPathRepository.save(characterPath);
            return audioBlob;
        }
    }

    private byte[] createAudioBlob(String text) {
        ElevenLabsApi elevenLabsApi = ElevenLabsApi.builder()
                .apiKey(apiKey)
                .build();

        ElevenLabsTextToSpeechModel elevenLabsTextToSpeechModel = ElevenLabsTextToSpeechModel.builder()
                .elevenLabsApi(elevenLabsApi)
                .defaultOptions(ElevenLabsTextToSpeechOptions.builder()
                        .model("eleven_turbo_v2_5")
                        .voiceId("JBFqnCBsd6RMkjVDRZzb") // e.g. "9BWtsMINqrJLrRacOk9x"
                        .outputFormat("mp3_44100_128")
                        .build())
                .build();

        // The call will use the default options configured above.
        TextToSpeechPrompt speechPrompt = new TextToSpeechPrompt(text);
        TextToSpeechResponse response = elevenLabsTextToSpeechModel.call(speechPrompt);

        return response.getResult().getOutput();
    }

    private boolean isAudioUpdatedAfterSummary(LocalDateTime summaryDate, LocalDateTime audioBlobDate) {
        if (summaryDate == null) return true;
        return audioBlobDate.isAfter(summaryDate);
    }
}