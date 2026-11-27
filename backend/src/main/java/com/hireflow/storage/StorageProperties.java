package com.hireflow.storage;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@code hireflow.storage.*} (env {@code STORAGE_TYPE}, {@code STORAGE_LOCAL_DIR}, {@code CLOUDINARY_URL},
 * {@code CLOUDINARY_FOLDER}).
 *
 * @param downloadLinkTtl how long a signed resume download link stays valid
 */
@Validated
@ConfigurationProperties(prefix = "hireflow.storage")
public record StorageProperties(
        @NotNull Type type,
        @NotNull Duration downloadLinkTtl,
        Local local,
        Cloudinary cloudinary) {

    public enum Type { LOCAL, CLOUDINARY }

    /**
     * @param dir           where files are written
     * @param publicBaseUrl how browsers reach public images (served by the API at {@code /media/**})
     */
    public record Local(String dir, String publicBaseUrl) {
    }

    /**
     * @param url    {@code cloudinary://<api_key>:<api_secret>@<cloud_name>}
     * @param folder prefix for every file, keeping environments apart in one account
     */
    public record Cloudinary(String url, String folder) {
    }
}
