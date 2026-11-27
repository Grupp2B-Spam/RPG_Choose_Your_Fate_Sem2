package dk.ek.gruppe2.storycontent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dk.ek.gruppe2.storycontent.api.SceneController;
import dk.ek.gruppe2.storycontent.config.ApiVersionConfig;
import dk.ek.gruppe2.storycontent.service.SceneService;

/**
 * The HTTP layer: how a request is versioned, and what happens when it is not.
 *
 * A slice test, so it runs without Docker. The integration tests call the service classes
 * directly and never touch a controller, so nothing else covers routing, the version header
 * or request validation.
 */
@WebMvcTest(SceneController.class)
@DisplayName("The API is versioned by header, not by path")
class ApiVersioningTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SceneService scenes;

    @Test
    @DisplayName("a request with X-API-VERSION: 1 is served")
    void aVersionedRequestIsServed() throws Exception {
        given(scenes.findAll()).willReturn(List.of());

        mvc.perform(get("/admin/scenes").header(ApiVersionConfig.HEADER, "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("the same request without the header is refused")
    void anUnversionedRequestIsRefused() throws Exception {
        // The endpoint declares a version, so the header is mandatory. Without the
        // interceptor this would quietly succeed: versionRequired is off globally so Swagger
        // and the Kubernetes probes work without a header.
        mvc.perform(get("/admin/scenes"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("the old /admin/v1 path is gone")
    void theOldPathVersionIsGone() throws Exception {
        mvc.perform(get("/admin/v1/scenes").header(ApiVersionConfig.HEADER, "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("a choice without a beat is rejected, because a beat cannot be added later")
    void aChoiceWithoutABeatIsRejected() throws Exception {
        String body = """
                {"destinationSceneId": 2, "label": "Trust him"}
                """;

        mvc.perform(post("/admin/scenes/1/choices")
                        .header(ApiVersionConfig.HEADER, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("beat")));
    }

    @Test
    @DisplayName("a missing scene answers 404 rather than a stack trace")
    void aMissingSceneIsNotFound() throws Exception {
        given(scenes.findById(any()))
                .willThrow(new dk.ek.gruppe2.storycontent.api.ApiExceptions.NotFoundException(
                        "Scene 99 does not exist"));

        mvc.perform(get("/admin/scenes/99").header(ApiVersionConfig.HEADER, "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Scene 99 does not exist"));
    }
}
