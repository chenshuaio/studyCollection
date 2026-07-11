import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ExamTakingPage from './ExamTakingPage.vue'
import { getExamSession, recordMistake, saveExamAnswer, submitExamSession, type ExamSession } from '../api'

vi.mock('vue-router', () => ({
  RouterLink: {
    name: 'RouterLink',
    props: ['to'],
    template: '<a :href="to"><slot /></a>'
  },
  useRoute: () => ({ params: { examId: '91' } })
}))

vi.mock('../api', () => ({
  getExamSession: vi.fn(),
  saveExamAnswer: vi.fn(),
  submitExamSession: vi.fn(),
  recordMistake: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

function activeSession(): ExamSession {
  return {
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
    questions: [
      {
        id: 1,
        title: 'HashMap 默认负载因子是多少？\nA. 0.75\nB. 0.5',
        type: 'SINGLE_CHOICE',
        difficulty: 'INTERMEDIATE',
        knowledgePoint: '集合框架',
        submittedAnswer: '',
        autoGraded: false,
        correct: null,
        score: 0,
        correctAnswer: '',
        analysis: ''
      },
      {
        id: 2,
        title: 'Java 局部变量必须先赋值再使用，这句话是否正确？',
        type: 'TRUE_FALSE',
        difficulty: 'BEGINNER',
        knowledgePoint: 'Java 基础',
        submittedAnswer: '',
        autoGraded: false,
        correct: null,
        score: 0,
        correctAnswer: '',
        analysis: ''
      },
      {
        id: 3,
        title: 'ArrayList 扩容通常发生在什么时候？',
        type: 'SINGLE_CHOICE',
        difficulty: 'INTERMEDIATE',
        knowledgePoint: '集合框架',
        submittedAnswer: '',
        autoGraded: false,
        correct: null,
        score: 0,
        correctAnswer: '',
        analysis: ''
      }
    ]
  }
}

describe('ExamTakingPage', () => {
  beforeEach(() => {
    vi.useRealTimers()
    vi.mocked(getExamSession).mockReset().mockResolvedValue(activeSession())
    vi.mocked(saveExamAnswer).mockReset().mockImplementation(async (_sessionId, questionId, answer) => ({
      ...activeSession(),
      questions: activeSession().questions.map((question) => question.id === questionId
        ? { ...question, submittedAnswer: answer }
        : question)
    }))
    vi.mocked(submitExamSession).mockReset().mockResolvedValue({
      ...activeSession(),
      status: 'SUBMITTED',
      submittedAt: '2026-07-11T06:10:00Z',
      remainingSeconds: 0,
      score: 20,
      totalScore: 30,
      questions: activeSession().questions.map((question, index) => ({
        ...question,
        submittedAnswer: index === 0 ? 'A' : index === 1 ? 'true' : 'B',
        autoGraded: true,
        correct: index < 2,
        score: index < 2 ? 10 : 0,
        correctAnswer: index === 1 ? 'true' : 'A',
        analysis: index === 2 ? 'ArrayList 在容量不足时扩容。' : '题目解析'
      }))
    })
    vi.mocked(recordMistake).mockReset().mockResolvedValue({
      userId: 7,
      questionId: 3,
      questionTitle: 'ArrayList 扩容通常发生在什么时候？',
      knowledgePoint: '集合框架',
      status: 'PENDING'
    })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  it('restores from the server, saves every answer and submits the persisted session', async () => {
    const wrapper = mount(ExamTakingPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    await flushPromises()

    expect(getExamSession).toHaveBeenCalledWith(91)
    expect(wrapper.text()).toContain('00:45:00')
    expect(wrapper.findAll('input[type="radio"]:checked')).toHaveLength(0)

    await wrapper.find('button[data-action="submit-exam"]').trigger('click')
    expect(wrapper.text()).toContain('请先完成所有题目后再提交。')

    await wrapper.find('input[name="question-1"][value="A"]').setValue()
    await wrapper.find('input[name="question-2"][value="true"]').setValue()
    await wrapper.find('input[name="question-3"][value="B"]').setValue()
    await flushPromises()
    await wrapper.find('button[data-action="submit-exam"]').trigger('click')
    await flushPromises()

    expect(saveExamAnswer).toHaveBeenCalledWith(91, 1, 'A')
    expect(saveExamAnswer).toHaveBeenCalledWith(91, 2, 'true')
    expect(saveExamAnswer).toHaveBeenCalledWith(91, 3, 'B')
    expect(submitExamSession).toHaveBeenCalledWith(91)
    expect(recordMistake).toHaveBeenCalledWith({
      questionId: 3,
      questionTitle: 'ArrayList 扩容通常发生在什么时候？',
      knowledgePoint: '集合框架',
      status: 'PENDING'
    })
    expect(wrapper.text()).toContain('20/30')
    expect(wrapper.text()).toContain('ArrayList 在容量不足时扩容。')
  })

  it('restores a saved multiple-choice answer without selecting unsaved options', async () => {
    vi.mocked(getExamSession).mockResolvedValueOnce({
      ...activeSession(),
      questions: [{
        ...activeSession().questions[0],
        id: 9,
        title: '以下哪些属于 Java 集合接口？\nA. List\nB. Set\nC. Thread',
        type: 'MULTIPLE_CHOICE',
        submittedAnswer: 'A,C'
      }]
    })
    const wrapper = mount(ExamTakingPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    await flushPromises()

    expect(wrapper.findAll('input[type="checkbox"]')).toHaveLength(3)
    expect(wrapper.findAll('input[type="checkbox"]:checked')).toHaveLength(2)
    expect((wrapper.find('input[value="B"]').element as HTMLInputElement).checked).toBe(false)
  })

  it('automatically submits saved progress when the countdown reaches zero', async () => {
    vi.useFakeTimers()
    vi.mocked(getExamSession).mockResolvedValueOnce({ ...activeSession(), remainingSeconds: 1 })
    const wrapper = mount(ExamTakingPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    await flushPromises()

    await vi.advanceTimersByTimeAsync(1000)
    await flushPromises()

    expect(submitExamSession).toHaveBeenCalledWith(91)
    expect(wrapper.text()).toContain('考试时间已到，系统已自动提交。')
  })

  it('shows subjective reference answers without marking them wrong', async () => {
    vi.mocked(getExamSession).mockResolvedValueOnce({
      ...activeSession(),
      status: 'SUBMITTED',
      remainingSeconds: 0,
      score: 0,
      totalScore: 0,
      questions: [{
        ...activeSession().questions[0],
        id: 12,
        title: '说明 ArrayList 与 LinkedList 的差异。',
        type: 'SHORT_ANSWER',
        submittedAnswer: '我的作答',
        autoGraded: false,
        correct: null,
        correctAnswer: 'ArrayList 基于数组，LinkedList 基于链表。',
        analysis: '从访问和增删复杂度分析。'
      }]
    })
    const wrapper = mount(ExamTakingPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('本题不自动评分，请结合参考答案自行核对。')
    expect(wrapper.text()).toContain('ArrayList 基于数组，LinkedList 基于链表。')
    expect(recordMistake).not.toHaveBeenCalled()
  })

  it('keeps a successful exam result when mistake synchronization fails', async () => {
    vi.mocked(recordMistake).mockRejectedValueOnce(new Error('错题服务暂不可用'))
    const wrapper = mount(ExamTakingPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    await flushPromises()

    await wrapper.find('input[name="question-1"][value="A"]').setValue()
    await wrapper.find('input[name="question-2"][value="true"]').setValue()
    await wrapper.find('input[name="question-3"][value="B"]').setValue()
    await wrapper.find('button[data-action="submit-exam"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('20/30')
    expect(wrapper.text()).toContain('试卷已提交，但有 1 道错题同步失败')
    expect(wrapper.text()).not.toContain('提交试卷失败')
  })
})
