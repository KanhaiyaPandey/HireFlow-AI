package com.hireflow.dashboard.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Recruitment numbers for applications received in the last {@code days} days (in {@code zone}).
 * Every part is scoped by the same window, so the numbers on the page always agree.
 *
 * @param applicationsByDay one entry per calendar day in the window, oldest first, zero-filled
 * @param pipeline          those applications by their CURRENT status, in pipeline order, zero-filled
 * @param topJobs           up to five jobs with the most applications in the window
 */
public record RecruitmentAnalytics(
        int days,
        String zone,
        long totalApplications,
        List<DayCount> applicationsByDay,
        List<StatusCount> pipeline,
        SourceSplit sources,
        List<JobActivity> topJobs) {

    public record DayCount(LocalDate date, long count) {
    }

    public record StatusCount(String status, long count) {
    }

    public record SourceSplit(long candidate, long recruiter) {
    }

    /** @param advanced applications that reached SHORTLISTED, INTERVIEW_SCHEDULED or SELECTED */
    public record JobActivity(UUID jobId, String title, String status, long applications, long advanced) {
    }
}
