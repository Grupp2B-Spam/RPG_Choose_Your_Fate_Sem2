package dk.ek.gruppe2.chooseyourfate.unit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import dk.ek.gruppe2.chooseyourfate.dto.CharacterResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.CreateCharacterRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.MultipleCharacterViewsResponseDto;
import dk.ek.gruppe2.chooseyourfate.enums.Role;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Account;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Chapter;
import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterAvatar;
import dk.ek.gruppe2.chooseyourfate.model.mysql.CharacterDetails;
import dk.ek.gruppe2.chooseyourfate.model.mysql.RaceDetails;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Scene;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.AccountRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.ChapterRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.CharacterAvatarRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.CharacterDetailsRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.RaceDetailsRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.SceneRepository;
import dk.ek.gruppe2.chooseyourfate.service.CharacterService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.StoredProcedureQuery;

@ExtendWith(MockitoExtension.class)
class CharacterServiceUnitTests {

    @Mock
    private EntityManager entityManager;

    @Mock
    private StoredProcedureQuery storedProcedureQuery;

    @Mock
    private CharacterAvatarRepository characterAvatarRepository;

    @Mock
    private RaceDetailsRepository raceDetailsRepository;

    @Mock
    private SceneRepository sceneRepository;

    @Mock
    private ChapterRepository chapterRepository;
    
    @Mock
    private CharacterDetailsRepository characterDetailsRepository;

    @Mock
    private AccountRepository accountRepository;


    @InjectMocks
    private CharacterService characterService;

    //-------------------------------------------------------------------------------------------------------------------------------------------
    //GetAllCharacters method
    //-------------------------------------------------------------------------------------------------------------------------------------------
    @Test
    void getAllCharacters_shouldReturnNonNullEmptyArray() {
        //Arrange
        when(characterAvatarRepository.findAll()).thenReturn(List.of()); // the list should be empty to test handling of finding nothing

        //Act
        List<CharacterResponseDTO> characters = characterService.getAllCharacters();

        //Act
        assertNotNull(characters);
        assertEquals(0, characters.size(),"Expected character to be of lengt 0 but it was: " + characters.size());
    }

    @Test
    void getAllCharacters_shouldReturnListOfCharacterResponseDTO() {
        //Arrange
        when(characterAvatarRepository.findAll()).thenReturn(List.of(getCharacterAvatar(1), getCharacterAvatar(2))); // the list should be empty to test handling of finding nothing

        //Act
        List<CharacterResponseDTO> characters = characterService.getAllCharacters();

        //Act
        assertNotNull(characters);
        assertEquals(2, characters.size(),"Expected character to be of lengt 0 but it was: " + characters.size());
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
    //getCharacterById method
    //-------------------------------------------------------------------------------------------------------------------------------------------
    @Test
    void getCharacterById_shouldReturnNonNullCharacterResponseDTO() {
        //Arrange
        Integer queryId = 1;
        when(characterAvatarRepository.findById(queryId))
            .thenReturn(Optional.of(getCharacterAvatar(queryId)));

        //Act
        CharacterResponseDTO character = characterService.getCharacterById(queryId);

        //Act
        assertNotNull(character);
        assertEquals(CharacterResponseDTO.class, character.getClass());
        assertEquals(character.getId(), queryId);
    }

    @Test
    void getCharacterById_OnNoCharacters_shouldThrowResourceNotFoundException() {
        //Arrange
        Integer queryId = 1;
        when(characterAvatarRepository.findById(queryId))
            .thenReturn(Optional.empty());


        //Act + Assert
        assertThatThrownBy(() ->
                characterService.getCharacterById(queryId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Character not found with id: " + queryId);
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
    //getCharactersViewBy_AccountId method
    //-------------------------------------------------------------------------------------------------------------------------------------------    
    @Test
    void getCharactersViewBy_AccountId_shouldReturnViewsListOfSameLengthAsCounted() {
        //Arrange
        Integer queryId = 2;

        Account account = new Account();
        account.setId(queryId);
        account.setCharacterLimit(5);
        when(characterAvatarRepository.countByAccount_Id(queryId))
            .thenReturn(Long.parseLong("2"));

        when(characterAvatarRepository.findByAccount_Id(queryId))
            .thenReturn(List.of(getCharacterAvatar(1), getCharacterAvatar(2)));

        when(characterDetailsRepository.findByIdWithCharacterView(1))
            .thenReturn(Optional.of(new CharacterDetails(1, getCharacterAvatar(1),5,2,3)));

        when(characterDetailsRepository.findByIdWithCharacterView(2))
            .thenReturn(Optional.of(new CharacterDetails(2, getCharacterAvatar(2),5,2,3)));

        
        when(accountRepository.findById(account.getId()))
            .thenReturn(Optional.of(account));

        //Act
        MultipleCharacterViewsResponseDto viewResponse = characterService.getCharactersViewBy_AccountId(queryId);

        //Act
        assertNotNull(viewResponse);
        assertEquals(MultipleCharacterViewsResponseDto.class, viewResponse.getClass());
        assertEquals(2, viewResponse.getViews().size(),"Expected character to be of lengt 0 but it was: " + viewResponse.getViews().size());
    }

    @Test
    void getCharactersViewBy_AccountId_ShouldReturnTrue_IfCharacterCount_IsLessThanAllowedAmount() {
        //Arrange
        Integer queryId = 2;

        Account account = new Account();
        account.setId(queryId);
        account.setCharacterLimit(5);
        when(characterAvatarRepository.countByAccount_Id(queryId))
            .thenReturn(Long.parseLong("2"));

        when(characterAvatarRepository.findByAccount_Id(queryId))
            .thenReturn(List.of(getCharacterAvatar(1), getCharacterAvatar(2)));

        when(characterDetailsRepository.findByIdWithCharacterView(1))
            .thenReturn(Optional.of(new CharacterDetails(1, getCharacterAvatar(1),5,2,3)));

        when(characterDetailsRepository.findByIdWithCharacterView(2))
            .thenReturn(Optional.of(new CharacterDetails(2, getCharacterAvatar(2),5,2,3)));

        
        when(accountRepository.findById(account.getId()))
            .thenReturn(Optional.of(account));
        //Act
        MultipleCharacterViewsResponseDto viewResponse = characterService.getCharactersViewBy_AccountId(queryId);

        //Act
        assertNotNull(viewResponse);
        assertEquals(MultipleCharacterViewsResponseDto.class, viewResponse.getClass());
        assertTrue(viewResponse.getCanCreateMoreCharacters());
    }

    @Test
    void getCharactersViewBy_AccountId_ShouldReturnFalse_IfCharacterCount_IsMoreThanAllowedAmount() {
        //Arrange
        Integer queryId = 2;

        Account account = new Account();
        account.setId(queryId);
        account.setCharacterLimit(5);
        when(characterAvatarRepository.countByAccount_Id(queryId))
            .thenReturn(Long.parseLong("5"));

        when(characterAvatarRepository.findByAccount_Id(queryId))
            .thenReturn(List.of(getCharacterAvatar(1), getCharacterAvatar(2),getCharacterAvatar(3), getCharacterAvatar(4), getCharacterAvatar(5)));

        when(characterDetailsRepository.findByIdWithCharacterView(1))
            .thenReturn(Optional.of(new CharacterDetails(1, getCharacterAvatar(1),5,2,3)));

        when(characterDetailsRepository.findByIdWithCharacterView(2))
            .thenReturn(Optional.of(new CharacterDetails(2, getCharacterAvatar(2),5,2,3)));

        when(characterDetailsRepository.findByIdWithCharacterView(3))
            .thenReturn(Optional.of(new CharacterDetails(3, getCharacterAvatar(2),5,2,3)));
        when(characterDetailsRepository.findByIdWithCharacterView(4))
            .thenReturn(Optional.of(new CharacterDetails(4, getCharacterAvatar(2),5,2,3)));
        when(characterDetailsRepository.findByIdWithCharacterView(5))
            .thenReturn(Optional.of(new CharacterDetails(5, getCharacterAvatar(2),5,2,3)));

        when(accountRepository.findById(account.getId()))
        
            .thenReturn(Optional.of(account));
        //Act
        MultipleCharacterViewsResponseDto viewResponse = characterService.getCharactersViewBy_AccountId(queryId);

        //Act
        assertNotNull(viewResponse);
        assertEquals(MultipleCharacterViewsResponseDto.class, viewResponse.getClass());
        assertFalse(viewResponse.getCanCreateMoreCharacters());
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
    //createCharacter method
    //-------------------------------------------------------------------------------------------------------------------------------------------

    //Had to split the list of P2, P5 and P6 into two test cases, since there were mockings that weren't used between runs in the seperate test cases.
    static Stream<CreateRequestParametersWithException> CreateRequestParametersWithException() {
        
        return Stream.of(
                //P2
                new CreateRequestParametersWithException(
                    new CreateCharacterRequestDTO(1, 2, null, "Bobby"),
                    false,
                    true,
                    true,
                    false,
                    true,
                    true,
                    IllegalArgumentException.class,
                    "Selected scene does not belong to the selected chapter."
                )
        );
    }

    @ParameterizedTest
    @MethodSource("CreateRequestParametersWithException")
    void createCharacter_ShouldReturnExpectedOutcome(CreateRequestParametersWithException params
    ) {
        // Arrange
        CreateCharacterRequestDTO request = params.request();
        when(chapterRepository.existsById(request.getChapterId()))
                .thenReturn(params.chapterExists());

        when(raceDetailsRepository.existsById(request.getRaceDetailsId()))
                .thenReturn(params.raceDetailsExists());
        
        RaceDetails raceDetails = new RaceDetails();
        raceDetails.setId(request.getRaceDetailsId());

        if (request.getChapterId() != null && request.getSceneId() != null) {

            Chapter sceneChapter = new Chapter();

            if (params.matchingChapter()) {
                sceneChapter.setId(request.getChapterId());
            } else {
                sceneChapter.setId(999999999);
            }

            Scene scene = new Scene();
            scene.setId(request.getSceneId());
            scene.setChapter(sceneChapter);

            when(sceneRepository.findById(request.getSceneId()))
                    .thenReturn(Optional.of(scene));
        }

         // Act + Assert
        assertThatThrownBy(() -> characterService.createCharacter(4, request))
            .isInstanceOf(params.exceptionToVerify())
            .hasMessageContaining(params.exceptionMessage());
    }

    static Stream<CreateRequestParametersWithException> CreateRequestParametersWithExceptionWhenStartingChaterOrSceneMissing() {
        
        return Stream.of(
                //P5
                new CreateRequestParametersWithException(
                    new CreateCharacterRequestDTO(null, null, 1, "Bobby"),
                    false,
                    true,
                    true,
                    false,
                    false,
                    true,
                    ResourceNotFoundException.class,
                    "Starting chapter not configured for race details with id: 1"
                ),

                //P6
                new CreateRequestParametersWithException(
                    new CreateCharacterRequestDTO(null, null, 1, "Bobby"),
                    false,
                    true,
                    true,
                    false,
                    true,
                    false,
                    ResourceNotFoundException.class,
                    "Starting scene not configured for race details with id: 1"
                )
        );
    }

    @ParameterizedTest
    @MethodSource("CreateRequestParametersWithExceptionWhenStartingChaterOrSceneMissing")
    void createCharacter_ShouldReturnExpectedOutcome_WhenStartingChapterOrSceneMissing(CreateRequestParametersWithException params
    ) {
        // Arrange
        CreateCharacterRequestDTO request = params.request();
        
        RaceDetails raceDetails = new RaceDetails();
        raceDetails.setId(request.getRaceDetailsId());
        //P5
        if (params.startingChapterExists() == true) {
            Chapter chapter = new Chapter();
            chapter.setId(1);
            
            //P6
            if (params.startingSceneExists() == true) {
                Scene startingScene = new Scene();

                chapter.setStartingScene(startingScene);
            }

            raceDetails.setStartingChapter(chapter);
        }
        
        when(raceDetailsRepository.findById(request.getRaceDetailsId()))
                .thenReturn(Optional.of(raceDetails));

        if (request.getChapterId() != null && request.getSceneId() != null) {

            Chapter sceneChapter = new Chapter();

            if (params.matchingChapter()) {
                sceneChapter.setId(request.getChapterId());
            } else {
                sceneChapter.setId(999999999);
            }

            Scene scene = new Scene();
            scene.setId(request.getSceneId());
            scene.setChapter(sceneChapter);

            when(sceneRepository.findById(request.getSceneId()))
                    .thenReturn(Optional.of(scene));
        }

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
        when(characterAvatarRepository.existsById(1)).thenReturn(true);

        when(entityManager.createStoredProcedureQuery("sp_delete_character"))
                .thenReturn(storedProcedureQuery);

        //Act
        characterService.deleteCharacter(1);

        //Assert
        verify(entityManager).createStoredProcedureQuery("sp_delete_character");
        verify(storedProcedureQuery).execute();
    }

    @Test
    void deleteCharacter_shouldThrowResourceNotFoundExceptionWhenNotExists() {
        //Arrange
        when(characterAvatarRepository.existsById(1)).thenReturn(false);
        
        //Act + Assert
        assertThatThrownBy(() ->
                characterService.deleteCharacter(1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Character not found with id: " + 1);
    }

    //-------------------------------------------------------------------------------------------------------------------------------------------
    //getCharactersByAccountId method
    //-------------------------------------------------------------------------------------------------------------------------------------------
    
    @Test
    void getCharactersByAccountId_shouldReturnNonNullEmptyArray() {
        //Arrange
        Integer queryId = 1;
        when(characterAvatarRepository.findByAccount_Id(queryId)).thenReturn(List.of()); // the list should be empty to test handling of finding nothing

        //Act
        List<CharacterResponseDTO> characters = characterService.getCharactersByAccountId(queryId);

        //Act
        assertNotNull(characters);
        assertEquals(0, characters.size(),"Expected character to be of lengt 0 but it was: " + characters.size());
    }

    @Test
    void getCharactersByAccountId_shouldReturnListOfCharacterResponseDTO() {
        //Arrange
        Integer queryId = 1;

        when(characterAvatarRepository.findByAccount_Id(queryId)).thenReturn(List.of(getCharacterAvatar(1), getCharacterAvatar(2)));

        //Act
        List<CharacterResponseDTO> characters = characterService.getCharactersByAccountId(queryId);

        //Act
        assertNotNull(characters);
        assertEquals(2, characters.size(),"Expected character to be of lengt 0 but it was: " + characters.size());
    }


    private CharacterAvatar getCharacterAvatar(Integer id) {
        CharacterAvatar characterAvatar = new CharacterAvatar();
        characterAvatar.setId(id);
        characterAvatar.setName("biggie");

        characterAvatar.setAccount(getAccount(1));

        characterAvatar.setChapter(getChapter());

        characterAvatar.setScene(getScene());

        characterAvatar.setRaceDetails(getRaceDetails());

        return characterAvatar;
    }

    private Account getAccount(Integer id) {
        Account account = new Account();
        account.setId(id);
        account.setUsername("Bob");
        account.setRole(Role.ROLE_USER);
        return account;
    }

    private Chapter getChapter() {
        Chapter chapter = new Chapter();

        chapter.setId(1);
        return chapter;
    }

    private Scene getScene() {
        Scene scene = new Scene();
        scene.setId(1);
        return scene;
    }

    private RaceDetails getRaceDetails() {
        RaceDetails raceDetails = new RaceDetails();
        raceDetails.setId(5);
        return raceDetails;
    }
}

record CreateRequestParametersWithException(
    CreateCharacterRequestDTO request, 
    boolean expectedValidation, 
    boolean raceDetailsExists, 
    boolean chapterExists, 
    boolean matchingChapter,
    boolean startingChapterExists,
    boolean startingSceneExists,
    Class<? extends Exception> exceptionToVerify,
    String exceptionMessage
) {}