package com.hireflow.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;

import com.hireflow.common.exception.FileTooLargeException;
import com.hireflow.common.exception.InvalidRequestException;
import com.hireflow.support.TestFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

class UploadsTest {

    @Test
    void detectsTypesByContentNotName() {
        assertThat(FileType.detect(TestFiles.pdfWithText("hi"))).contains(FileType.PDF);
        assertThat(FileType.detect(TestFiles.PNG)).contains(FileType.PNG);
        assertThat(FileType.detect(TestFiles.JPEG)).contains(FileType.JPEG);
        assertThat(FileType.detect(TestFiles.concat(TestFiles.text("RIFF\0\0\0\0WEBPVP8 "), new byte[16])))
                .contains(FileType.WEBP);
        assertThat(FileType.detect(TestFiles.concat(TestFiles.text("RIFF\0\0\0\0WAVE"), new byte[16]))).isEmpty();
        assertThat(FileType.detect(TestFiles.text("just text"))).isEmpty();
        assertThat(FileType.detect(new byte[0])).isEmpty();
    }

    @Test
    void renamedTextFileIsRejectedEvenWithPdfNameAndHeader() {
        MockMultipartFile fake = new MockMultipartFile("file", "cv.pdf", "application/pdf", TestFiles.text("hello"));

        assertThatThrownBy(() -> Uploads.validate(fake, EnumSet.of(FileType.PDF), Uploads.MAX_RESUME))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Unsupported file. Allowed: pdf.");
    }

    @Test
    void oversizedFileIsRejected() {
        MockMultipartFile big = new MockMultipartFile("file", "big.png", "image/png",
                TestFiles.concat(TestFiles.PNG, new byte[1024]));

        assertThatThrownBy(() -> Uploads.validate(big, FileType.IMAGES, DataSize.ofBytes(1000)))
                .isInstanceOf(FileTooLargeException.class);
    }

    @Test
    void emptyUploadIsRejected() {
        assertThatThrownBy(() -> Uploads.validate(new MockMultipartFile("file", new byte[0]),
                FileType.IMAGES, Uploads.MAX_IMAGE))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Choose a file to upload.");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "My Resume.pdf               | My Resume.pdf",
            "../../etc/passwd.pdf        | passwd.pdf",
            "C:\\Users\\me\\cv.PDF       | cv.pdf",
            "evil.exe                    | evil.pdf",
            "no-extension                | no-extension.pdf",
            "\"<script>\".pdf            | script.pdf",
            ".pdf                        | file.pdf",
    })
    void fileNamesAreCleanedAndGetTheRealExtension(String original, String expected) {
        assertThat(Uploads.safeFileName(original, FileType.PDF)).isEqualTo(expected);
    }

    @Test
    void missingNameGetsADefault() {
        assertThat(Uploads.safeFileName(null, FileType.PNG)).isEqualTo("file.png");
    }
}
