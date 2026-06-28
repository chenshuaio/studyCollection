import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  acceptQuestionFeedback,
  composeCustomExam,
  generateKnowledgeQuestions,
  generateLearningReport,
  listPendingFeedback,
  listMistakes,
  login,
  previewImport,
  recordMistake,
  submitPractice,
  submitQuestionFeedback,
  uploadKnowledgeFile
} from './api'

describe('api client', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('posts login requests through the local api proxy', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ code: 'OK', data: { token: 'token-1', role: 'USER', displayName: '学习用户' } })
    })
    vi.stubGlobal('fetch', fetchMock)

    const response = await login({ username: 'user', password: 'user123' })

    expect(fetchMock).toHaveBeenCalledWith('/api/auth/login', expect.objectContaining({ method: 'POST' }))
    expect(response.token).toBe('token-1')
  })

  it('posts import preview, knowledge generation, file upload and practice submissions', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ code: 'OK', data: { questions: [{ title: 'Java 中 int 默认值是多少？' }] } })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ code: 'OK', data: { questions: [{ title: 'HashMap 默认负载因子是多少？' }] } })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ code: 'OK', data: { questions: [{ title: 'HashMap 默认负载因子是多少？' }] } })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ code: 'OK', data: { score: 10, totalScore: 10, items: [] } })
      })
    vi.stubGlobal('fetch', fetchMock)

    const preview = await previewImport('题目: Java 中 int 默认值是多少？')
    const generated = await generateKnowledgeQuestions('HashMap 默认负载因子是 0.75。')
    const uploaded = await uploadKnowledgeFile(new File(['HashMap 默认负载因子是 0.75。'], 'hashmap.md', { type: 'text/markdown' }))
    await submitPractice([{ questionId: 1, answer: 'A' }])

    expect(preview[0].title).toBe('Java 中 int 默认值是多少？')
    expect(generated[0].title).toBe('HashMap 默认负载因子是多少？')
    expect(uploaded[0].title).toBe('HashMap 默认负载因子是多少？')
    expect(fetchMock).toHaveBeenCalledWith('/api/imports/preview', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/imports/knowledge/generate', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/imports/knowledge/upload', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/practice/submit', expect.objectContaining({ method: 'POST' }))
  })

  it('posts and reviews question feedback', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: {
            id: 1,
            userId: 7,
            questionId: 101,
            type: 'ANSWER_ERROR',
            content: '标准答案应为 B',
            status: 'PENDING'
          }
        })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: [
            {
              id: 1,
              userId: 7,
              questionId: 101,
              type: 'ANSWER_ERROR',
              content: '标准答案应为 B',
              status: 'PENDING'
            }
          ]
        })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: {
            id: 1,
            questionId: 101,
            feedbackId: 1,
            adminUserId: 1,
            changeSummary: '答案从 A 修改为 B',
            reviewNote: '用户反馈属实'
          }
        })
      })
    vi.stubGlobal('fetch', fetchMock)

    const submitted = await submitQuestionFeedback({
      userId: 7,
      questionId: 101,
      type: 'ANSWER_ERROR',
      content: '标准答案应为 B'
    })
    const pending = await listPendingFeedback()
    const revision = await acceptQuestionFeedback(1, {
      adminUserId: 1,
      changeSummary: '答案从 A 修改为 B',
      reviewNote: '用户反馈属实'
    })

    expect(submitted.status).toBe('PENDING')
    expect(pending).toHaveLength(1)
    expect(revision.changeSummary).toContain('答案从 A 修改为 B')
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/feedback', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/feedback/pending', expect.objectContaining({ method: 'GET' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/feedback/1/accept', expect.objectContaining({ method: 'POST' }))
  })

  it('creates custom exam papers', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        code: 'OK',
        data: {
          name: '集合专项测试',
          durationMinutes: 45,
          questionIds: [1, 2, 3]
        }
      })
    })
    vi.stubGlobal('fetch', fetchMock)

    const paper = await composeCustomExam({
      name: '集合专项测试',
      durationMinutes: 45,
      questionIds: [1, 2, 3]
    })

    expect(paper.name).toBe('集合专项测试')
    expect(paper.questionIds).toEqual([1, 2, 3])
    expect(fetchMock).toHaveBeenCalledWith('/api/exams/custom', expect.objectContaining({ method: 'POST' }))
  })

  it('generates learning reports with selectable analysis mode', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        code: 'OK',
        data: {
          weakestKnowledgePoint: 'JVM',
          recommendation: '建议优先强化 JVM',
          adviceSource: 'RULES',
          adviceContent: '规则分析建议：请针对 JVM 继续练习。'
        }
      })
    })
    vi.stubGlobal('fetch', fetchMock)

    const report = await generateLearningReport({
      mode: 'OFFLINE_RULES',
      results: [
        { knowledgePoint: '集合框架', correct: true },
        { knowledgePoint: 'JVM', correct: false }
      ]
    })

    expect(report.weakestKnowledgePoint).toBe('JVM')
    expect(report.adviceSource).toBe('RULES')
    expect(fetchMock).toHaveBeenCalledWith('/api/reports/learning', expect.objectContaining({ method: 'POST' }))
  })

  it('records and lists user mistakes', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: {
            userId: 7,
            questionId: 1,
            questionTitle: 'HashMap 默认负载因子是多少？',
            knowledgePoint: '集合框架',
            status: 'PENDING'
          }
        })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: [
            {
              userId: 7,
              questionId: 1,
              questionTitle: 'HashMap 默认负载因子是多少？',
              knowledgePoint: '集合框架',
              status: 'PENDING'
            }
          ]
        })
      })
    vi.stubGlobal('fetch', fetchMock)

    const recorded = await recordMistake({
      userId: 7,
      questionId: 1,
      questionTitle: 'HashMap 默认负载因子是多少？',
      knowledgePoint: '集合框架',
      status: 'PENDING'
    })
    const mistakes = await listMistakes(7)

    expect(recorded.status).toBe('PENDING')
    expect(mistakes).toHaveLength(1)
    expect(fetchMock).toHaveBeenCalledWith('/api/mistakes', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/mistakes?userId=7', expect.objectContaining({ method: 'GET' }))
  })
})
