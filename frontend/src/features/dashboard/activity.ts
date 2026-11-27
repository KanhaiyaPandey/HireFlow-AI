import type { ActivityItem } from './types'

/** One activity line, split so the page can emphasise the people and the job. */
export type ActivityPart = { text: string; strong?: boolean }

/**
 * Turns a pipeline event into a sentence:
 * - "Ananya Iyer applied for Frontend Engineer"
 * - "Hema Rao added Ravi Kumar to Java Developer"
 * - "Hema Rao updated Ananya Iyer" (the page shows the new status as a badge beside the job)
 */
export function describeActivity(item: ActivityItem): ActivityPart[] {
  const actor = item.changedByName ?? 'Someone'
  if (item.fromStatus === null) {
    return item.source === 'RECRUITER'
      ? [{ text: actor, strong: true }, { text: ' added ' }, { text: item.candidateName, strong: true }, { text: ' to ' }, { text: item.jobTitle, strong: true }]
      : [{ text: item.candidateName, strong: true }, { text: ' applied for ' }, { text: item.jobTitle, strong: true }]
  }
  return [{ text: actor, strong: true }, { text: ' updated ' }, { text: item.candidateName, strong: true }]
}

/** Status changes show the new status as a badge; submissions don't. */
export function showsStatus(item: ActivityItem): boolean {
  return item.fromStatus !== null
}
