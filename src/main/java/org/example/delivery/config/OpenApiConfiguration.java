package org.example.delivery.config;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.example.delivery.entity.BaseFeeEntity;
import org.example.delivery.entity.FeeConfigurationEntity;
import org.example.delivery.entity.WeatherEntity;
import org.example.delivery.exceptions.response.ExceptionResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenApiCustomizer deliveryOpenApiCustomizer() {
        return openAPI -> {
            Components components = openAPI.getComponents();
            if (components == null) {
                components = new Components();
                openAPI.setComponents(components);
            }
            registerSchema(components, BaseFeeEntity.class);
            registerSchema(components, FeeConfigurationEntity.class);
            registerSchema(components, WeatherEntity.class);
            registerSchema(components, ExceptionResponse.class);
        };
    }

    private void registerSchema(Components components, Class<?> type) {
        ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(false));
        if (resolvedSchema.schema != null) {
            components.addSchemas(type.getSimpleName(), resolvedSchema.schema);
        }
        if (resolvedSchema.referencedSchemas != null) {
            resolvedSchema.referencedSchemas.forEach(components::addSchemas);
        }
    }
}
