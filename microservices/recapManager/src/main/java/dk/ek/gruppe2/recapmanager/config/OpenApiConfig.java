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

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

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
                        .description("Character recaps. Requires an RS256 bearer JWT issued by AccountManager."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
