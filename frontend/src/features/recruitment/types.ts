/** Mirrors application.entity.ApplicationStatus */
export type ApplicationStatus = 'APPLIED' | 'SCREENING' | 'SHORTLISTED' | 'INTERVIEW_SCHEDULED' | 'SELECTED' | 'REJECTED'

export type ApplicationSource = 'CANDIDATE' | 'RECRUITER'

/** Mirrors application.dto.ApplicationResponse (staff view) */
export interface Application {
  id: string
  jobId: string
  jobTitle: string
  candidateId: string
  candidateName: string
  candidateEmail: string
  resumeId: string
  status: ApplicationStatus
  source: ApplicationSource
  appliedAt: string
  updatedAt: string
}

/** Mirrors application.dto.MyApplicationResponse (candidate view) */
export interface MyApplication {
  id: string
  jobId: string
  jobTitle: string
  companyName: string
  status: ApplicationStatus
  appliedAt: string
  updatedAt: string
}
