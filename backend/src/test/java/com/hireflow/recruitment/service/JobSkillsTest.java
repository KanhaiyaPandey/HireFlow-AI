package com.hireflow.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import com.hireflow.recruitment.entity.JobStatus;
import org.junit.jupiter.api.Test;

class JobSkillsTest {

    @Test
    void skillsAreTrimmedDeduplicatedIgnoringCaseAndKeepTheirFirstSpelling() {
        assertThat(JobService.cleanSkills(Arrays.asList(" Java ", "java", "Spring Boot", "", "  ", null, "JAVA", "React")))
                .containsExactly("Java", "Spring Boot", "React");
    }

    @Test
    void missingSkillsBecomeAnEmptyList() {
        assertThat(JobService.cleanSkills(null)).isEmpty();
    }

    @Test
    void jobStatusTransitions() {
        assertThat(JobStatus.DRAFT.canTransitionTo(JobStatus.PUBLISHED)).isTrue();
        assertThat(JobStatus.DRAFT.canTransitionTo(JobStatus.CLOSED)).isTrue();
        assertThat(JobStatus.PUBLISHED.canTransitionTo(JobStatus.CLOSED)).isTrue();
        assertThat(JobStatus.CLOSED.canTransitionTo(JobStatus.PUBLISHED)).isTrue();

        assertThat(JobStatus.PUBLISHED.canTransitionTo(JobStatus.DRAFT)).isFalse();
        assertThat(JobStatus.CLOSED.canTransitionTo(JobStatus.DRAFT)).isFalse();
        assertThat(JobStatus.PUBLISHED.canTransitionTo(JobStatus.PUBLISHED)).isFalse();
    }
}
