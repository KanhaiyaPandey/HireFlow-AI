package com.hireflow.application.entity;

import static com.hireflow.application.entity.ApplicationStatus.APPLIED;
import static com.hireflow.application.entity.ApplicationStatus.INTERVIEW_SCHEDULED;
import static com.hireflow.application.entity.ApplicationStatus.REJECTED;
import static com.hireflow.application.entity.ApplicationStatus.SCREENING;
import static com.hireflow.application.entity.ApplicationStatus.SELECTED;
import static com.hireflow.application.entity.ApplicationStatus.SHORTLISTED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Checks every one of the 36 (from, to) pairs against the state diagram in docs/architecture.md §9. */
class ApplicationStatusTest {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> DIAGRAM = Map.of(
            APPLIED, EnumSet.of(SCREENING, SHORTLISTED, REJECTED),
            SCREENING, EnumSet.of(SHORTLISTED, REJECTED),
            SHORTLISTED, EnumSet.of(INTERVIEW_SCHEDULED, REJECTED),
            INTERVIEW_SCHEDULED, EnumSet.of(SELECTED, REJECTED, SHORTLISTED),
            SELECTED, EnumSet.noneOf(ApplicationStatus.class),
            REJECTED, EnumSet.noneOf(ApplicationStatus.class));

    @ParameterizedTest
    @EnumSource(ApplicationStatus.class)
    void transitionsMatchTheStateDiagram(ApplicationStatus from) {
        for (ApplicationStatus to : ApplicationStatus.values()) {
            assertThat(from.canTransitionTo(to))
                    .as("%s -> %s", from, to)
                    .isEqualTo(DIAGRAM.get(from).contains(to));
        }
    }

    @Test
    void onlySelectedAndRejectedAreFinal() {
        assertThat(EnumSet.allOf(ApplicationStatus.class).stream().filter(ApplicationStatus::isFinal))
                .containsExactlyInAnyOrder(SELECTED, REJECTED);
    }

    @Test
    void noStatusCanMoveToItselfOrBackToApplied() {
        for (ApplicationStatus status : ApplicationStatus.values()) {
            assertThat(status.canTransitionTo(status)).as(status.name()).isFalse();
            assertThat(status.canTransitionTo(APPLIED)).as(status.name()).isFalse();
        }
    }
}
