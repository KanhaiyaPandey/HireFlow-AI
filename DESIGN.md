# HireFlow AI — Design System

**Binding for every frontend page.** The rules in "Mandatory design theme", "Layout system", "Component guidelines" and "Important rules" were set by the project owner and are recorded verbatim in substance. Change them only when the owner asks. "Implementation" below maps them to code, so every page uses the same tokens and components.

Before implementing any new page, inspect the existing shared components in `frontend/src/components` and reuse them.

---

## Mandatory design theme

### Brand

| Token | Value | Use |
|---|---|---|
| Primary | `#4338CA` | Primary actions, active navigation, focus |
| Primary hover | `#3730A3` | Hover and pressed state of primary actions |
| Accent | `#6366F1` | Secondary emphasis, selected states |
| Accent light | `#EEF2FF` | Tinted backgrounds for selected and active items |

### Backgrounds

| Token | Value |
|---|---|
| Main background | `#F8FAFC` |
| Card background | `#FFFFFF` |
| Muted background | `#F1F5F9` |

### Typography

- **Font family:** Inter
- **Primary text:** `#0F172A`
- **Secondary text:** `#64748B`
- **Muted text:** `#94A3B8`

### Borders

- **Default border:** `#E2E8F0`
- **Strong border:** `#CBD5E1`

### Semantic colours

| Token | Value |
|---|---|
| Success | `#16A34A` |
| Warning | `#D97706` |
| Danger | `#DC2626` |
| Info | `#0284C7` |

### AI accent (violet)

The guidelines require violet accents for AI features but give no hex value. One violet is defined for that purpose only, and nothing else uses it:

| Token | Value | Use |
|---|---|---|
| AI | `#7C3AED` | Icons, labels and outlines of AI features (screening, semantic search, question generation) |
| AI light | `#F5F3FF` | Background of AI panels and badges |

## Layout system

| Element | Value |
|---|---|
| Sidebar width | 248px |
| Header height | 64px |
| Main content padding | 28px |
| Card border radius | 12px |
| Button border radius | 8px |
| Input height | 42px |
| Card padding | 20–24px |

Use a responsive grid system.

## Component guidelines

1. White cards with subtle slate borders.
2. Indigo for primary actions and active navigation.
3. Violet accents for AI-related features.
4. Consistent status badge colours (see "Status semantics").
5. Lucide icons throughout.
6. Consistent typography and spacing.
7. No unnecessary gradients or heavy shadows.
8. Subtle hover and transition effects.
9. Skeleton loaders for loading states.
10. Empty states and error states on every data view.
11. Accessible and responsive.

## Application shell

These shared components are reused everywhere and never duplicated:

`AppSidebar`, `AppHeader`, `MainLayout`, `PageHeader`, `StatCard`, `DataTable`, `StatusBadge`, `SearchInput`, `FilterDropdown`, `FormField`, `ConfirmDialog`, `EmptyState`, `LoadingSkeleton`

## Important rules

- Do not invent new colour palettes for individual pages.
- Do not duplicate layout components.
- Reuse shared UI components.
- Maintain consistent visual hierarchy.
- All HR admin and recruiter pages use the same application shell.
- Candidate pages may use a separate layout but keep the same brand identity.
- Keep the design professional, minimal, clean and enterprise-focused.

---

## Status semantics

Every status in the product has exactly one badge style. The badge always shows its text, so colour is never the only signal.

| Status | Tone |
|---|---|
| Application `APPLIED` | info |
| Application `SCREENING` | accent |
| Application `SHORTLISTED` | primary |
| Application `INTERVIEW_SCHEDULED` | warning |
| Application `SELECTED` | success |
| Application `REJECTED` | danger |
| Job `DRAFT` | neutral |
| Job `PUBLISHED` | success |
| Job `CLOSED` | neutral (strong) |
| Company / employment `ACTIVE` | success |
| Company / employment `INACTIVE` | neutral (strong) |
| Resume text `EXTRACTED` | success |
| Resume text `EMPTY` | warning |
| Resume text `FAILED` | danger |

The mapping lives in `frontend/src/components/StatusBadge.tsx`; add new statuses there, never inline.

## Implementation

How the rules above live in `frontend/` (recorded from the built code, Day 7).

### Tokens: `src/index.css`

All colours are CSS variables in `:root`, exposed to Tailwind through `@theme inline`. Use the utility, never a hex value in a component.

| Need | Utility |
|---|---|
| Primary action / hover | `bg-primary` / `hover:bg-primary-hover` |
| Accent text, accent-light background | `text-brand-accent`, `bg-accent` |
| Page / card / muted background | `bg-background`, `bg-card`, `bg-muted` |
| Primary / secondary / muted text | `text-foreground`, `text-muted-foreground`, `text-subtle` |
| Default / strong border | `border-border`, `border-border-strong` (inputs use `border-input` = strong) |
| Semantic | `text-success` + `bg-success-soft`, likewise `warning`, `danger`, `info` |
| AI features only | `text-ai`, `bg-ai-soft` |
| Layout sizes | `w-sidebar` / `pl-sidebar` (248px), `h-header` (64px), `p-page` (28px), `h-control` / `size-control` (42px) |
| Radii | `rounded-lg` = 8px (buttons, inputs), `rounded-xl` = 12px (cards) |

- **Font:** Inter Variable, self-hosted via `@fontsource-variable/inter`, with features `cv11` and `ss03` for data-heavy UI. Use `tabular-nums` for numbers that line up.
- **Browser surfaces:** text selection, the caret, scrollbars and `:focus-visible` outlines are themed from the palette. Motion is turned off under `prefers-reduced-motion`.

### Primitives: `src/components/ui` (shadcn/ui, Radix)

These are generated, then adjusted to the system:
- **Button:** 42px default height, 8px radius, `primary-hover` on hover, and an outline variant with the strong border.
- **Input:** 42px, white, strong border, indigo focus ring.
- **Card:** a 1px slate border and no shadow, with 24px padding (20px for the `sm` size).

Keep those changes when re-running `shadcn add --overwrite`.

### Shared components: `src/components`

| Component | Purpose |
|---|---|
| `MainLayout`, `AppSidebar`, `AppHeader`, `UserMenu` | The one staff shell. Navigation per role lives in `src/app/navigation.ts`: flip `ready: true` as each page ships, and AI items set `ai: true` for the violet icon |
| `CandidateLayout` | Top-navigation layout for candidates, same brand |
| `PageHeader` | One `h1`, an optional description, and right-aligned page actions |
| `StatCard` | One real number with an icon chip, hint and loading/error states; `accent="ai"` for AI-derived numbers |
| `DataTable` | Typed `Column<T>[]`; server-side sort (`aria-sort`) and pagination; skeleton rows; `ErrorState` on failure; only the `empty` node when there are no rows; `hideBelow` keeps phone views readable |
| `StatusBadge` | Text + dot + tone from `src/lib/status.ts` |
| `SearchInput`, `FilterDropdown` | Debounced search; single-choice filter that shows its current value |
| `FormField`, `PasswordInput`, `FormAlert` | Label, control, hint and error wired with `aria-invalid` / `aria-describedby`; show/hide password; form-level messages |
| `ConfirmDialog` | Only for destructive or hard-to-undo actions, with a named confirm label and a pending state |
| `EmptyState`, `ErrorState`, `LoadingSkeleton`, `FullPageLoader`, `FullPageError` | Every data view's non-happy states |
| `BrandLogo` | The logo slot: put the owner's file at `public/logo.svg`; until then a plain "HF" monogram holds the place |
| `SegmentedControl` | One choice from a few options: date ranges, chart/table view, account type. A radio group with arrow-key navigation |
| `ChartCard` | Frame for every chart: title, a description that states the headline in words, a Chart/Table switch, and skeleton, empty, error and refreshing (dimmed, no flash) states. The plot fills a stretched card |
| `charts/Charts.tsx` | `AreaTrend` (change over time), `HorizontalBars` (a few named categories), `ColumnBars` (counts per period). All single-series, all with the value-first tooltip |

### Charts (Day 8, following the dataviz method)

- **One series per chart, one hue.** Magnitude uses brand indigo (`#4338CA`, validated on white: lightness band, chroma, ≥3:1 contrast); axis labels carry identity, so no legend box is needed. Violet is only for AI-derived charts. Status colours are never used as series colours.
- **Colours** come from `CHART_COLORS` in `src/lib/chart.ts`. SVG attributes can't reliably read CSS variables, so that file is the only place besides `index.css` that holds hex values.
- **Marks:**
  - Lines are 2px and straight (`linear`): a curve would invent values between whole-number days.
  - The area wash is 10% opacity.
  - Bars are at most 22px thick, with a 4px rounded data-end and a square baseline.
  - Gridlines are solid 1px `--border`, and horizontal only for vertical-value charts.
  - Markers appear only on hover: 8px, with a 2px white ring.
- **Text** uses text tokens, never the series colour. Bars show their value at the tip; lines and columns rely on axes and the tooltip.
- **Tooltip:** the value first (strong), the label second.
- **Table view:** every chart has one, and empty data shows a message, not a flat chart.
- **Date range:** one `SegmentedControl` in a row above the section it scopes, never inside a chart card. Everything below it uses the same window, and data refetches keep the previous render dimmed rather than flashing a skeleton.
- **Big numbers** (stat cards) use proportional digits; `tabular-nums` is only for columns and tables.

### Patterns

- **Surfaces:** white cards with a 1px `border-border` on the `#F8FAFC` page. No shadow on resting cards; shadows only on floating layers (menus, dialogs, the auth example card).
- **Navigation:** the active item uses `bg-sidebar-accent` with `text-sidebar-accent-foreground` and an indigo icon. Pages not built yet show "Soon" and are not links.
- **Tables:** sentence-case header labels on a `bg-muted/60` header row, and a `hover:bg-muted/40` row hover.
- **Auth pages:** a solid indigo brand panel (no gradient) beside a 400px form column. The panel's "Match breakdown" card is labelled "Illustrative example" and shows the scoring rubric (architecture.md §8.4) rather than claiming it.
- **Motion:** colour transitions of 150ms. No entrance animations; Radix's own dialog and menu transitions only.
