import type { ApplicationSource, ApplicationStatus } from '@/features/recruitment/types'

/** Mirrors dashboard.dto.ActivityItem */
export interface ActivityItem {
  applicationId: string
  candidateName: string
  jobTitle: string
  source: ApplicationSource
  /** null: the application was submitted (no previous status). */
  fromStatus: ApplicationStatus | null
  toStatus: ApplicationStatus
  changedByName: string | null
  at: string
}

/** Mirrors dashboard.dto.DashboardOverview */
export interface DashboardOverview {
  activeJobs: number
  draftJobs: number
  candidates: number
  pendingApplications: number
  totalApplications: number
  hires: number
  /** null for recruiters. */
  activeEmployees: number | null
  departments: number | null
  recentActivity: ActivityItem[]
}

/** Mirrors dashboard.dto.RecruitmentAnalytics */
export interface RecruitmentAnalytics {
  days: number
  zone: string
  totalApplications: number
  applicationsByDay: { date: string; count: number }[]
  pipeline: { status: ApplicationStatus; count: number }[]
  sources: { candidate: number; recruiter: number }
  topJobs: { jobId: string; title: string; status: string; applications: number; advanced: number }[]
}

/** Mirrors dashboard.dto.EmployeeAnalytics */
export interface EmployeeAnalytics {
  total: number
  active: number
  inactive: number
  byDepartment: { departmentId: string | null; name: string; count: number }[]
  joinersByMonth: { month: string; count: number }[]
}

export type RangeDays = 7 | 30 | 90
