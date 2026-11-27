import { BriefcaseBusiness, Clock3, Inbox, Trophy, Users, UsersRound } from 'lucide-react'
import { useState } from 'react'

import { ChartCard } from '@/components/ChartCard'
import { AreaTrend, ColumnBars, HorizontalBars } from '@/components/charts/Charts'
import { DataTable, type Column } from '@/components/DataTable'
import { EmptyState } from '@/components/EmptyState'
import { PageHeader } from '@/components/PageHeader'
import { SegmentedControl } from '@/components/SegmentedControl'
import { StatCard } from '@/components/StatCard'
import { StatusBadge } from '@/components/StatusBadge'
import { useCurrentUser } from '@/features/auth/api'
import { formatDayLong, formatDayTick, formatMonthLong, formatMonthTick } from '@/lib/chart'
import { formatNumber, humanise } from '@/lib/format'

import { useDashboardOverview, useEmployeeAnalytics, useRecruitmentAnalytics } from '../api'
import { RecentActivity } from '../components/RecentActivity'
import type { RangeDays, RecruitmentAnalytics } from '../types'

const RANGES: { value: `${RangeDays}`; label: string }[] = [
  { value: '7', label: 'Last 7 days' },
  { value: '30', label: 'Last 30 days' },
  { value: '90', label: 'Last 90 days' },
]

type TopJob = RecruitmentAnalytics['topJobs'][number]

const TOP_JOB_COLUMNS: Column<TopJob>[] = [
  {
    id: 'job',
    header: 'Job',
    className: 'w-full max-w-0',
    cell: (job) => (
      <div className="flex min-w-0 items-center gap-2.5">
        <span className="truncate font-medium">{job.title}</span>
        <StatusBadge status={job.status} className="hidden sm:inline-flex" />
      </div>
    ),
  },
  { id: 'applications', header: 'Applications', align: 'right', cell: (job) => formatNumber(job.applications) },
  {
    id: 'advanced',
    header: 'Shortlisted or later',
    align: 'right',
    hideBelow: 'sm',
    cell: (job) => formatNumber(job.advanced),
  },
]

/** The staff home for HR admins and recruiters (Day 8 dashboard). */
export default function OverviewPage() {
  const user = useCurrentUser()
  const isHrAdmin = user.role === 'HR_ADMIN'
  const [range, setRange] = useState<`${RangeDays}`>('30')
  const days = Number(range) as RangeDays

  const overview = useDashboardOverview()
  const recruitment = useRecruitmentAnalytics(days)
  const team = useEmployeeAnalytics(isHrAdmin)

  const stats = overview.data
  const analytics = recruitment.data
  const rangeText = `the last ${days} days`
  const noApplications = analytics?.totalApplications === 0

  return (
    <>
      <PageHeader title={`Welcome back, ${user.name.split(' ')[0]}`} description={isHrAdmin ? "Here's where hiring and your team stand today." : "Here's where hiring stands today."}
      />

      {/* Headline numbers: current state, not affected by the range below. */}
      <section aria-label="Key numbers" className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
        {isHrAdmin && (
          <StatCard
            label="Active employees"
            icon={Users}
            value={stats?.activeEmployees ?? undefined}
            loading={overview.isPending}
            error={overview.isError}
            hint={stats && `Across ${formatNumber(stats.departments ?? 0)} departments`}
          />
        )}
        <StatCard
          label="Open jobs"
          icon={BriefcaseBusiness}
          value={stats?.activeJobs}
          loading={overview.isPending}
          error={overview.isError}
          hint={stats && `${formatNumber(stats.draftJobs)} more in draft`}
        />
        <StatCard
          label="Candidates"
          icon={UsersRound}
          value={stats?.candidates}
          loading={overview.isPending}
          error={overview.isError}
          hint="People who applied to your jobs"
        />
        <StatCard
          label="Awaiting review"
          icon={Clock3}
          value={stats?.pendingApplications}
          loading={overview.isPending}
          error={overview.isError}
          hint="Applied or in screening"
        />
        {!isHrAdmin && (
          <StatCard
            label="Hires"
            icon={Trophy}
            value={stats?.hires}
            loading={overview.isPending}
            error={overview.isError}
            hint="Applications marked selected"
          />
        )}
      </section>

      {/* Recruitment: everything in this section is scoped by the one range control above it. */}
      <section aria-labelledby="recruitment-heading" className="mt-9">
        <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h2 id="recruitment-heading" className="text-lg font-semibold tracking-[-0.01em]">
              Recruitment
            </h2>
            <p className="mt-0.5 text-sm text-muted-foreground">Applications received in {rangeText}.</p>
          </div>
          <SegmentedControl label="Date range" options={RANGES} value={range} onChange={setRange} size="sm" />
        </div>

        <div className="grid gap-4 lg:grid-cols-3">
          <ChartCard
            className="lg:col-span-2"
            title="Applications per day"
            description={analytics && `${formatNumber(analytics.totalApplications)} received in ${rangeText}`}
            loading={recruitment.isPending}
            error={recruitment.error}
            onRetry={() => recruitment.refetch()}
            refreshing={recruitment.isPlaceholderData}
            isEmpty={noApplications}
            emptyMessage={`No applications in ${rangeText}. They'll appear here as candidates apply.`}
            table={
              analytics && {
                headers: ['Day', 'Applications'],
                rows: analytics.applicationsByDay.map((d) => [formatDayLong(d.date), d.count]),
              }
            }
          >
            {analytics && (
              <AreaTrend
                label={`Applications per day, ${rangeText}`}
                data={analytics.applicationsByDay.map((d) => ({ key: d.date, value: d.count }))}
                unit="applications"
                formatTick={formatDayTick}
                formatKey={formatDayLong}
              />
            )}
          </ChartCard>

          <ChartCard
            title="Where they are now"
            description="Those applications by current stage"
            loading={recruitment.isPending}
            error={recruitment.error}
            onRetry={() => recruitment.refetch()}
            refreshing={recruitment.isPlaceholderData}
            isEmpty={noApplications}
            emptyMessage="The pipeline fills up as applications arrive."
            table={
              analytics && {
                headers: ['Stage', 'Applications'],
                rows: analytics.pipeline.map((p) => [humanise(p.status), p.count]),
              }
            }
            footer={
              analytics && (
                <>
                  <span className="font-medium text-foreground">{formatNumber(analytics.sources.candidate)}</span> applied
                  directly · <span className="font-medium text-foreground">{formatNumber(analytics.sources.recruiter)}</span>{' '}
                  added by recruiters
                </>
              )
            }
          >
            {analytics && (
              <HorizontalBars
                label={`Applications by current stage, ${rangeText}`}
                data={analytics.pipeline.map((p) => ({ key: p.status, value: p.count }))}
                unit="applications"
                formatKey={humanise}
              />
            )}
          </ChartCard>

          <section aria-labelledby="top-jobs" className="overflow-hidden rounded-xl border border-border bg-card lg:col-span-3">
            <header className="px-5 pt-5 pb-4">
              <h3 id="top-jobs" className="text-[15px] font-semibold">
                Most active jobs
              </h3>
              <p className="mt-0.5 text-sm text-muted-foreground">Jobs with the most applications in {rangeText}.</p>
            </header>
            <div className={recruitment.isPlaceholderData ? 'opacity-50 transition-opacity' : 'transition-opacity'}>
              <DataTable
                label="Most active jobs"
                columns={TOP_JOB_COLUMNS}
                rows={analytics?.topJobs}
                rowKey={(job) => job.jobId}
                loading={recruitment.isPending}
                error={recruitment.error}
                onRetry={() => recruitment.refetch()}
                skeletonRows={3}
                empty={
                  <EmptyState
                    icon={Inbox}
                    title={`No applications in ${rangeText}`}
                    description="Publish a job, or add a candidate from a resume, and the busiest jobs will rank here."
                  />
                }
              />
            </div>
          </section>
        </div>
      </section>

      {/* Activity and team: not range-scoped. */}
      <section aria-label="Activity and team" className="mt-9 grid gap-4 lg:grid-cols-3">
        <RecentActivity
          className={isHrAdmin ? 'lg:row-span-2' : 'lg:col-span-3'}
          items={stats?.recentActivity}
          loading={overview.isPending}
          error={overview.error}
          onRetry={() => overview.refetch()}
        />
        {isHrAdmin && (
          <>
            <ChartCard
              className="lg:col-span-2"
              title="People by department"
              description={team.data && `${formatNumber(team.data.active)} active employees`}
              loading={team.isPending}
              error={team.error}
              onRetry={() => team.refetch()}
              isEmpty={team.data?.byDepartment.length === 0}
              emptyMessage="Add departments and employees to see how your team is spread."
              height={Math.max(160, Math.min(8, team.data?.byDepartment.length ?? 4) * 40)}
              table={
                team.data && {
                  headers: ['Department', 'Active employees'],
                  rows: team.data.byDepartment.map((d) => [d.name, d.count]),
                }
              }
            >
              {team.data && (
                <HorizontalBars
                  label="Active employees by department"
                  data={team.data.byDepartment.slice(0, 8).map((d) => ({ key: d.name, value: d.count }))}
                  unit="employees"
                  labelWidth={140}
                />
              )}
            </ChartCard>
            <ChartCard
              className="lg:col-span-2"
              title="Joiners per month"
              description="Employees by joining date, last 12 months"
              loading={team.isPending}
              error={team.error}
              onRetry={() => team.refetch()}
              isEmpty={team.data?.joinersByMonth.every((m) => m.count === 0)}
              emptyMessage="No one has joined in the last 12 months. Joining dates come from employee records."
              height={200}
              table={
                team.data && {
                  headers: ['Month', 'Joiners'],
                  rows: team.data.joinersByMonth.map((m) => [formatMonthLong(m.month), m.count]),
                }
              }
            >
              {team.data && (
                <ColumnBars
                  label="Joiners per month, last 12 months"
                  data={team.data.joinersByMonth.map((m) => ({ key: m.month, value: m.count }))}
                  unit="joined"
                  formatTick={formatMonthTick}
                  formatKey={formatMonthLong}
                />
              )}
            </ChartCard>
          </>
        )}
      </section>
    </>
  )
}
