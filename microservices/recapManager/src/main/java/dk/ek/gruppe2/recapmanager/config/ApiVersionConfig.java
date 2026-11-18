package dk.ek.gruppe2.recapmanager.config;

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

@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {
    public static final String HEADER = "X-API-VERSION";

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.useRequestHeader(HEADER)
                .detectSupportedVersions(true)
                // Unversioned routes (including Swagger) do not require the header.
                // The interceptor below requires it only on versioned handler methods.
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
