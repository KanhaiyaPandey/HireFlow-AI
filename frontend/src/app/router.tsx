import { createBrowserRouter } from 'react-router'

import { CandidateLayout } from '@/components/CandidateLayout'
import { MainLayout } from '@/components/MainLayout'
import { GuestOnly, HomeRedirect, RequireRole } from '@/features/auth/components/RouteGuards'

import { NotFoundPage } from './NotFoundPage'

/**
 * Route map. Guards are layout routes: RequireRole decides who may enter a branch, the layout
 * draws the shell, and pages render inside its <Outlet />. Every rule is re-checked by the API.
 *
 * Pages load lazily (one chunk each), so the sign-in page doesn't download every screen of the app.
 */
const page = (load: () => Promise<{ default: React.ComponentType }>) => async () => ({ Component: (await load()).default })

export const router = createBrowserRouter([
  { path: '/', element: <HomeRedirect /> },

  // Public: signed-in users are sent to their home instead.
  {
    element: <GuestOnly />,
    children: [
      { path: '/login', lazy: page(() => import('@/features/auth/pages/LoginPage')) },
      { path: '/register', lazy: page(() => import('@/features/auth/pages/RegisterPage')) },
      { path: '/forgot-password', lazy: page(() => import('@/features/auth/pages/ForgotPasswordPage')) },
    ],
  },
  // Public, and usable while signed in (links from emails).
  { path: '/reset-password', lazy: page(() => import('@/features/auth/pages/ResetPasswordPage')) },
  { path: '/accept-invite', lazy: page(() => import('@/features/auth/pages/AcceptInvitePage')) },

  // Staff shell (DESIGN.md: one application shell for every staff page).
  {
    element: <RequireRole roles={['HR_ADMIN', 'RECRUITER', 'EMPLOYEE', 'SUPER_ADMIN']} />,
    children: [
      {
        path: '/app',
        element: <MainLayout />,
        children: [
          {
            element: <RequireRole roles={['HR_ADMIN', 'RECRUITER']} />,
            children: [{ index: true, lazy: page(() => import('@/features/dashboard/pages/OverviewPage')) }],
          },
          {
            element: <RequireRole roles={['HR_ADMIN']} />,
            children: [
              { path: 'employees', lazy: page(() => import('@/features/employees/pages/EmployeesPage')) },
              { path: 'employees/new', lazy: page(() => import('@/features/employees/pages/EmployeeFormPage')) },
              { path: 'employees/:id', lazy: page(() => import('@/features/employees/pages/EmployeeProfilePage')) },
              { path: 'employees/:id/edit', lazy: page(() => import('@/features/employees/pages/EmployeeFormPage')) },
              { path: 'departments', lazy: page(() => import('@/features/departments/pages/DepartmentsPage')) },
              { path: 'departments/:id', lazy: page(() => import('@/features/departments/pages/DepartmentDetailPage')) },
            ],
          },
          {
            element: <RequireRole roles={['EMPLOYEE', 'HR_ADMIN', 'RECRUITER']} />,
            children: [{ path: 'profile', lazy: page(() => import('@/features/employee/pages/MyProfilePage')) }],
          },
          {
            element: <RequireRole roles={['SUPER_ADMIN']} />,
            children: [{ path: 'companies', lazy: page(() => import('@/features/admin/pages/CompaniesPage')) }],
          },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },

  // Candidate layout: same brand, lighter shell.
  {
    element: <RequireRole roles={['CANDIDATE']} />,
    children: [
      {
        path: '/candidate',
        element: <CandidateLayout />,
        children: [
          { index: true, lazy: page(() => import('@/features/candidate/pages/MyApplicationsPage')) },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },

  { path: '*', element: <NotFoundPage standalone /> },
])
