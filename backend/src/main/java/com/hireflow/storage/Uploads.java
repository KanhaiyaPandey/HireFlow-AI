package com.hireflow.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

import com.hireflow.common.exception.FileTooLargeException;
import com.hireflow.common.exception.InvalidRequestException;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

/** Shared checks for every upload endpoint (docs/architecture.md §10). */
public final class Uploads {

    public static final DataSize MAX_RESUME = DataSize.ofMegabytes(5);
    public static final DataSize MAX_IMAGE = DataSize.ofMegabytes(1);
    private static final int MAX_NAME_LENGTH = 200;

    private Uploads() {
    }

    /** A validated upload: its bytes, its real type, and a safe display name. */
    public record ValidatedFile(byte[] bytes, FileType type, String fileName) {
    }

    /**
     * Reads the upload (at most {@code maxSize} + 1 bytes, so a huge file is never fully buffered),
     * checks its real type from its content, and cleans its name.
     */
    public static ValidatedFile validate(MultipartFile file, Set<FileType> allowed, DataSize maxSize) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Choose a file to upload.");
        }
        byte[] bytes = readAtMost(file, maxSize.toBytes());
        if (bytes.length > maxSize.toBytes()) {
            throw new FileTooLargeException("The file must be at most %d MB.".formatted(maxSize.toMegabytes()));
        }
        FileType type = FileType.detect(bytes)
                .filter(allowed::contains)
                .orElseThrow(() -> new InvalidRequestException("Unsupported file. Allowed: %s."
                        .formatted(allowed.stream().map(FileType::extension).sorted().collect(Collectors.joining(", ")))));
        return new ValidatedFile(bytes, type, safeFileName(file.getOriginalFilename(), type));
    }

    /**
     * The original name, for display and download only, never as a storage path: no directories,
     * no control characters, bounded length, and the extension of the real type.
     */
    public static String safeFileName(String original, FileType type) {
        String name = original == null ? "" : original;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("[\\p{Cntrl}\"<>|*?:]", "").strip();
        int dot = name.lastIndexOf('.');
        String base = dot >= 0 ? name.substring(0, dot) : name;
        if (base.isBlank()) {
            base = "file";
        }
        if (base.length() > MAX_NAME_LENGTH) {
            base = base.substring(0, MAX_NAME_LENGTH);
        }
        return base + "." + type.extension();
    }

    private static byte[] readAtMost(MultipartFile file, long limit) {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes((int) Math.min(Integer.MAX_VALUE - 8, limit + 1));
        } catch (IOException ex) {
            throw new InvalidRequestException("The upload could not be read.");
        }
    }
}
