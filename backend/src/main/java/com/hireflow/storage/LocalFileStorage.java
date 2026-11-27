package com.hireflow.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;

import com.hireflow.common.exception.NotFoundException;

/**
 * Files on local disk, for development and tests.
 *
 * <pre>
 * {dir}/public/companies/{id}/logo.png   served at {publicBaseUrl}/companies/{id}/logo.png
 * {dir}/private/resumes/{uuid}.pdf       only readable through the API
 * </pre>
 */
public class LocalFileStorage implements FileStorage {

    private final Path publicRoot;
    private final Path privateRoot;
    private final String publicBaseUrl;
    private final Clock clock;

    public LocalFileStorage(Path dir, String publicBaseUrl, Clock clock) {
        Path root = dir.toAbsolutePath().normalize();
        this.publicRoot = root.resolve("public");
        this.privateRoot = root.resolve("private");
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
        this.clock = clock;
    }

    public Path publicRoot() {
        return publicRoot;
    }

    @Override
    public String storePublicImage(String key, byte[] bytes, FileType type) {
        deletePublicImage(key); // a new logo may have a different extension than the old one
        String fileKey = key + "." + type.extension();
        write(resolve(publicRoot, fileKey), bytes, false);
        // The version parameter changes on every upload, so browsers fetch the new image.
        return publicBaseUrl + "/" + fileKey + "?v=" + clock.millis();
    }

    @Override
    public void deletePublicImage(String key) {
        Path target = resolve(publicRoot, key);
        Path parent = target.getParent();
        if (!Files.isDirectory(parent)) {
            return;
        }
        try (DirectoryStream<Path> siblings = Files.newDirectoryStream(parent, target.getFileName() + ".*")) {
            for (Path sibling : siblings) {
                Files.deleteIfExists(sibling);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void storePrivateFile(String key, byte[] bytes, FileType type) {
        write(resolve(privateRoot, key), bytes, true);
    }

    @Override
    public PrivateFileAccess openPrivateFile(String key, String downloadName) {
        Path file = resolve(privateRoot, key);
        try {
            String contentType = key.endsWith(".pdf") ? FileType.PDF.contentType() : "application/octet-stream";
            return new PrivateFileAccess.Content(Files.readAllBytes(file), contentType, downloadName);
        } catch (java.nio.file.NoSuchFileException ex) {
            throw new NotFoundException("The file is no longer available");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void deletePrivateFile(String key) {
        try {
            Files.deleteIfExists(resolve(privateRoot, key));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /** Refuses any key that would escape the storage directory (e.g. "../../etc/passwd"). */
    private static Path resolve(Path root, String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new IllegalArgumentException("Invalid storage key: " + key);
        }
        return target;
    }

    private static void write(Path target, byte[] bytes, boolean failIfExists) {
        try {
            Files.createDirectories(target.getParent());
            if (failIfExists) {
                Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            } else {
                Files.write(target, bytes);
            }
        } catch (FileAlreadyExistsException ex) {
            throw new IllegalStateException("Storage key already exists: " + target.getFileName(), ex);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
