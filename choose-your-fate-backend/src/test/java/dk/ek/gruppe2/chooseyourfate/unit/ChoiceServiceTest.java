package dk.ek.gruppe2.chooseyourfate.unit;

import dk.ek.gruppe2.chooseyourfate.dto.choice.ChoiceResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.choice.CreateChoiceRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.choice.UpdateChoiceRequestDTO;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Chapter;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Choice;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Scene;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.ChoiceRepository;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.SceneRepository;
import dk.ek.gruppe2.chooseyourfate.service.ChoiceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChoiceServiceTest {

    @Mock
    private ChoiceRepository choiceRepository;

    @Mock
    private SceneRepository sceneRepository;

    @InjectMocks
    private ChoiceService choiceService;
    private Scene townGate;
    private Scene marketSquare;
    private Scene watchtower;
    private Choice enterMarketChoice;
    private Choice climbWatchtowerChoice;
    private CreateChoiceRequestDTO createChoiceRequest;
    private UpdateChoiceRequestDTO updateChoiceRequest;

    @BeforeEach
    void setUp() {
        Chapter chapter = new Chapter();
        chapter.setId(1);
        chapter.setName("Festival Chapter");

        townGate = scene(1, "Town Gate", chapter);
        marketSquare = scene(2, "Market Square", chapter);
        watchtower = scene(3, "Watchtower", chapter);

        enterMarketChoice = choice(
                1,
                townGate,
                marketSquare,
                "Show the guard your invitation and step into the festival crowd.",
                "gain_quest",
                1,
                1,
                "{\"requires\":[],\"grants\":[\"festival-access\"]}"
        );
        climbWatchtowerChoice = choice(
                2,
                townGate,
                watchtower,
                "Climb the watchtower to get a better view of the city.",
                "gain_stat",
                null,
                1,
                "{\"requires\":[],\"grants\":[\"watchtower-visited\"]}"
        );

        createChoiceRequest = new CreateChoiceRequestDTO();
        createChoiceRequest.setSceneId(1);
        createChoiceRequest.setDestinationSceneId(2);
        createChoiceRequest.setDescription("Show the guard your invitation and step into the festival crowd.");
        createChoiceRequest.setConsequence("gain_quest");
        createChoiceRequest.setTargetId(1);
        createChoiceRequest.setValueInt(1);
        createChoiceRequest.setStoryWeight((short) 8);
        createChoiceRequest.setRequirements("{\"requires\":[],\"grants\":[\"festival-access\"]}");

        updateChoiceRequest = new UpdateChoiceRequestDTO();
        updateChoiceRequest.setSceneId(1);
        updateChoiceRequest.setDestinationSceneId(2);
        updateChoiceRequest.setDescription("Take the festival road toward the market.");
        updateChoiceRequest.setConsequence("gain_stat");
        updateChoiceRequest.setTargetId(2);
        updateChoiceRequest.setValueInt(2);
        updateChoiceRequest.setStoryWeight((short) 5);
        updateChoiceRequest.setRequirements("{\"requires\":[\"festival-access\"]}");
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(choiceRepository, sceneRepository);
    }

    // Checks that an existing choice is fetched and mapped to a response DTO.
    @Test
    void getChoiceByIdReturnsChoiceMappedFromRepositoryEntity() {
        when(choiceRepository.findById(1)).thenReturn(Optional.of(enterMarketChoice));

        ChoiceResponseDTO actual = choiceService.getChoiceById(1);

        assertEquals("1", actual.getId());
        assertEquals("1", actual.getSceneId());
        assertEquals("2", actual.getDestinationSceneId());
        assertEquals("Show the guard your invitation and step into the festival crowd.", actual.getDescription());
        assertEquals("gain_quest", actual.getConsequence());
        assertEquals(1, actual.getTargetId());
        assertEquals(1, actual.getValueInt());
        assertEquals("{\"requires\":[],\"grants\":[\"festival-access\"]}", actual.getRequirements());
        verify(choiceRepository).findById(1);
    }

    // Checks that a missing choice ID throws the expected not-found exception.
    @Test
    void getChoiceByIdThrowsResourceNotFoundWhenChoiceDoesNotExist() {
        when(choiceRepository.findById(0)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> choiceService.getChoiceById(0)
        );

        assertEquals("Choice not found with id: 0", exception.getMessage());
        verify(choiceRepository).findById(0);
    }

    // Checks that all repository choices are mapped into response DTOs.
    @Test
    void getAllChoicesReturnsMappedChoicesFromRepositoryEntities() {
        when(choiceRepository.findAll()).thenReturn(List.of(enterMarketChoice, climbWatchtowerChoice));

        List<ChoiceResponseDTO> actual = choiceService.getAllChoices();

        assertEquals(2, actual.size());
        assertEquals("1", actual.get(0).getId());
        assertEquals("2", actual.get(0).getDestinationSceneId());
        assertEquals("2", actual.get(1).getId());
        assertEquals("3", actual.get(1).getDestinationSceneId());
        verify(choiceRepository).findAll();
    }

    // Checks that a valid create request builds a Choice, saves it, and returns the saved DTO.
    @Test
    void createChoiceBuildsChoiceFromRequestAndSavesIt() {
        when(sceneRepository.findById(1)).thenReturn(Optional.of(townGate));
        when(sceneRepository.findById(2)).thenReturn(Optional.of(marketSquare));
        when(choiceRepository.save(any(Choice.class))).thenAnswer(invocation -> {
            Choice savedChoice = invocation.getArgument(0);
            savedChoice.setId(7);
            return savedChoice;
        });

        ChoiceResponseDTO actual = choiceService.createChoice(createChoiceRequest);

        ArgumentCaptor<Choice> choiceCaptor = ArgumentCaptor.forClass(Choice.class);
        verify(choiceRepository).save(choiceCaptor.capture());
        Choice savedChoice = choiceCaptor.getValue();

        assertSame(townGate, savedChoice.getScene());
        assertSame(marketSquare, savedChoice.getDestinationScene());
        assertEquals("Show the guard your invitation and step into the festival crowd.", savedChoice.getDescription());
        assertEquals("gain_quest", savedChoice.getConsequence());
        assertEquals(1, savedChoice.getTargetId());
        assertEquals(1, savedChoice.getValueInt());
        assertEquals((short) 8, savedChoice.getStoryWeight());
        assertEquals("{\"requires\":[],\"grants\":[\"festival-access\"]}", savedChoice.getRequirements());
        assertEquals("7", actual.getId());
        assertEquals("1", actual.getSceneId());
        assertEquals("2", actual.getDestinationSceneId());
        verify(sceneRepository).findById(1);
        verify(sceneRepository).findById(2);
    }

    // Checks that creating a choice fails when the source scene cannot be found.
    @Test
    void createChoiceThrowsResourceNotFoundWhenSourceSceneDoesNotExist() {
        createChoiceRequest.setSceneId(0);
        when(sceneRepository.findById(0)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> choiceService.createChoice(createChoiceRequest)
        );

        assertEquals("Scene not found with id: 0", exception.getMessage());
        verify(sceneRepository).findById(0);
        verify(choiceRepository, never()).save(any(Choice.class));
    }

    // Checks that creating a choice fails when the destination scene cannot be found.
    @Test
    void createChoiceThrowsResourceNotFoundWhenDestinationSceneDoesNotExist() {
        createChoiceRequest.setDestinationSceneId(99);
        when(sceneRepository.findById(1)).thenReturn(Optional.of(townGate));
        when(sceneRepository.findById(99)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> choiceService.createChoice(createChoiceRequest)
        );

        assertEquals("Scene not found with id: 99", exception.getMessage());
        verify(sceneRepository).findById(1);
        verify(sceneRepository).findById(99);
        verify(choiceRepository, never()).save(any(Choice.class));
    }

    // Checks that updating a missing choice fails before anything is saved.
    @Test
    void updateChoiceThrowsResourceNotFoundWhenChoiceDoesNotExist() {
        when(choiceRepository.findById(0)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> choiceService.updateChoice(0, updateChoiceRequest)
        );

        assertEquals("Choice not found with id: 0", exception.getMessage());
        verify(choiceRepository).findById(0);
        verify(choiceRepository, never()).save(any(Choice.class));
    }

    // Checks that deleting a missing choice fails before deleteById is called.
    @Test
    void deleteChoiceThrowsResourceNotFoundWhenChoiceDoesNotExist() {
        when(choiceRepository.existsById(0)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> choiceService.deleteChoice(0)
        );

        assertEquals("Choice not found with id: 0", exception.getMessage());
        verify(choiceRepository).existsById(0);
        verify(choiceRepository, never()).deleteById(0);
    }

    private Scene scene(Integer id, String name, Chapter chapter) {
        Scene scene = new Scene();
        scene.setId(id);
        scene.setName(name);
        scene.setChapter(chapter);
        return scene;
    }

    private Choice choice(
            Integer id,
            Scene scene,
            Scene destinationScene,
            String description,
            String consequence,
            Integer targetId,
            Integer valueInt,
            String requirements
    ) {
        Choice choice = new Choice();
        choice.setId(id);
        choice.setScene(scene);
        choice.setDestinationScene(destinationScene);
        choice.setDescription(description);
        choice.setConsequence(consequence);
        choice.setTargetId(targetId);
        choice.setValueInt(valueInt);
        choice.setStoryWeight((short) 8);
        choice.setRequirements(requirements);
        return choice;
    }
}
