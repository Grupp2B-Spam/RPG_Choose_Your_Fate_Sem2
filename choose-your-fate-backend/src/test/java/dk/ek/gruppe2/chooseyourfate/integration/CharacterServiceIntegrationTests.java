package dk.ek.gruppe2.chooseyourfate.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import dk.ek.gruppe2.chooseyourfate.ChooseYourFateBackendApplication;
import dk.ek.gruppe2.chooseyourfate.TestContainerConfig;
import dk.ek.gruppe2.chooseyourfate.dto.CharacterResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.CreateCharacterRequestDTO;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.service.CharacterService;
import jakarta.transaction.Transactional;

@Testcontainers
@SpringBootTest
@Transactional
class CharacterServiceIntegrationTests {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestContainerConfig.MYSQL::getJdbcUrl);
        registry.add("spring.datasource.password", TestContainerConfig.MYSQL::getPassword);
        registry.add("spring.datasource.username", TestContainerConfig.MYSQL::getUsername);
    }

    @Autowired
    private CharacterService characterService;

    //-------------------------------------------------------------------------------------------------------------------------------------------
    //GetAllCharacters method
    //-------------------------------------------------------------------------------------------------------------------------------------------
    @Test
    void getAllCharacters_shouldReturnNonNullData() {
        //Arrange
        //Act
        List<CharacterResponseDTO> characters = characterService.getAllCharacters();

        //Act
        assertNotNull(characters);
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
    //getCharacterById method
    //-------------------------------------------------------------------------------------------------------------------------------------------
    @Test
    void getCharacterById_shouldReturnNonNullCharacterResponseDTO() {
        //Arrange
        Integer queryId = 1;

        //Act
        CharacterResponseDTO character = characterService.getCharacterById(queryId);

        //Act
        assertNotNull(character);
        assertEquals(CharacterResponseDTO.class, character.getClass());
        assertEquals(character.getId(), queryId);
    }

    @Test
    void getCharacterById_OnParameterNull_shouldThrowInvalidDataAccessApiUsageException() {
        //Arrange
        Integer queryId = null;

        //Act + Assert
        assertThatThrownBy(() ->
                characterService.getCharacterById(queryId))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("The given id must not be null");
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
    //getCharactersViewBy_AccountId method
    //-------------------------------------------------------------------------------------------------------------------------------------------
    @Test
    void getCharactersViewBy_AccountIdIsNull_ShouldThrowException() {
        //Arrange
        Integer queryId = null;


        //Act + Assert
        assertThatThrownBy(() ->
                characterService.getCharactersViewBy_AccountId(queryId))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("The given id must not be null");
    }

    @Test
    void getCharactersViewBy_AccountIdDoesNotExist_ShouldReturnEmptyList() {
        //Arrange
        Integer queryId = 99999;

        //Act + Assert
        assertThatThrownBy(() ->
                characterService.getCharactersViewBy_AccountId(queryId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("account not found with id: " + queryId);
    }


    //-------------------------------------------------------------------------------------------------------------------------------------------
    //createCharacter method
    //-------------------------------------------------------------------------------------------------------------------------------------------

    //decision table parameters. (look in black box design pdf for full view).
    //Some are handled as integration tests and some as unit tests, and lastly some are handled in both. 
    static Stream<CreateCharacterRequestDTO> CreateRequestParameters() {
        return Stream.of(
            // P1
            new CreateCharacterRequestDTO( 1, 1, 1, "Bobby"),
            // P3
            new CreateCharacterRequestDTO( null, null, 1, "Bobby"),
            // P7
            new CreateCharacterRequestDTO( 1, null, 1, "Bobby"),
            // P8
            new CreateCharacterRequestDTO( null, 2, 1, "Bobby")
        );
    }

    @ParameterizedTest
    @MethodSource("CreateRequestParameters")
    void createCharacter_ShouldReturnExpectedOutcome(CreateCharacterRequestDTO request
    ) {
        // Arrange
        CreateCharacterRequestDTO verificationrequest = new CreateCharacterRequestDTO(
            request.getChapterId(), 
            request.getSceneId(), 
            request.getRaceDetailsId(), 
            request.getName());
            //Used to verify the original data

        // Act
        CharacterResponseDTO characterResponse = characterService.createCharacter(4, request);

        // Assert
        assertNotNull(characterResponse);
        assertEquals(characterResponse.getAccountId(), 4);
        if (verificationrequest.getChapterId() != null && verificationrequest.getSceneId() != null) {
            assertEquals(characterResponse.getChapterId(), verificationrequest.getChapterId());
            assertEquals(characterResponse.getSceneId(), verificationrequest.getSceneId());

        }
        else{
            assertNotNull(characterResponse.getChapterId());
            assertNotNull(characterResponse.getSceneId());
            assertNotEquals(characterResponse.getChapterId(), verificationrequest.getChapterId());
            assertNotEquals(characterResponse.getSceneId(), verificationrequest.getSceneId());
        }

        if (verificationrequest.getRaceDetailsId() != null) {
            assertEquals(characterResponse.getRaceDetailsId(), verificationrequest.getRaceDetailsId());
        } else{
            assertNotNull(characterResponse.getRaceDetailsId());
        }

        assertEquals(characterResponse.getName(), verificationrequest.getName());
    }

    //decision table parameters. (look in black box design pdf for full view).
    //Some are handled as integration tests and some as unit tests, and lastly some are handled in both. 
    static Stream<CreateRequestParameters> CreateRequestParametersThatThrowExceptions() {
        return Stream.of(
            // P4
            new CreateRequestParameters(
                new CreateCharacterRequestDTO( 99999, 99999, 99999, "Bobby"),
                ResourceNotFoundException.class,
                "Chapter not found with id: 99999"
            ),
            // P9
            new CreateRequestParameters(
                new CreateCharacterRequestDTO( 99999, 1, 99999, "Bobby"),
                ResourceNotFoundException.class,
                "Chapter not found with id: 99999"
            ),
            // P10
            new CreateRequestParameters(
                new CreateCharacterRequestDTO( 1, 99999, 99999, "Bobby"),
                ResourceNotFoundException.class,
                "Race details not found with id: 99999"
            ),
            // P11
            new CreateRequestParameters(
                new CreateCharacterRequestDTO( null, null, null, "Bobby"),
                InvalidDataAccessApiUsageException.class,
                "The given id must not be null"
            ),
            // P12
            new CreateRequestParameters(
                new CreateCharacterRequestDTO( 1, null, null, "Bobby"),
                InvalidDataAccessApiUsageException.class,
                "The given id must not be null"
            ),
            // P13
            new CreateRequestParameters(
                new CreateCharacterRequestDTO( 1, 1, null, "Bobby"),
                InvalidDataAccessApiUsageException.class,
                "The given id must not be null"
            ),
            // P14
            new CreateRequestParameters(
                new CreateCharacterRequestDTO( 99999, 99999, 1, "Bobby"),
                ResourceNotFoundException.class,
                "Chapter not found with id: 99999"
            )
        );
    }

    @ParameterizedTest
    @MethodSource("CreateRequestParametersThatThrowExceptions")
    void createCharacter_ShouldThrowExceptionOnParameterSetups(CreateRequestParameters params
    ) {
        // Arrange
        CreateCharacterRequestDTO request = params.request();

        // Act + Assert
        assertThatThrownBy(() -> characterService.createCharacter(4, request))
            .isInstanceOf(params.exceptionToVerify())
            .hasMessageContaining(params.exceptionMessage());        
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
    //deleteCharacter method
    //-------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    void deleteCharacter_shouldDeleteWhenExists() {
        //Arrange
        Integer queryId = getCreateResponseDTO(4).getId();
        
        //Act + Assert
        //We can only assert on that when it should be deleted it will not throw an exception.
        assertDoesNotThrow(() ->
                characterService.deleteCharacter(queryId));
    }

    @Test
    void deleteCharacter_shouldThrowResourceNotFoundExceptionWhenNotExists() {
        //Arrange
        Integer queryId = 999999999; //Is the literal largest integer value so if this is in use we have a whole different problem
        //So this integer could potentially catch a whole different bug for us, if we were to use sanitised data as the seed data for the test database.

        //Act + Assert
        assertThatThrownBy(() ->
                characterService.deleteCharacter(queryId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Character not found with id: " + queryId);
    }

    @Test
    void deleteCharacter_shouldThrowResourceNotFoundExceptionWhenIdIsNull() {
        //Arrange
        Integer queryId = null; //Is the literal largest integer value so if this is in use we have a whole different problem
        //So this integer could potentially catch a whole different bug for us, if we were to use sanitised data as the seed data for the test database.

        //Act + Assert
        assertThatThrownBy(() ->
                characterService.deleteCharacter(queryId))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("The given id must not be null");
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
    //getCharactersByAccountId method
    //-------------------------------------------------------------------------------------------------------------------------------------------

    @Test
    void getCharactersByAccountId_shouldReturnListOfCharacterResponseDTO_OnlyOnesWithCorrectAccountId() {
        //Arrange
        Integer queryId = 4;
        CharacterResponseDTO responseDTONotInList = getCreateResponseDTO(queryId + 1);
        CharacterResponseDTO responseDTO = getCreateResponseDTO(queryId);

        //Act
        List<CharacterResponseDTO> characters = characterService.getCharactersByAccountId(queryId);

        //Act
        assertNotNull(characters);

        assertTrue(
            characters.stream()
                .anyMatch(c -> c.getId().equals(responseDTO.getId()))
        );

        assertFalse(
            characters.stream()
                .anyMatch(c -> c.getId().equals(responseDTONotInList.getId()))
        );
    }

    @Test
    void getCharactersByAccountId_shouldThrowResourceNotFoundExceptionWhenNotExists() {
        //Arrange
        Integer queryId = 999999999; //Is the literal largest integer value so if this is in use we have a whole different problem
        //So this integer could potentially catch a whole different bug for us, if we were to use sanitised data as the seed data for the test database.

        //Act
        List<CharacterResponseDTO> characters = characterService.getCharactersByAccountId(queryId);

        //Act + Assert
        assertNotNull(characters);
        assertEquals(0, characters.size(),"Expected character to be of lengt 0 but it was: " + characters.size());

    }

    @Test
    void getCharactersByAccountId_shouldThrowResourceNotFoundExceptionWhenNull() {
        //Arrange
        Integer queryId = null; 

        //Act
        List<CharacterResponseDTO> characters = characterService.getCharactersByAccountId(queryId);

        //Act + Assert
        assertNotNull(characters);
        assertEquals(0, characters.size(),"Expected character to be of lengt 0 but it was: " + characters.size());

    }

    private CharacterResponseDTO getCreateResponseDTO(Integer accountId) {
        CreateCharacterRequestDTO createDTO = new CreateCharacterRequestDTO();
        createDTO.setRaceDetailsId(1);
        createDTO.setName("benny");
        return characterService.createCharacter(accountId, createDTO);
    }
}

record CreateRequestParameters(
    CreateCharacterRequestDTO request, 
    Class<? extends Exception> exceptionToVerify,
    String exceptionMessage
) {}