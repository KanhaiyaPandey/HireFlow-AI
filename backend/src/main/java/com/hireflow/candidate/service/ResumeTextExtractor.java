package com.hireflow.candidate.service;

import java.io.IOException;

import com.hireflow.candidate.entity.ResumeTextStatus;
import com.hireflow.common.exception.BusinessRuleException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/**
 * Pulls plain text out of a PDF with Apache PDFBox, from the uploaded bytes (never from storage).
 * AI screening and semantic search work on this text.
 */
@Component
public class ResumeTextExtractor {

    /** Resumes are short. A limit also bounds the work for a malicious "PDF bomb". */
    static final int MAX_PAGES = 20;
    /** Plenty for any real resume; keeps the column and later LLM prompts bounded. */
    static final int MAX_CHARS = 100_000;

    public record Extraction(String text, ResumeTextStatus status) {
    }

    /**
     * @throws BusinessRuleException for files we refuse to store: password-protected, unreadable,
     *                               or too many pages
     */
    public Extraction extract(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            if (document.getNumberOfPages() > MAX_PAGES) {
                throw new BusinessRuleException("Resumes can be at most %d pages.".formatted(MAX_PAGES));
            }
            String text = new PDFTextStripper().getText(document).strip();
            if (text.isEmpty()) {
                // A scanned image without a text layer. Stored, but AI screening can't read it (no OCR).
                return new Extraction(null, ResumeTextStatus.EMPTY);
            }
            return new Extraction(text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text,
                    ResumeTextStatus.EXTRACTED);
        } catch (InvalidPasswordException ex) {
            throw new BusinessRuleException("Password-protected PDFs can't be read. Upload an unprotected copy.");
        } catch (IOException ex) {
            throw new BusinessRuleException("This file is not a readable PDF.");
        }
    }
}
