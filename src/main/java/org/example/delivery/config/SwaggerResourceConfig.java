package org.example.delivery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for mapping Swagger UI resources to correct webjars location.
 * This resolves the issue where Swagger UI HTML tries to load resources via relative paths
 * (like ./swagger-ui.css) but they are actually located under /webjars/swagger-ui/{version}/.
 */
@Configuration
public class SwaggerResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/5.17.14/");
    }
}