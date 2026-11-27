package com.hireflow.storage;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hireflow.common.exception.ExternalServiceException;

/**
 * Cloudinary storage (decision D15). Behaviour verified against a real account on Day 6:
 *
 * <ul>
 *   <li>Images: resource type {@code image}, delivery type {@code upload} (public). Replacing one
 *       returns a new versioned URL, so CDN caches never serve the old logo.</li>
 *   <li>Resumes: resource type {@code raw}, delivery type {@code authenticated}. The plain URL returns
 *       401; {@code privateDownload} gives an expiring, signed api.cloudinary.com link. Raw files are
 *       not affected by Cloudinary's PDF-delivery restriction, which applies to image-type PDFs.</li>
 * </ul>
 */
public class CloudinaryFileStorage implements FileStorage {

    private static final String SERVICE = "Cloudinary";
    private static final String RAW = "raw";
    private static final String AUTHENTICATED = "authenticated";

    private final Cloudinary cloudinary;
    private final String folder;
    private final Duration downloadLinkTtl;
    private final Clock clock;

    public CloudinaryFileStorage(Cloudinary cloudinary, String folder, Duration downloadLinkTtl, Clock clock) {
        this.cloudinary = cloudinary;
        this.folder = folder.replaceAll("^/+|/+$", "");
        this.downloadLinkTtl = downloadLinkTtl;
        this.clock = clock;
    }

    @Override
    public String storePublicImage(String key, byte[] bytes, FileType type) {
        Map<?, ?> result = call("upload image", () -> cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                "resource_type", "image",
                "type", "upload",
                "public_id", publicId(key),
                "overwrite", true,
                "invalidate", true)));
        return String.valueOf(result.get("secure_url"));
    }

    @Override
    public void deletePublicImage(String key) {
        call("delete image", () -> cloudinary.uploader().destroy(publicId(key),
                ObjectUtils.asMap("resource_type", "image", "invalidate", true)));
    }

    @Override
    public void storePrivateFile(String key, byte[] bytes, FileType type) {
        call("upload file", () -> cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                "resource_type", RAW,
                "type", AUTHENTICATED,
                "public_id", publicId(key), // raw files keep their extension in the public id
                "overwrite", false)));
    }

    @Override
    public PrivateFileAccess openPrivateFile(String key, String downloadName) {
        long expiresAt = clock.instant().plus(downloadLinkTtl).getEpochSecond();
        String url = call("sign download", () -> cloudinary.privateDownload(publicId(key), "", ObjectUtils.asMap(
                "resource_type", RAW,
                "type", AUTHENTICATED,
                "expires_at", expiresAt,
                "attachment", true)));
        return new PrivateFileAccess.Redirect(URI.create(url));
    }

    @Override
    public void deletePrivateFile(String key) {
        call("delete file", () -> cloudinary.uploader().destroy(publicId(key),
                ObjectUtils.asMap("resource_type", RAW, "type", AUTHENTICATED, "invalidate", true)));
    }

    private String publicId(String key) {
        return folder.isEmpty() ? key : folder + "/" + key;
    }

    @FunctionalInterface
    private interface CloudinaryCall<T> {
        T run() throws Exception;
    }

    /** Wraps SDK failures (network, auth, quota) so the API answers 503 instead of a raw 500. */
    @SuppressWarnings("unchecked")
    private <T> T call(String action, CloudinaryCall<?> call) {
        try {
            Object result = call.run();
            if (result instanceof Map<?, ?> map && map.get("error") != null) {
                throw new IOException(String.valueOf(map.get("error")));
            }
            return (T) result;
        } catch (Exception ex) {
            throw new ExternalServiceException(SERVICE, action + " failed", ex);
        }
    }
}
