import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mount } from '@vue/test-utils'
import DashboardPage from './DashboardPage.vue'

const mocks = vi.hoisted(() => ({
  listMistakes: vi.fn(),
  listUserFeedback: vi.fn(),
  getPracticeStats: vi.fn(),
  push: vi.fn()
}))

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>'
  },
  useRouter: () => ({
    push: mocks.push
  })
}))

vi.mock('../api', () => ({
  getPracticeStats: mocks.getPracticeStats,
  listMistakes: mocks.listMistakes,
  listUserFeedback: mocks.listUserFeedback
}))

describe('DashboardPage', () => {
  beforeEach(() => {
    mocks.push.mockReset()
    mocks.getPracticeStats.mockReset()
    mocks.listMistakes.mockReset()
    mocks.listUserFeedback.mockReset()
    mocks.getPracticeStats.mockResolvedValue({ userId: 7, answeredQuestionCount: 0, correctQuestionCount: 0 })
    mocks.listMistakes.mockResolvedValue([])
    mocks.listUserFeedback.mockResolvedValue([])
    window.localStorage.clear()
  })

  it('renders the main local learning workspace', () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ username: 'alice', role: 'USER', displayName: 'Alice' })
    )

    const wrapper = mount(DashboardPage)

    expect(wrapper.text()).toContain('学习控制台')
    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('普通用户')
    expect(wrapper.text()).toContain('题库导入')
    expect(wrapper.text()).toContain('自定义组卷')
    expect(wrapper.text()).toContain('错题反馈')
    expect(wrapper.text()).toContain('AI 分析')
  })

  it('hides administrator-only navigation from ordinary users', () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ username: 'alice', role: 'USER', displayName: 'Alice' })
    )

    const wrapper = mount(DashboardPage)

    expect(wrapper.text()).not.toContain('题库管理')
    expect(wrapper.text()).not.toContain('反馈审核')
    expect(wrapper.text()).toContain('题目导入')
    expect(wrapper.text()).toContain('练习中心')
  })

  it('loads dashboard metrics from the signed-in user activity', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ userId: 7, username: 'alice', role: 'USER', displayName: 'Alice' })
    )
    mocks.getPracticeStats.mockResolvedValue({ userId: 7, answeredQuestionCount: 18, correctQuestionCount: 15 })
    mocks.listMistakes.mockResolvedValue([
      { userId: 7, questionId: 1, questionTitle: 'HashMap 默认负载因子是多少？', knowledgePoint: '集合框架', status: 'PENDING' },
      { userId: 7, questionId: 2, questionTitle: 'JVM 栈保存什么？', knowledgePoint: 'JVM', status: 'PENDING' }
    ])
    mocks.listUserFeedback.mockResolvedValue([
      { id: 1, userId: 7, questionId: 1, type: 'ANSWER_ERROR', content: '标准答案应为 B', status: 'PENDING' }
    ])

    const wrapper = mount(DashboardPage)
    await flushPromises()

    expect(mocks.getPracticeStats).toHaveBeenCalledWith(7)
    expect(mocks.listMistakes).toHaveBeenCalledWith(7)
    expect(mocks.listUserFeedback).toHaveBeenCalledWith(7)
    expect(wrapper.text()).toContain('18 题')
    expect(wrapper.text()).toContain('正确率 83%')
    expect(wrapper.text()).toContain('2 题')
    expect(wrapper.text()).toContain('1 条')
  })

  it('logs out and returns to login page', async () => {
    window.localStorage.setItem('studyCollectionUser', JSON.stringify({ role: 'USER', displayName: '学习用户' }))
    const wrapper = mount(DashboardPage)

    await wrapper.get('button[aria-label="退出登录"]').trigger('click')

    expect(window.localStorage.getItem('studyCollectionUser')).toBeNull()
    expect(mocks.push).toHaveBeenCalledWith('/')
  })
})
