package com.celehub.backend.config;

import java.nio.file.Path;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Phục vụ file upload local tại /files/** (chỉ khi storage.type=local). */
@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local")
public class WebConfig implements WebMvcConfigurer {
    private final AppProperties props;

    public WebConfig(AppProperties props) {
        this.props = props;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Path.of(props.storage().localDir()).toAbsolutePath().normalize();
        registry.addResourceHandler("/files/**").addResourceLocations(root.toUri().toString());
    }
}