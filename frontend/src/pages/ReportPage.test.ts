import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ReportPage from './ReportPage.vue'
import { generateLearningReport, listLearningReports, type LearningReport } from '../api'

vi.mock('../api', () => ({
  generateLearningReport: vi.fn(),
  listLearningReports: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a :data-to="JSON.stringify(to)"><slot /></a>'
}

const report: LearningReport = {
  id: 21,
  createdAt: '2026-07-11T09:00:00Z',
  weakestKnowledgePoint: 'JVM',
  recommendation: '建议优先强化 JVM（当前客观题正确率 0%）。',
  adviceSource: 'RULES',
  adviceContent: '规则分析建议：请针对 JVM 继续练习。',
  answeredQuestionCount: 4,
  gradedQuestionCount: 3,
  correctQuestionCount: 1,
  accuracy: 1 / 3,
  knowledgePointPerformance: [
    { label: 'JVM', answeredQuestionCount: 2, gradedQuestionCount: 1, correctQuestionCount: 0, accuracy: 0 },
    { label: '集合框架', answeredQuestionCount: 2, gradedQuestionCount: 2, correctQuestionCount: 1, accuracy: 0.5 }
  ],
  questionTypePerformance: [
    { label: 'FILL_BLANK', answeredQuestionCount: 1, gradedQuestionCount: 1, correctQuestionCount: 0, accuracy: 0 },
    { label: 'SHORT_ANSWER', answeredQuestionCount: 1, gradedQuestionCount: 0, correctQuestionCount: 0, accuracy: 0 }
  ],
  recentTrend: [
    { date: '2026-07-09', gradedQuestionCount: 1, correctQuestionCount: 0, accuracy: 0 },
    { date: '2026-07-10', gradedQuestionCount: 2, correctQuestionCount: 1, accuracy: 0.5 }
  ],
  strengtheningQuestions: [
    { id: 91, title: 'JVM 堆中主要保存什么？', type: 'FILL_BLANK', difficulty: 'INTERMEDIATE', knowledgePoint: 'JVM' }
  ],
  revisionPolicy: 'EXCLUDE_REVISED',
  revisedAttemptCount: 2
}

describe('ReportPage', () => {
  beforeEach(() => {
    window.localStorage.setItem('studyCollectionUser', JSON.stringify({
      token: 'user-token', userId: 7, username: 'alice', role: 'USER', displayName: 'Alice'
    }))
    vi.mocked(generateLearningReport).mockReset()
    vi.mocked(listLearningReports).mockReset()
    vi.mocked(listLearningReports).mockResolvedValue([report])
    vi.mocked(generateLearningReport).mockResolvedValue({ ...report, id: 22 })
  })

  it('shows trusted totals, breakdowns, trends, history and strengthening questions', async () => {
    const wrapper = mount(ReportPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })

    await flushPromises()

    expect(listLearningReports).toHaveBeenCalledWith()
    expect(wrapper.text()).toContain('已作答4')
    expect(wrapper.text()).toContain('客观题3')
    expect(wrapper.text()).toContain('正确率33%')
    expect(wrapper.text()).toContain('知识点表现')
    expect(wrapper.text()).toContain('题型表现')
    expect(wrapper.text()).toContain('2026-07-09')
    expect(wrapper.text()).toContain('历史报告')
    expect(wrapper.text()).toContain('JVM 堆中主要保存什么？')
    expect(wrapper.text()).toContain('已排除 2 条受题目修订影响的历史作答')
    expect(wrapper.get('[data-action="strengthen"]').attributes('data-to')).toContain('knowledgePoint')
    expect(wrapper.get('[data-action="strengthen"]').attributes('data-to')).toContain('JVM')
  })

  it('generates a new report with the selected analysis and revised-question policies', async () => {
    const wrapper = mount(ReportPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    await flushPromises()

    await wrapper.find('select[aria-label="修订题处理策略"]').setValue('RECALCULATE_REVISED')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(generateLearningReport).toHaveBeenCalledWith({
      mode: 'OFFLINE_RULES',
      revisedQuestionPolicy: 'RECALCULATE_REVISED'
    })
    expect(wrapper.text()).toContain('报告已生成')
    expect(wrapper.text()).toContain('JVM')
  })

  it('lets the backend explain why a report cannot be generated without attempts', async () => {
    vi.mocked(listLearningReports).mockResolvedValue([])
    vi.mocked(generateLearningReport).mockRejectedValue(new Error('暂无可用于分析的真实作答记录'))
    const wrapper = mount(ReportPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    await flushPromises()

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(generateLearningReport).toHaveBeenCalledWith({
      mode: 'OFFLINE_RULES',
      revisedQuestionPolicy: 'EXCLUDE_REVISED'
    })
    expect(wrapper.text()).toContain('暂无可用于分析的真实作答记录')
  })
})
