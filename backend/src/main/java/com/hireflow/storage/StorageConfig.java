package com.hireflow.storage;

import java.nio.file.Path;
import java.time.Clock;

import com.cloudinary.Cloudinary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Picks the FileStorage implementation from {@code hireflow.storage.type}. */
@Configuration
public class StorageConfig {

    @Bean
    FileStorage fileStorage(StorageProperties properties, Clock clock) {
        return switch (properties.type()) {
            case LOCAL -> new LocalFileStorage(Path.of(properties.local().dir()), properties.local().publicBaseUrl(), clock);
            case CLOUDINARY -> {
                StorageProperties.Cloudinary config = properties.cloudinary();
                if (config == null || !StringUtils.hasText(config.url()) || !config.url().startsWith("cloudinary://")) {
                    throw new IllegalStateException(
                            "STORAGE_TYPE=cloudinary needs CLOUDINARY_URL=cloudinary://<api_key>:<api_secret>@<cloud_name>");
                }
                yield new CloudinaryFileStorage(new Cloudinary(config.url()), config.folder(),
                        properties.downloadLinkTtl(), clock);
            }
        };
    }

    /** With local storage, the API itself serves public images at {@code /media/**}. */
    @Configuration
    @ConditionalOnProperty(name = "hireflow.storage.type", havingValue = "local", matchIfMissing = true)
    static class LocalMediaConfig implements WebMvcConfigurer {

        private final StorageProperties properties;

        LocalMediaConfig(StorageProperties properties) {
            this.properties = properties;
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            Path publicRoot = Path.of(properties.local().dir()).toAbsolutePath().normalize().resolve("public");
            registry.addResourceHandler("/media/**").addResourceLocations(publicRoot.toUri().toString());
        }
    }
}
