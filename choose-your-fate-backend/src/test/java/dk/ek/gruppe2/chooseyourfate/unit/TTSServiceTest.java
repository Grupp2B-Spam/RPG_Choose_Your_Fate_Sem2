package dk.ek.gruppe2.chooseyourfate.unit;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterPath;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.CharacterPathRepository;
import dk.ek.gruppe2.chooseyourfate.service.TTSService;

@ExtendWith(MockitoExtension.class)
class TTSServiceTest {

    //The way the code is structured makes it impossible to mock the apikey så this is the best way to structure it.
    //This service could probably have benefitted from test driven development.
    @Value("${elevenlabs.api.key}")
    private String apiKey;

    @Mock
    private CharacterPathRepository characterPathRepository;

    @InjectMocks
    private TTSService ttsService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(
                ttsService,
                "apiKey",
                apiKey
        );
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
    //GetAllCharacters method
    //-------------------------------------------------------------------------------------------------------------------------------------------
    
    //TTSP1
    @Test
    void textToSpeech_ShouldThrowException_WhenCharacterIdIsNull() {
        //Arrange
        Integer characterId = null;
        when(characterPathRepository.findByCharacter_Id(characterId)).thenReturn(null);

        //Act + Assert
        assertThatThrownBy(() ->
                ttsService.textToSpeech(characterId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Character path not found for character id: " + characterId);

    }

    //TTSP1
    @Test
    void textToSpeech_ShouldThrowException_WhenCharacterIdDoesNotExist() {
        //Arrange
        Integer characterId = 9999;
        when(characterPathRepository.findByCharacter_Id(characterId)).thenReturn(null);

        //Act + Assert
        assertThatThrownBy(() ->
                ttsService.textToSpeech(characterId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Character path not found for character id: " + characterId);

    }
}