package dk.ek.gruppe2.chooseyourfate.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import dk.ek.gruppe2.chooseyourfate.TestContainerConfig;
import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterPath;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.CharacterPathRepository;
import dk.ek.gruppe2.chooseyourfate.service.TTSService;


@Testcontainers
@SpringBootTest
@Transactional
class TTSServiceIT {
    
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestContainerConfig.MYSQL::getJdbcUrl);
        registry.add("spring.datasource.password", TestContainerConfig.MYSQL::getPassword);
        registry.add("spring.datasource.username", TestContainerConfig.MYSQL::getUsername);
    }

    @Autowired
    private CharacterPathRepository characterPathRepository;


    @Autowired
    private TTSService ttsService;


    //TTSP2
    @Test
    void textToSpeech_ShouldThrowException_WhenCharacterIdExists_And_BlobAndSummaryDoesNotExist() {
        //Arrange
        Integer characterId = 3;

        //Act + Assert
        assertThatThrownBy(() ->
                ttsService.textToSpeech(characterId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt must contain text to convert to speech.");
    }

    //TTSP2
    @Test
    void textToSpeech_ShouldThrowException_WhenCharacterIdExists_And_BlobDoesNotExist_And_SummaryIsEmpty() {
        //Arrange
        Integer characterId = 2;

        //Act + Assert
        assertThatThrownBy(() ->
                ttsService.textToSpeech(characterId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt must contain text to convert to speech.");
    }

    //TTSP3
    @Test
    void textToSpeech_ShouldThrowException_WhenCharacterIdAndSummaryExists_And_BlobDoesNotExist() {
        //Arrange
        Integer characterId = 1;
        LocalDateTime currentTime = LocalDateTime.now();

        //Act 
        byte[] blob = ttsService.textToSpeech(characterId);
        
        //Assert
        assertNotNull(blob);
        CharacterPath characterPath = characterPathRepository.findByCharacter_Id(characterId);
        assertNotNull(characterPath.getAudioBlob());
        assertEquals(blob, characterPath.getAudioBlob());
        assertTrue(currentTime.isBefore(characterPath.getAudioBlobUpdatedAt()));
    }

    //TTSP4
    @Test
    void textToSpeech_ShouldThrowException_WhenCharacterIdAndBlobExists_And_SummaryDoesNotExist() {
        //Arrange
        Integer characterId = 6;
        LocalDateTime currentTime = LocalDateTime.now();
        //Act 
        byte[] blob = ttsService.textToSpeech(characterId);
        
        //Assert
        assertNotNull(blob);
        CharacterPath characterPath = characterPathRepository.findByCharacter_Id(characterId);
        assertNotNull(characterPath.getAudioBlob());
        assertEquals(blob, characterPath.getAudioBlob());
        assertTrue(currentTime.isAfter(characterPath.getAudioBlobUpdatedAt()));
    }

    //TTSP5
    @Test
    void textToSpeech_ShouldThrowException_WhenCharacterIdAndBlobExists_And_BlobDateAfterSummaryDate() {
        //Arrange
        Integer characterId = 5;
        LocalDateTime currentTime = LocalDateTime.now();
        //Act 
        byte[] blob = ttsService.textToSpeech(characterId);
        
        //Assert
        assertNotNull(blob);
        CharacterPath characterPath = characterPathRepository.findByCharacter_Id(characterId);
        assertNotNull(characterPath.getAudioBlob());
        assertEquals(blob, characterPath.getAudioBlob());
        assertTrue(currentTime.isAfter(characterPath.getAudioBlobUpdatedAt()));
    }

    //TTSP6
    @Test
    void textToSpeech_ShouldThrowException_WhenCharacterIdAndBlobExists_And_BlobDateBeforeSummaryDate() {
        //Arrange
        Integer characterId = 7;
        LocalDateTime currentTime = LocalDateTime.now();
        CharacterPath characterPath = characterPathRepository.findByCharacter_Id(characterId);

        String BeforeCharacterPathDate = characterPath.getAudioBlob().toString();

        //Act 
        byte[] blob = ttsService.textToSpeech(characterId);
        
        //Assert
        assertNotNull(blob);
        assertNotNull(characterPath.getAudioBlob());
        assertNotEquals(blob.toString(), BeforeCharacterPathDate);
        assertEquals(blob.toString(), characterPath.getAudioBlob().toString());
        assertTrue(currentTime.isBefore(characterPath.getAudioBlobUpdatedAt()));
    }
}