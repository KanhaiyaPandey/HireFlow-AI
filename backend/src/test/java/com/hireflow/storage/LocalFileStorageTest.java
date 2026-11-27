package com.hireflow.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.hireflow.common.exception.NotFoundException;
import com.hireflow.support.TestFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTest {

    @TempDir
    Path dir;

    private LocalFileStorage storage() {
        return new LocalFileStorage(dir, "http://localhost:8080/media/",
                Clock.fixed(Instant.ofEpochMilli(1234), ZoneOffset.UTC));
    }

    @Test
    void privateFilesRoundTripAndCannotBeOverwritten() {
        LocalFileStorage storage = storage();
        byte[] pdf = TestFiles.pdfWithText("hello");

        storage.storePrivateFile("resumes/a.pdf", pdf, FileType.PDF);
        assertThat(storage.openPrivateFile("resumes/a.pdf", "cv.pdf"))
                .isInstanceOfSatisfying(PrivateFileAccess.Content.class, content -> {
                    assertThat(content.bytes()).isEqualTo(pdf);
                    assertThat(content.contentType()).isEqualTo("application/pdf");
                    assertThat(content.downloadName()).isEqualTo("cv.pdf");
                });
        assertThatThrownBy(() -> storage.storePrivateFile("resumes/a.pdf", pdf, FileType.PDF))
                .isInstanceOf(IllegalStateException.class);

        storage.deletePrivateFile("resumes/a.pdf");
        assertThatThrownBy(() -> storage.openPrivateFile("resumes/a.pdf", "cv.pdf"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void privateFilesAreOutsideThePublicFolder() {
        storage().storePrivateFile("resumes/b.pdf", TestFiles.pdfWithText("x"), FileType.PDF);

        assertThat(dir.resolve("private/resumes/b.pdf")).exists();
        assertThat(dir.resolve("public")).doesNotExist();
    }

    @Test
    void publicImageGetsAVersionedUrlAndReplacingItRemovesTheOldExtension() {
        LocalFileStorage storage = storage();

        String url = storage.storePublicImage("companies/1/logo", TestFiles.PNG, FileType.PNG);
        assertThat(url).isEqualTo("http://localhost:8080/media/companies/1/logo.png?v=1234");

        storage.storePublicImage("companies/1/logo", TestFiles.JPEG, FileType.JPEG);
        assertThat(dir.resolve("public/companies/1/logo.png")).doesNotExist();
        assertThat(dir.resolve("public/companies/1/logo.jpg")).exists();

        storage.deletePublicImage("companies/1/logo");
        assertThat(dir.resolve("public/companies/1/logo.jpg")).doesNotExist();
    }

    @Test
    void keysCannotEscapeTheStorageDirectory() throws Exception {
        Path secret = Files.writeString(dir.resolve("secret.txt"), "top secret");

        assertThatThrownBy(() -> storage().openPrivateFile("../secret.txt", "x"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage().storePrivateFile("../../escape.pdf", new byte[]{1}, FileType.PDF))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(secret).hasContent("top secret");
    }
}
