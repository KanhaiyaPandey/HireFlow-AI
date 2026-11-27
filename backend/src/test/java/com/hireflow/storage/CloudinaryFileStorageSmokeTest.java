package com.hireflow.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import com.cloudinary.Cloudinary;
import com.hireflow.support.TestFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Talks to a REAL Cloudinary account, so it only runs when CLOUDINARY_URL is exported in the shell:
 *
 * <pre>
 *   CLOUDINARY_URL="$(grep '^CLOUDINARY_URL=' ../.env | cut -d= -f2-)" \
 *     ./mvnw test -Dtest=CloudinaryFileStorageSmokeTest
 * </pre>
 *
 * Normal builds and CI skip it. Files go under "hireflow-test/" and are deleted at the end.
 */
@EnabledIfEnvironmentVariable(named = "CLOUDINARY_URL", matches = "cloudinary://.+")
class CloudinaryFileStorageSmokeTest {

    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private final CloudinaryFileStorage storage = new CloudinaryFileStorage(
            new Cloudinary(System.getenv("CLOUDINARY_URL")), "hireflow-test", Duration.ofMinutes(2), Clock.systemUTC());

    @Test
    void privateResumeIsOnlyReachableThroughTheSignedLink() throws Exception {
        String key = "resumes/" + UUID.randomUUID() + ".pdf";
        byte[] pdf = TestFiles.pdfWithText("Cloudinary smoke test");
        storage.storePrivateFile(key, pdf, FileType.PDF);
        try {
            PrivateFileAccess access = storage.openPrivateFile(key, "cv.pdf");
            assertThat(access).isInstanceOf(PrivateFileAccess.Redirect.class);
            URI signed = ((PrivateFileAccess.Redirect) access).url();

            HttpResponse<byte[]> download = http.send(HttpRequest.newBuilder(signed).build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            assertThat(download.statusCode()).isEqualTo(200);
            assertThat(download.body()).isEqualTo(pdf);

            URI tampered = URI.create(signed.toString().replaceAll("signature=[^&]+", "signature=forged"));
            assertThat(http.send(HttpRequest.newBuilder(tampered).build(), HttpResponse.BodyHandlers.discarding())
                    .statusCode()).isGreaterThanOrEqualTo(400);
        } finally {
            storage.deletePrivateFile(key);
        }
    }

    /**
     * Found on Day 6: re-uploading identical bytes keeps the same version (and URL), which is fine
     * since the image is unchanged. A different image gets a new version, so browsers and the CDN
     * fetch it instead of a cached copy.
     */
    @Test
    void publicImageIsReachableAndANewImageGetsANewUrl() throws Exception {
        String key = "companies/" + UUID.randomUUID() + "/logo";
        try {
            String first = storage.storePublicImage(key, TestFiles.png(0x3366FF), FileType.PNG);
            assertThat(http.send(HttpRequest.newBuilder(URI.create(first)).build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(200);

            Thread.sleep(1100); // Cloudinary versions are timestamps in seconds
            String second = storage.storePublicImage(key, TestFiles.png(0xFF6633), FileType.PNG);
            assertThat(second).isNotEqualTo(first);
            assertThat(http.send(HttpRequest.newBuilder(URI.create(second)).build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(200);
        } finally {
            storage.deletePublicImage(key);
        }
    }
}
