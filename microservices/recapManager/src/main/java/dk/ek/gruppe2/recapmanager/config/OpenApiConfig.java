package dk.ek.gruppe2.recapmanager.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.util.StringUtils;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.models.GroupedOpenApi;
import dk.ek.gruppe2.recapmanager.controller.AiController;
import dk.ek.gruppe2.recapmanager.dto.AiRequestDTO;
import dk.ek.gruppe2.recapmanager.dto.AiRequestV2DTO;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public GroupedOpenApi aiV1Api() throws NoSuchMethodException {
        return aiApi("ask", AiRequestDTO.class);
    }

    @Bean
    public GroupedOpenApi aiV2Api() throws NoSuchMethodException {
        return aiApi("askV2", AiRequestV2DTO.class);
    }

    private GroupedOpenApi aiApi(String handler, Class<?> requestType) throws NoSuchMethodException {
        // Read versions from endpoint methods; documentation has no separate supported-version list.
        String version = AnnotatedElementUtils.findMergedAnnotation(
                AiController.class.getMethod(handler, requestType), RequestMapping.class).version();
        return GroupedOpenApi.builder().group("ai-v" + version)
                .packagesToScan(AiController.class.getPackageName())
                .addOpenApiMethodFilter(method -> {
                    RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                    return mapping != null && version.equals(mapping.version());
                })
                .addOperationCustomizer(apiVersionHeader()).build();
    }

    @Bean
    public OperationCustomizer apiVersionHeader() {
        return (operation, handlerMethod) -> {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getMethod(), RequestMapping.class);
            if (mapping != null && StringUtils.hasText(mapping.version())) {
                Parameter parameter = operation.getParameters() == null ? null : operation.getParameters().stream()
                        .filter(existing -> ApiVersionConfig.HEADER.equalsIgnoreCase(existing.getName())
                                && "header".equals(existing.getIn()))
                        .findFirst().orElse(null);
                if (parameter == null) {
                    parameter = new Parameter().name(ApiVersionConfig.HEADER).in("header");
                    operation.addParametersItem(parameter);
                }
                parameter.required(true).description("API version declared by this endpoint")
                        .schema(new StringSchema()).example(mapping.version());
            }
            return operation;
        };
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Recap Manager API")
                        .version("1.0")
                        .description("Character recaps. Requires a Keycloak RS256 access token. Offline PEM verification is available in the local profile."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
