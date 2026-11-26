package dk.ek.gruppe2.storycontent.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.accept.MissingApiVersionException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The HTTP API is versioned by request header, not by a segment in the path.
 *
 * <p>A caller sends {@code X-API-VERSION: 1}; endpoints declare which version they answer
 * with {@code @PostMapping(path = "...", version = "1")}. Same arrangement as
 * RecapManager, so the whole system is called the same way.
 *
 * <p>This uses the framework's own API versioning rather than anything hand-rolled, so
 * routing by version is the framework's job and a request for a version nobody serves
 * fails before it reaches a controller.
 *
 * <p><b>Why the version is not required globally.</b> {@code setVersionRequired(false)}
 * lets unversioned routes — Swagger UI, the actuator health endpoints — work without the
 * header, which they must, since Kubernetes probes will not send one. The interceptor
 * below then demands the header only where a handler actually declares a version. Without
 * that pair, either the probes break or a versioned endpoint silently accepts a request
 * that never said which version it wanted.
 *
 * <p>Do not confuse this with the two other version numbers in the system:
 * {@code schemaVersion} in a published message's envelope, and the per-entity
 * {@code sceneVersion} / {@code itemVersion} / {@code raceVersion}, which are how a
 * consumer spots a stale message rather than a version of any interface.
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    public static final String HEADER = "X-API-VERSION";

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.useRequestHeader(HEADER)
                .detectSupportedVersions(true)
                .setVersionRequired(false);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (handler instanceof HandlerMethod method) {
                    RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(
                            method.getMethod(), RequestMapping.class);
                    if (mapping != null && StringUtils.hasText(mapping.version())
                            && !StringUtils.hasText(request.getHeader(HEADER))) {
                        throw new MissingApiVersionException();
                    }
                }
                return true;
            }
        });
    }
}
