import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from './pages/LoginPage.vue'
import DashboardPage from './pages/DashboardPage.vue'
import RegisterPage from './pages/RegisterPage.vue'
import QuestionBankPage from './pages/QuestionBankPage.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'login', component: LoginPage },
    { path: '/register', name: 'register', component: RegisterPage },
    { path: '/dashboard', name: 'dashboard', component: DashboardPage },
    { path: '/questions', name: 'questions', component: QuestionBankPage }
  ]
})
