package dk.ek.gruppe2.chooseyourfate.integration;

import dk.ek.gruppe2.chooseyourfate.TestContainerConfig;
import dk.ek.gruppe2.chooseyourfate.dto.scene.CreateSceneRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.scene.SceneLookaheadResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.scene.SceneResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.scene.UpdateSceneRequestDTO;
import dk.ek.gruppe2.chooseyourfate.exception.ResourceNotFoundException;
import dk.ek.gruppe2.chooseyourfate.model.mysql.Scene;
import dk.ek.gruppe2.chooseyourfate.repository.mysql.SceneRepository;
import dk.ek.gruppe2.chooseyourfate.service.SceneService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Testcontainers
@SpringBootTest
@Transactional
class SceneServiceIT {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestContainerConfig.MYSQL::getJdbcUrl);
        registry.add("spring.datasource.password", TestContainerConfig.MYSQL::getPassword);
        registry.add("spring.datasource.username", TestContainerConfig.MYSQL::getUsername);
    }

    @Autowired
    private SceneService sceneService;

    @Autowired
    private SceneRepository sceneRepository;

    // Verifies that the service returns scenes loaded from the seeded database.
    @Test
    void getAllScenes_ShouldReturnSeededScenes() {
        // Act
        List<SceneResponseDTO> scenes = sceneService.getAllScenes();

        // Assert
        assertThat(scenes)
                .isNotEmpty()
                .anySatisfy(scene -> assertAll(
                        () -> assertThat(scene.getId()).isEqualTo("1"),
                        () -> assertThat(scene.getName()).isEqualTo("Town Gate"),
                        () -> assertThat(scene.getChapterId()).isEqualTo("1")
                ));
    }

    // Verifies that an existing scene can be fetched by id.
    @Test
    void getSceneById_ShouldReturnScene_WhenSceneExists() {
        // Arrange
        Integer sceneId = 1;

        // Act
        SceneResponseDTO scene = sceneService.getSceneById(sceneId);

        // Assert
        assertAll(
                () -> assertThat(scene.getId()).isEqualTo(sceneId.toString()),
                () -> assertThat(scene.getName()).isEqualTo("Town Gate"),
                () -> assertThat(scene.getChapterId()).isEqualTo("1")
        );
    }

    // Verifies that fetching an unknown scene id throws a not-found exception.
    @Test
    void getSceneById_ShouldThrowResourceNotFoundException_WhenSceneDoesNotExist() {
        // Arrange
        Integer sceneId = 99999;

        // Act + Assert
        assertThatThrownBy(() -> sceneService.getSceneById(sceneId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Scene not found with id: " + sceneId);
    }

    // Verifies that fetching a scene with a null id is rejected.
    @Test
    void getSceneById_ShouldThrowResourceNotFoundException_WhenIdIsNull() {
        // Act + Assert
        assertThatThrownBy(() -> sceneService.getSceneById(null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Scene not found with id: null");
    }

    // Verifies that lookahead returns a scene's choices and their destination scenes.
    @Test
    void getSceneLookahead_ShouldReturnChoicesAndDestinationScenes_WhenSceneHasChoices() {
        // Arrange
        Integer sceneId = 1;

        // Act
        SceneLookaheadResponseDTO lookahead = sceneService.getSceneLookahead(sceneId);

        // Assert
        assertAll(
                () -> assertThat(lookahead.getScene().getId()).isEqualTo(sceneId.toString()),
                () -> assertThat(lookahead.getChoices()).hasSize(2),
                () -> assertThat(lookahead.getChoices())
                        .extracting("id")
                        .containsExactly("1", "2"),
                () -> assertThat(lookahead.getChoices())
                        .extracting("destinationSceneId")
                        .containsExactly("2", "3"),
                () -> assertThat(lookahead.getDestinationScenes())
                        .extracting("id")
                        .containsExactlyInAnyOrder("2", "3")
        );
    }

    // Verifies that lookahead for an unknown scene id throws a not-found exception.
    @Test
    void getSceneLookahead_ShouldThrowResourceNotFoundException_WhenSceneDoesNotExist() {
        // Arrange
        Integer sceneId = 99999;

        // Act + Assert
        assertThatThrownBy(() -> sceneService.getSceneLookahead(sceneId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Scene not found with id: " + sceneId);
    }

    // Verifies that lookahead with a null scene id is rejected.
    @Test
    void getSceneLookahead_ShouldThrowResourceNotFoundException_WhenIdIsNull() {
        // Act + Assert
        assertThatThrownBy(() -> sceneService.getSceneLookahead(null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Scene not found with id: null");
    }

    // Verifies that a scene is created and persisted when its chapter exists.
    @Test
    void createScene_ShouldPersistScene_WhenChapterExists() {
        // Arrange
        CreateSceneRequestDTO request = new CreateSceneRequestDTO();
        request.setName("Integration Test Scene");
        request.setChapterId(1);

        // Act
        SceneResponseDTO createdScene = sceneService.createScene(request);

        // Assert
        Scene persistedScene = sceneRepository.findById(Integer.valueOf(createdScene.getId())).orElseThrow();
        assertAll(
                () -> assertThat(createdScene.getName()).isEqualTo(request.getName()),
                () -> assertThat(createdScene.getChapterId()).isEqualTo(request.getChapterId().toString()),
                () -> assertThat(persistedScene.getName()).isEqualTo(request.getName()),
                () -> assertThat(persistedScene.getChapter().getId()).isEqualTo(request.getChapterId())
        );
    }

    // Verifies that a scene cannot be created for an unknown chapter.
    @Test
    void createScene_ShouldThrowResourceNotFoundException_WhenChapterDoesNotExist() {
        // Arrange
        CreateSceneRequestDTO request = new CreateSceneRequestDTO();
        request.setName("Missing Chapter Scene");
        request.setChapterId(99999);

        // Act + Assert
        assertThatThrownBy(() -> sceneService.createScene(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Chapter not found with id: " + request.getChapterId());
    }

    // Verifies that a scene cannot be created without a chapter id.
    @Test
    void createScene_ShouldThrowInvalidDataAccessApiUsageException_WhenChapterIdIsNull() {
        // Arrange
        CreateSceneRequestDTO request = createSceneRequest("Missing Chapter Id Scene", null);

        // Act + Assert
        assertThatThrownBy(() -> sceneService.createScene(request))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("The given id must not be null");
    }

    // Verifies that an existing scene's name and chapter can be updated.
    @Test
    void updateScene_ShouldPersistUpdatedNameAndChapter_WhenSceneAndChapterExist() {
        // Arrange
        SceneResponseDTO createdScene = sceneService.createScene(createSceneRequest("Scene Before Update", 1));
        Integer sceneId = Integer.valueOf(createdScene.getId());

        UpdateSceneRequestDTO request = new UpdateSceneRequestDTO();
        request.setName("Scene After Update");
        request.setChapterId(2);

        // Act
        SceneResponseDTO updatedScene = sceneService.updateScene(sceneId, request);

        // Assert
        Scene persistedScene = sceneRepository.findById(sceneId).orElseThrow();
        assertAll(
                () -> assertThat(updatedScene.getId()).isEqualTo(sceneId.toString()),
                () -> assertThat(updatedScene.getName()).isEqualTo(request.getName()),
                () -> assertThat(updatedScene.getChapterId()).isEqualTo(request.getChapterId().toString()),
                () -> assertThat(persistedScene.getName()).isEqualTo(request.getName()),
                () -> assertThat(persistedScene.getChapter().getId()).isEqualTo(request.getChapterId())
        );
    }

    // Verifies that an unknown scene cannot be updated.
    @Test
    void updateScene_ShouldThrowResourceNotFoundException_WhenSceneDoesNotExist() {
        // Arrange
        Integer sceneId = 99999;
        UpdateSceneRequestDTO request = new UpdateSceneRequestDTO();
        request.setName("Never Updated");
        request.setChapterId(1);

        // Act + Assert
        assertThatThrownBy(() -> sceneService.updateScene(sceneId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Scene not found with id: " + sceneId);
    }

    // Verifies that updating a scene with a null scene id is rejected.
    @Test
    void updateScene_ShouldThrowResourceNotFoundException_WhenSceneIdIsNull() {
        // Arrange
        UpdateSceneRequestDTO request = new UpdateSceneRequestDTO();
        request.setName("Never Updated");
        request.setChapterId(1);

        // Act + Assert
        assertThatThrownBy(() -> sceneService.updateScene(null, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Scene not found with id: null");
    }

    // Verifies that a scene cannot be moved to an unknown chapter.
    @Test
    void updateScene_ShouldThrowResourceNotFoundException_WhenChapterDoesNotExist() {
        // Arrange
        SceneResponseDTO createdScene = sceneService.createScene(createSceneRequest("Scene Before Bad Update", 1));

        UpdateSceneRequestDTO request = new UpdateSceneRequestDTO();
        request.setName("Never Updated");
        request.setChapterId(99999);

        // Act + Assert
        assertThatThrownBy(() -> sceneService.updateScene(Integer.valueOf(createdScene.getId()), request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Chapter not found with id: " + request.getChapterId());
    }

    // Verifies that a scene cannot be updated without a chapter id.
    @Test
    void updateScene_ShouldThrowInvalidDataAccessApiUsageException_WhenChapterIdIsNull() {
        // Arrange
        SceneResponseDTO createdScene = sceneService.createScene(createSceneRequest("Scene Before Null Chapter Update", 1));

        UpdateSceneRequestDTO request = new UpdateSceneRequestDTO();
        request.setName("Never Updated");
        request.setChapterId(null);

        // Act + Assert
        assertThatThrownBy(() -> sceneService.updateScene(Integer.valueOf(createdScene.getId()), request))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("The given id must not be null");
    }

    // Verifies that an existing scene can be deleted from the database.
    @Test
    void deleteScene_ShouldDeleteScene_WhenSceneExists() {
        // Arrange
        SceneResponseDTO createdScene = sceneService.createScene(createSceneRequest("Scene To Delete", 1));
        Integer sceneId = Integer.valueOf(createdScene.getId());

        // Act
        assertDoesNotThrow(() -> sceneService.deleteScene(sceneId));

        // Assert
        assertThat(sceneRepository.existsById(sceneId)).isFalse();
    }

    // Verifies that deleting an unknown scene id throws a not-found exception.
    @Test
    void deleteScene_ShouldThrowResourceNotFoundException_WhenSceneDoesNotExist() {
        // Arrange
        Integer sceneId = 99999;

        // Act + Assert
        assertThatThrownBy(() -> sceneService.deleteScene(sceneId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Scene not found with id: " + sceneId);
    }

    // Verifies that deleting a scene with a null id is rejected.
    @Test
    void deleteScene_ShouldThrowInvalidDataAccessApiUsageException_WhenIdIsNull() {
        // Act + Assert
        assertThatThrownBy(() -> sceneService.deleteScene(null))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("The given id must not be null");
    }

    private CreateSceneRequestDTO createSceneRequest(String name, Integer chapterId) {
        CreateSceneRequestDTO request = new CreateSceneRequestDTO();
        request.setName(name);
        request.setChapterId(chapterId);
        return request;
    }
}
