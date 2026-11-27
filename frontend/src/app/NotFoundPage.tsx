import { Compass } from 'lucide-react'
import { Link } from 'react-router'

import { BrandLogo } from '@/components/BrandLogo'
import { EmptyState } from '@/components/EmptyState'
import { Button } from '@/components/ui/button'

/** Unknown routes. Inside a layout it keeps the shell; `standalone` is the bare, signed-out version. */
export function NotFoundPage({ standalone = false }: { standalone?: boolean }) {
  const content = (
    <EmptyState
      icon={Compass}
      title="This page doesn't exist"
      description="The link may be old, or the page may have moved. Head back to your home page to carry on."
      action={
        <Button asChild>
          <Link to="/">Go to my home page</Link>
        </Button>
      }
    />
  )
  if (!standalone) {
    return <div className="rounded-xl border border-border bg-card">{content}</div>
  }
  return (
    <div className="grid min-h-dvh place-items-center bg-background p-6">
      <div className="w-full max-w-md">
        <div className="mb-6 flex justify-center">
          <BrandLogo />
        </div>
        <div className="rounded-xl border border-border bg-card">{content}</div>
      </div>
    </div>
  )
}
