import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ExamRuleManagementPage from './ExamRuleManagementPage.vue'
import {
  listAdminExamRules,
  listKnowledgePoints,
  publishExamRule
} from '../api'

vi.mock('../api', () => ({
  createExamRule: vi.fn(),
  deleteExamRule: vi.fn(),
  listAdminExamRules: vi.fn(),
  listKnowledgePoints: vi.fn(),
  publishExamRule: vi.fn(),
  unpublishExamRule: vi.fn(),
  updateExamRule: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

describe('ExamRuleManagementPage', () => {
  beforeEach(() => {
    vi.mocked(listKnowledgePoints).mockReset().mockResolvedValue([
      { id: 1, name: 'Java 基础', description: '基础语法', enabled: true }
    ])
    vi.mocked(listAdminExamRules).mockReset().mockResolvedValue([{
      id: 5,
      name: 'Java 入门模拟考试',
      description: '检验基础语法',
      durationMinutes: 20,
      totalQuestions: 10,
      knowledgePoints: ['Java 基础'],
      typeQuotas: {
        SINGLE_CHOICE: 10,
        MULTIPLE_CHOICE: 0,
        TRUE_FALSE: 0,
        FILL_BLANK: 0,
        SHORT_ANSWER: 0,
        PROGRAMMING: 0
      },
      difficultyQuotas: { BEGINNER: 10, INTERMEDIATE: 0, ADVANCED: 0 },
      status: 'DRAFT',
      createdBy: 1,
      createdAt: '2026-07-12T03:00:00Z',
      updatedAt: '2026-07-12T03:00:00Z'
    }])
    vi.mocked(publishExamRule).mockReset().mockResolvedValue({
      id: 5,
      name: 'Java 入门模拟考试',
      description: '检验基础语法',
      durationMinutes: 20,
      totalQuestions: 10,
      knowledgePoints: ['Java 基础'],
      typeQuotas: { SINGLE_CHOICE: 10, MULTIPLE_CHOICE: 0, TRUE_FALSE: 0, FILL_BLANK: 0, SHORT_ANSWER: 0, PROGRAMMING: 0 },
      difficultyQuotas: { BEGINNER: 10, INTERMEDIATE: 0, ADVANCED: 0 },
      status: 'PUBLISHED',
      createdBy: 1,
      createdAt: '2026-07-12T03:00:00Z',
      updatedAt: '2026-07-12T03:01:00Z'
    })
  })

  it('renders every quota dimension and publishes a draft rule', async () => {
    const wrapper = mount(ExamRuleManagementPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('考试规则管理')
    expect(wrapper.text()).toContain('Java 入门模拟考试')
    expect(wrapper.text()).toContain('草稿')
    expect(wrapper.findAll('[data-quota-type]')).toHaveLength(6)
    expect(wrapper.findAll('[data-quota-difficulty]')).toHaveLength(3)
    expect(wrapper.text()).toContain('题型合计 10 / 10')
    expect(wrapper.text()).toContain('难度合计 10 / 10')

    await wrapper.get('[data-action="publish"]').trigger('click')
    await flushPromises()
    expect(publishExamRule).toHaveBeenCalledWith(5)
    expect(wrapper.text()).toContain('已发布')
  })
})
