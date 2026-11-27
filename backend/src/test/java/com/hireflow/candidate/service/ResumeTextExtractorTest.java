package com.hireflow.candidate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hireflow.candidate.entity.ResumeTextStatus;
import com.hireflow.common.exception.BusinessRuleException;
import com.hireflow.support.TestFiles;
import org.junit.jupiter.api.Test;

class ResumeTextExtractorTest {

    private final ResumeTextExtractor extractor = new ResumeTextExtractor();

    @Test
    void extractsTextFromAllLines() {
        ResumeTextExtractor.Extraction result = extractor.extract(
                TestFiles.pdfWithText("Asha Verma", "Java, Spring Boot, PostgreSQL", "3 years at Acme"));

        assertThat(result.status()).isEqualTo(ResumeTextStatus.EXTRACTED);
        assertThat(result.text()).contains("Asha Verma", "Spring Boot", "3 years at Acme");
    }

    @Test
    void pdfWithoutATextLayerIsStoredAsEmpty() {
        ResumeTextExtractor.Extraction result = extractor.extract(TestFiles.blankPdf());

        assertThat(result.status()).isEqualTo(ResumeTextStatus.EMPTY);
        assertThat(result.text()).isNull();
    }

    @Test
    void passwordProtectedPdfIsRefused() {
        assertThatThrownBy(() -> extractor.extract(TestFiles.passwordProtectedPdf()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Password-protected");
    }

    @Test
    void corruptPdfIsRefused() {
        assertThatThrownBy(() -> extractor.extract(TestFiles.text("%PDF-1.7 this is not really a pdf")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("This file is not a readable PDF.");
    }

    @Test
    void tooManyPagesIsRefused() {
        assertThat(extractor.extract(TestFiles.pdfWithPages(ResumeTextExtractor.MAX_PAGES)).status())
                .isEqualTo(ResumeTextStatus.EXTRACTED);
        assertThatThrownBy(() -> extractor.extract(TestFiles.pdfWithPages(ResumeTextExtractor.MAX_PAGES + 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at most 20 pages");
    }
}
