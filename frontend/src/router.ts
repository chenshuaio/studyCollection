import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from './pages/LoginPage.vue'
import DashboardPage from './pages/DashboardPage.vue'
import RegisterPage from './pages/RegisterPage.vue'
import QuestionBankPage from './pages/QuestionBankPage.vue'
import ImportPage from './pages/ImportPage.vue'
import PracticePage from './pages/PracticePage.vue'
import FeedbackReviewPage from './pages/FeedbackReviewPage.vue'
import ExamPage from './pages/ExamPage.vue'
import ExamTakingPage from './pages/ExamTakingPage.vue'
import ReportPage from './pages/ReportPage.vue'
import MistakePage from './pages/MistakePage.vue'
import UserManagementPage from './pages/UserManagementPage.vue'
import { getCurrentUser, type CurrentUser } from './session'

type AccessRoute = {
  name?: unknown
  meta?: {
    requiresAuth?: boolean
    requiredRole?: string
  }
}

export function resolveRouteAccess(to: AccessRoute, currentUser: CurrentUser | null) {
  if (!to.meta?.requiresAuth) {
    return true
  }
  if (!currentUser) {
    return 'login'
  }
  if (to.meta.requiredRole && currentUser.role !== to.meta.requiredRole) {
    return 'dashboard'
  }
  return true
}

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'login', component: LoginPage },
    { path: '/register', name: 'register', component: RegisterPage },
    { path: '/dashboard', name: 'dashboard', component: DashboardPage, meta: { requiresAuth: true } },
    { path: '/questions', name: 'questions', component: QuestionBankPage, meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
    { path: '/import', name: 'import', component: ImportPage, meta: { requiresAuth: true } },
    { path: '/practice', name: 'practice', component: PracticePage, meta: { requiresAuth: true } },
    { path: '/exams', name: 'exams', component: ExamPage, meta: { requiresAuth: true } },
    { path: '/exams/take', name: 'exam-taking', component: ExamTakingPage, meta: { requiresAuth: true } },
    { path: '/mistakes', name: 'mistakes', component: MistakePage, meta: { requiresAuth: true } },
    { path: '/reports', name: 'reports', component: ReportPage, meta: { requiresAuth: true } },
    { path: '/feedback', name: 'feedback', component: FeedbackReviewPage, meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
    { path: '/users', name: 'users', component: UserManagementPage, meta: { requiresAuth: true, requiredRole: 'ADMIN' } }
  ]
})

router.beforeEach((to) => {
  const decision = resolveRouteAccess(to, getCurrentUser())
  return decision === true ? true : { name: decision }
})
