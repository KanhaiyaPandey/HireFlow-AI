package com.hireflow.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.mock.web.MockMultipartFile;

/** Upload fixtures built in code, so the repository holds no binary test files. */
public final class TestFiles {

    /** A valid 1×1 transparent PNG. */
    public static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    /** JPEG magic bytes followed by filler: enough for type detection, which is all images get. */
    public static final byte[] JPEG = concat(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, new byte[64]);

    private TestFiles() {
    }

    /** A real, decodable PNG of one colour, so different colours give genuinely different images. */
    public static byte[] png(int rgb) {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                image.setRGB(x, y, rgb);
            }
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public static byte[] pdfWithText(String... lines) {
        return pdf(1, lines, false);
    }

    /** A PDF with a page but no text layer, like a scanned image. */
    public static byte[] blankPdf() {
        return pdf(1, new String[0], false);
    }

    public static byte[] passwordProtectedPdf() {
        return pdf(1, new String[]{"secret"}, true);
    }

    public static byte[] pdfWithPages(int pages) {
        return pdf(pages, new String[]{"page"}, false);
    }

    public static MockMultipartFile upload(byte[] content, String fileName, String contentType) {
        return new MockMultipartFile("file", fileName, contentType, content);
    }

    public static MockMultipartFile resume(byte[] pdf) {
        return upload(pdf, "My Resume.pdf", "application/pdf");
    }

    public static byte[] text(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static byte[] pdf(int pages, String[] lines, boolean encrypted) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (lines.length > 0) {
                    try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                        stream.beginText();
                        stream.setFont(font, 12);
                        stream.newLineAtOffset(50, 700);
                        for (String line : lines) {
                            stream.showText(line);
                            stream.newLineAtOffset(0, -16);
                        }
                        stream.endText();
                    }
                }
            }
            if (encrypted) {
                StandardProtectionPolicy policy = new StandardProtectionPolicy("owner-pw", "user-pw", new AccessPermission());
                policy.setEncryptionKeyLength(128);
                document.protect(policy);
            }
            document.save(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
