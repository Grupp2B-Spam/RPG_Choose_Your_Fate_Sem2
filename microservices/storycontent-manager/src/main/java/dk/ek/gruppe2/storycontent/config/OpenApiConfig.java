package dk.ek.gruppe2.storycontent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

@Configuration
public class OpenApiConfig {

    /**
     * Shows X-API-VERSION as a required parameter on every versioned endpoint. Without this
     * the header is invisible in Swagger UI and every "Try it out" fails with a
     * missing-version error.
     */
    @Bean
    public OperationCustomizer apiVersionHeader() {
        return (operation, handlerMethod) -> {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getMethod(), RequestMapping.class);

            if (mapping == null || !StringUtils.hasText(mapping.version())) {
                return operation;
            }

            Parameter parameter = operation.getParameters() == null ? null
                    : operation.getParameters().stream()
                            .filter(existing -> ApiVersionConfig.HEADER.equalsIgnoreCase(existing.getName())
                                    && "header".equals(existing.getIn()))
                            .findFirst()
                            .orElse(null);

            if (parameter == null) {
                parameter = new Parameter().name(ApiVersionConfig.HEADER).in("header");
                operation.addParametersItem(parameter);
            }

            parameter.required(true)
                    .description("API version this endpoint answers")
                    .schema(new StringSchema())
                    .example(mapping.version());

            return operation;
        };
    }

    @Bean
    public OpenAPI storyContentOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("StoryContentManager API")
                .version("1.0")
                .description("""
                        Authoring API for story content: chapters, scenes, choices, NPCs, \
                        races, and item and quest definitions. Every change here is \
                        published to the broker for the rest of the system to consume.

                        Admin only. Send X-API-VERSION: 1."""));
    }
}
