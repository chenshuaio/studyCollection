import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from './pages/LoginPage.vue'
import DashboardPage from './pages/DashboardPage.vue'
import RegisterPage from './pages/RegisterPage.vue'
import QuestionBankPage from './pages/QuestionBankPage.vue'
import ImportPage from './pages/ImportPage.vue'
import PracticePage from './pages/PracticePage.vue'
import FeedbackReviewPage from './pages/FeedbackReviewPage.vue'
import ExamPage from './pages/ExamPage.vue'
import ReportPage from './pages/ReportPage.vue'
import MistakePage from './pages/MistakePage.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'login', component: LoginPage },
    { path: '/register', name: 'register', component: RegisterPage },
    { path: '/dashboard', name: 'dashboard', component: DashboardPage },
    { path: '/questions', name: 'questions', component: QuestionBankPage },
    { path: '/import', name: 'import', component: ImportPage },
    { path: '/practice', name: 'practice', component: PracticePage },
    { path: '/exams', name: 'exams', component: ExamPage },
    { path: '/mistakes', name: 'mistakes', component: MistakePage },
    { path: '/reports', name: 'reports', component: ReportPage },
    { path: '/feedback', name: 'feedback', component: FeedbackReviewPage }
  ]
})
