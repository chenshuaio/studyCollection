import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ExamPage from './ExamPage.vue'
import { composeCustomExam, listExamSessions, searchQuestions, type ExamSession } from '../api'

vi.mock('../api', () => ({
  composeCustomExam: vi.fn(),
  listExamSessions: vi.fn(),
  searchQuestions: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

const question = {
  id: 42,
  title: 'ConcurrentHashMap 如何降低锁粒度？',
  type: 'SHORT_ANSWER',
  difficulty: 'ADVANCED',
  knowledgePoint: '并发编程',
  answer: '',
  analysis: ''
}

const createdSession: ExamSession = {
  id: 91,
  name: '集合专项测试',
  durationMinutes: 45,
  status: 'IN_PROGRESS',
  startedAt: '2026-07-11T06:00:00Z',
  expiresAt: '2026-07-11T06:45:00Z',
  submittedAt: null,
  remainingSeconds: 2700,
  score: null,
  totalScore: null,
  questions: [{
    ...question,
    submittedAnswer: '',
    autoGraded: false,
    correct: null,
    score: 0,
    correctAnswer: ''
  }]
}

describe('ExamPage', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    vi.mocked(searchQuestions).mockReset().mockResolvedValue([question])
    vi.mocked(listExamSessions).mockReset().mockResolvedValue([
      {
        id: 55,
        name: '上次 Java 测试',
        durationMinutes: 30,
        status: 'IN_PROGRESS',
        questionCount: 3,
        answeredCount: 1,
        startedAt: '2026-07-11T05:00:00Z',
        expiresAt: '2026-07-11T05:30:00Z',
        submittedAt: null,
        score: null,
        totalScore: null
      }
    ])
    vi.mocked(composeCustomExam).mockReset().mockResolvedValue(createdSession)
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('creates a server exam and renders resumable history without storing answers in the browser', async () => {
    const wrapper = mount(ExamPage, {
      global: {
        stubs: { RouterLink: routerLinkStub, LogoutButton: true }
      }
    })

    await flushPromises()
    expect(wrapper.text()).toContain('ConcurrentHashMap 如何降低锁粒度？')
    expect(wrapper.text()).toContain('上次 Java 测试')
    expect(wrapper.find('a[href="/exams/55/take"]').text()).toContain('继续答题')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(composeCustomExam).toHaveBeenCalledWith({
      name: '集合专项测试',
      durationMinutes: 45,
      questionIds: [42]
    })
    expect(wrapper.text()).toContain('共 1 题')
    expect(wrapper.find('a[href="/exams/91/take"]').exists()).toBe(true)
    expect(window.sessionStorage.getItem('studyCollectionExamPaper')).toBeNull()
  })

  it('shows completed exam score in history', async () => {
    vi.mocked(listExamSessions).mockResolvedValueOnce([
      {
        id: 77,
        name: '集合结课测试',
        durationMinutes: 20,
        status: 'SUBMITTED',
        questionCount: 5,
        answeredCount: 5,
        startedAt: '2026-07-10T05:00:00Z',
        expiresAt: '2026-07-10T05:20:00Z',
        submittedAt: '2026-07-10T05:18:00Z',
        score: 40,
        totalScore: 50
      }
    ])

    const wrapper = mount(ExamPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('40 / 50')
    expect(wrapper.find('a[href="/exams/77/take"]').text()).toContain('查看结果')
  })
})
