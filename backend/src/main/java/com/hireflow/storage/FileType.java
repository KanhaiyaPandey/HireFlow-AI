package com.hireflow.storage;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * File types we accept, recognised by their first bytes ("magic numbers"), not by file name or the
 * client's Content-Type header, both of which the client controls.
 */
public enum FileType {
    PDF("application/pdf", "pdf", bytes("%PDF-")),
    PNG("image/png", "png", new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}),
    JPEG("image/jpeg", "jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    /** "RIFF" + 4 size bytes + "WEBP": checked specially in {@link #matches}. */
    WEBP("image/webp", "webp", bytes("RIFF"));

    public static final Set<FileType> IMAGES = EnumSet.of(PNG, JPEG, WEBP);

    private final String contentType;
    private final String extension;
    private final byte[] signature;

    FileType(String contentType, String extension, byte[] signature) {
        this.contentType = contentType;
        this.extension = extension;
        this.signature = signature;
    }

    public String contentType() {
        return contentType;
    }

    public String extension() {
        return extension;
    }

    public static Optional<FileType> detect(byte[] content) {
        return Arrays.stream(values()).filter(type -> type.matches(content)).findFirst();
    }

    private boolean matches(byte[] content) {
        if (content.length < signature.length || !Arrays.equals(content, 0, signature.length, signature, 0, signature.length)) {
            return false;
        }
        if (this == WEBP) {
            byte[] webp = bytes("WEBP");
            return content.length >= 12 && Arrays.equals(content, 8, 12, webp, 0, 4);
        }
        return true;
    }

    private static byte[] bytes(String ascii) {
        return ascii.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }
}
