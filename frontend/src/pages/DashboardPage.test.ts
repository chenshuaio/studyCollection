import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import DashboardPage from './DashboardPage.vue'

const mocks = vi.hoisted(() => ({
  listMistakes: vi.fn(),
  listUserFeedback: vi.fn(),
  getPracticeStats: vi.fn(),
  getRecentPractices: vi.fn(),
  listExamSessions: vi.fn(),
  listLearningReports: vi.fn(),
  push: vi.fn()
}))

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a :data-to="JSON.stringify(to)"><slot /></a>'
  },
  useRouter: () => ({
    push: mocks.push
  })
}))

vi.mock('../api', () => ({
  getPracticeStats: mocks.getPracticeStats,
  getRecentPractices: mocks.getRecentPractices,
  listExamSessions: mocks.listExamSessions,
  listLearningReports: mocks.listLearningReports,
  listMistakes: mocks.listMistakes,
  listUserFeedback: mocks.listUserFeedback
}))

describe('DashboardPage', () => {
  beforeEach(() => {
    mocks.push.mockReset()
    mocks.getPracticeStats.mockReset()
    mocks.listMistakes.mockReset()
    mocks.listUserFeedback.mockReset()
    mocks.getRecentPractices.mockReset()
    mocks.listExamSessions.mockReset()
    mocks.listLearningReports.mockReset()
    mocks.getPracticeStats.mockResolvedValue({ userId: 7, answeredQuestionCount: 0, gradedQuestionCount: 0, correctQuestionCount: 0 })
    mocks.listMistakes.mockResolvedValue([])
    mocks.listUserFeedback.mockResolvedValue([])
    mocks.getRecentPractices.mockResolvedValue([])
    mocks.listExamSessions.mockResolvedValue([])
    mocks.listLearningReports.mockResolvedValue([])
    window.localStorage.clear()
  })

  it('renders the main local learning workspace in Chinese', () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'user-token', userId: 7, username: 'alice', role: 'USER', displayName: 'Alice' })
    )

    const wrapper = mount(DashboardPage)

    expect(wrapper.text()).toContain('学习控制台')
    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('普通用户')
    expect(wrapper.text()).toContain('题库导入')
    expect(wrapper.text()).toContain('最近练习')
    expect(wrapper.text()).toContain('最近考试')
    expect(wrapper.text()).toContain('学习趋势')
  })

  it('hides administrator-only navigation from ordinary users', () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'user-token', userId: 7, username: 'alice', role: 'USER', displayName: 'Alice' })
    )

    const wrapper = mount(DashboardPage)

    expect(wrapper.text()).not.toContain('题库管理')
    expect(wrapper.text()).not.toContain('反馈审核')
    expect(wrapper.text()).not.toContain('用户管理')
    expect(wrapper.text()).toContain('题目导入')
    expect(wrapper.text()).toContain('练习中心')
  })

  it('shows user management navigation to administrators', () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'admin-token', userId: 1, username: 'admin', role: 'ADMIN', displayName: '系统管理员' })
    )

    const wrapper = mount(DashboardPage)

    expect(wrapper.text()).toContain('题库管理')
    expect(wrapper.text()).toContain('反馈审核')
    expect(wrapper.text()).toContain('用户管理')
  })

  it('loads dashboard metrics from the signed-in user activity', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'user-token', userId: 7, username: 'alice', role: 'USER', displayName: 'Alice' })
    )
    mocks.getPracticeStats.mockResolvedValue({ userId: 7, answeredQuestionCount: 18, gradedQuestionCount: 10, correctQuestionCount: 8 })
    mocks.listMistakes.mockResolvedValue([
      { userId: 7, questionId: 1, questionTitle: 'HashMap 默认负载因子是多少？', knowledgePoint: '集合框架', status: 'PENDING' },
      { userId: 7, questionId: 2, questionTitle: 'JVM 栈保存什么？', knowledgePoint: 'JVM', status: 'PENDING' }
    ])
    mocks.listUserFeedback.mockResolvedValue([
      { id: 1, userId: 7, questionId: 1, type: 'ANSWER_ERROR', content: '标准答案应为 B', status: 'PENDING' }
    ])

    const wrapper = mount(DashboardPage)
    await flushPromises()

    expect(mocks.getPracticeStats).toHaveBeenCalledWith()
    expect(mocks.listMistakes).toHaveBeenCalledWith()
    expect(mocks.listUserFeedback).toHaveBeenCalledWith()
    expect(wrapper.text()).toContain('18 题')
    expect(wrapper.text()).toContain('正确率 80%')
    expect(wrapper.text()).toContain('2 题')
    expect(wrapper.text()).toContain('1 条')
  })

  it('shows recent practices, exams, trend and the latest weak-point action', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'user-token', userId: 7, username: 'alice', role: 'USER', displayName: 'Alice' })
    )
    mocks.getRecentPractices.mockResolvedValue([{
      referenceId: 'practice-2',
      attemptedAt: '2026-07-12T01:00:00Z',
      answeredQuestionCount: 3,
      gradedQuestionCount: 2,
      correctQuestionCount: 1,
      accuracy: 0.5,
      knowledgePoints: ['JVM']
    }])
    mocks.listExamSessions.mockResolvedValue([{
      id: 91,
      name: '集合专项测试',
      durationMinutes: 30,
      status: 'SUBMITTED',
      questionCount: 5,
      answeredCount: 5,
      startedAt: '2026-07-11T08:00:00Z',
      expiresAt: '2026-07-11T08:30:00Z',
      submittedAt: '2026-07-11T08:20:00Z',
      score: 30,
      totalScore: 50
    }])
    mocks.listLearningReports.mockResolvedValue([{
      id: 21,
      createdAt: '2026-07-12T01:05:00Z',
      weakestKnowledgePoint: 'JVM',
      recommendation: '建议优先强化 JVM。',
      adviceSource: 'RULES',
      adviceContent: '规则分析建议。',
      answeredQuestionCount: 8,
      gradedQuestionCount: 7,
      correctQuestionCount: 4,
      accuracy: 4 / 7,
      knowledgePointPerformance: [],
      questionTypePerformance: [],
      recentTrend: [
        { date: '2026-07-11', gradedQuestionCount: 3, correctQuestionCount: 2, accuracy: 2 / 3 },
        { date: '2026-07-12', gradedQuestionCount: 4, correctQuestionCount: 2, accuracy: 0.5 }
      ],
      strengtheningQuestions: []
    }])

    const wrapper = mount(DashboardPage)
    await flushPromises()

    expect(mocks.getRecentPractices).toHaveBeenCalledWith(3)
    expect(mocks.listExamSessions).toHaveBeenCalledWith()
    expect(mocks.listLearningReports).toHaveBeenCalledWith()
    expect(wrapper.text()).toContain('JVM · 3 题')
    expect(wrapper.text()).toContain('集合专项测试')
    expect(wrapper.text()).toContain('30 / 50')
    expect(wrapper.text()).toContain('2026-07-12')
    expect(wrapper.text()).toContain('优先强化 JVM')
    const strengthenLink = wrapper.get('[data-action="dashboard-strengthen"]')
    expect(strengthenLink.attributes('data-to')).toContain('knowledgePoint')
    expect(strengthenLink.attributes('data-to')).toContain('JVM')
  })

  it('keeps available dashboard data visible when one source fails', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'user-token', userId: 7, username: 'alice', role: 'USER', displayName: 'Alice' })
    )
    mocks.getRecentPractices.mockRejectedValue(new Error('最近练习暂不可用'))
    mocks.listExamSessions.mockResolvedValue([{
      id: 92,
      name: '仍可显示的考试',
      durationMinutes: 20,
      status: 'IN_PROGRESS',
      questionCount: 2,
      answeredCount: 1,
      startedAt: '2026-07-12T01:00:00Z',
      expiresAt: '2026-07-12T01:20:00Z',
      submittedAt: null,
      score: null,
      totalScore: null
    }])

    const wrapper = mount(DashboardPage)
    await flushPromises()

    expect(wrapper.text()).toContain('有 1 项学习数据暂时加载失败')
    expect(wrapper.text()).toContain('仍可显示的考试')
    expect(wrapper.text()).toContain('进行中')
  })

  it('logs out and returns to login page', async () => {
    window.localStorage.setItem('studyCollectionUser', JSON.stringify({
      token: 'user-token', userId: 2, username: 'user', role: 'USER', displayName: '学习用户'
    }))
    const wrapper = mount(DashboardPage)

    await wrapper.get('button[aria-label="退出登录"]').trigger('click')

    expect(window.localStorage.getItem('studyCollectionUser')).toBeNull()
    expect(mocks.push).toHaveBeenCalledWith('/')
  })
  it('keeps shared responsive layout rules for desktop and phone viewports', () => {
    const theme = readFileSync(join(process.cwd(), 'src/styles/theme.css'), 'utf8')

    expect(theme).toContain('.dashboard-shell')
    expect(theme).toContain('.dashboard-sidebar nav')
    expect(theme).toContain('.header-actions')
    expect(theme).toContain('@media (max-width: 560px)')
    expect(theme).toContain('overflow-x: auto')
    expect(theme).toContain('min-width: 640px')
  })
})
