package dk.ek.gruppe2.recapmanager.integration;

import dk.ek.gruppe2.recapmanager.config.*;
import dk.ek.gruppe2.recapmanager.controller.*;
import dk.ek.gruppe2.recapmanager.dto.AiRequestV2DTO;
import dk.ek.gruppe2.recapmanager.dto.AiResponseV2DTO;
import dk.ek.gruppe2.recapmanager.dto.AiResponseV2DTO.AudioStatus;
import dk.ek.gruppe2.recapmanager.security.CharacterAuthorizationService;
import dk.ek.gruppe2.recapmanager.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AiController.class, properties = {
        "app.security.jwt.issuer=http://issuer.test", "app.security.jwt.audience=test"})
@Import({SecurityConfig.class, ApiVersionConfig.class, RecapExceptionHandler.class, RecapHttpIT.TestSecurity.class})
class RecapHttpIT {
    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurity {}
    @Autowired MockMvc mvc;
    @MockitoBean RecapService recaps;
    @MockitoBean AiService ai;
    @MockitoBean(name = "characterAuthorizationService") CharacterAuthorizationService authorization;
    @MockitoBean JwtDecoder decoder;
    private static final String URL = "/choose-your-fate/ai/ask";
    private static final String BODY = "{\"characterId\":1,\"requestType\":\"PATH_SUMMARY\"}";

    @Test void ownershipDenialHappensBeforeDataRetrieval() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content(BODY).header("X-API-VERSION", "2")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(recaps);
    }

    @Test void versionTwoReturnsTextAndOptionalAudio() throws Exception {
        when(authorization.canAccessCharacter(eq(1), any())).thenReturn(true);
        when(recaps.latest(any())).thenReturn(new AiResponseV2DTO("A journey", AudioStatus.PENDING, null, "hash"));
        mvc.perform(post(URL).contentType("application/json")
                .content("{\"characterId\":1,\"requestType\":\"PATH_SUMMARY\",\"audio\":true}")
                .header("X-API-VERSION", "2")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("A journey"))
                .andExpect(jsonPath("$.audioStatus").value("PENDING"));
        var captured = ArgumentCaptor.forClass(AiRequestV2DTO.class);
        verify(recaps).latest(captured.capture());
        assertThat(captured.getValue().isAudio()).isTrue();
        verifyNoInteractions(ai);
    }

    @Test void originalPostContractStillWorks() throws Exception {
        when(ai.handleRequest(any())).thenReturn("A journey");
        mvc.perform(post("/choose-your-fate/ai/ask").header("X-API-VERSION", "1")
                .contentType("application/json").content("{\"characterId\":1,\"requestType\":\"PATH_SUMMARY\"}")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.response").value("A journey"))
                .andExpect(jsonPath("$.audioStatus").doesNotExist())
                .andExpect(jsonPath("$.audioUrl").doesNotExist());
        verifyNoInteractions(recaps);
    }

    @Test void versionTwoDefaultsToTextOnly() throws Exception {
        when(recaps.latest(any())).thenReturn(new AiResponseV2DTO("A journey", AudioStatus.NOT_REQUESTED, null, null));
        mvc.perform(post(URL).header("X-API-VERSION", "2").contentType("application/json").content(BODY)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.audioStatus").value("NOT_REQUESTED"));
        var captured = ArgumentCaptor.forClass(AiRequestV2DTO.class);
        verify(recaps).latest(captured.capture());
        assertThat(captured.getValue().isAudio()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"characterId\":0,\"requestType\":\"PATH_SUMMARY\",\"audio\":true}",
            "{\"characterId\":-1,\"requestType\":\"PATH_SUMMARY\",\"audio\":true}",
            "{\"requestType\":\"PATH_SUMMARY\",\"audio\":true}",
            "{\"characterId\":1,\"audio\":true}"
    })
    void versionTwoRejectsEachInvalidFieldIndependently(String body) throws Exception {
        mvc.perform(post(URL).header("X-API-VERSION", "2").contentType("application/json")
                .content(body)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(recaps, ai);
    }

}
