import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  acceptQuestionFeedback,
  approvePendingQuestion,
  composeCustomExam,
  deleteQuestion,
  generateKnowledgeQuestions,
  generateLearningReport,
  getPracticeStats,
  listPendingFeedback,
  listPendingQuestions,
  listMistakes,
  listUserFeedback,
  login,
  markFeedbackNeedsReview,
  previewImport,
  recordMistake,
  rejectPendingQuestion,
  rejectQuestionFeedback,
  searchQuestions,
  submitPendingQuestion,
  submitPractice,
  submitUserPractice,
  submitQuestionFeedback,
  updateMistakeStatus,
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
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ code: 'OK', data: { score: 10, totalScore: 10, items: [] } })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ code: 'OK', data: { userId: 7, answeredQuestionCount: 2, correctQuestionCount: 1 } })
      })
    vi.stubGlobal('fetch', fetchMock)

    const preview = await previewImport('题目: Java 中 int 默认值是多少？')
    const generated = await generateKnowledgeQuestions('HashMap 默认负载因子是 0.75。')
    const uploaded = await uploadKnowledgeFile(new File(['HashMap 默认负载因子是 0.75。'], 'hashmap.md', { type: 'text/markdown' }))
    await submitPractice([{ questionId: 1, answer: 'A' }])
    await submitUserPractice(7, [{ questionId: 2, answer: 'B', correctAnswer: 'A', analysis: '真实题库解析' }])
    const stats = await getPracticeStats(7)

    expect(preview[0].title).toBe('Java 中 int 默认值是多少？')
    expect(generated[0].title).toBe('HashMap 默认负载因子是多少？')
    expect(uploaded[0].title).toBe('HashMap 默认负载因子是多少？')
    expect(fetchMock).toHaveBeenCalledWith('/api/imports/preview', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/imports/knowledge/generate', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/imports/knowledge/upload', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/practice/submit', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/practice/submit',
      expect.objectContaining({
        body: JSON.stringify({
          userId: 7,
          answers: [{ questionId: 2, answer: 'B', correctAnswer: 'A', analysis: '真实题库解析' }]
        })
      })
    )
    expect(fetchMock).toHaveBeenCalledWith('/api/practice/stats?userId=7', expect.objectContaining({ method: 'GET' }))
    expect(stats.answeredQuestionCount).toBe(2)
  })

  it('searches all questions and supports fuzzy title keyword', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ code: 'OK', data: [] })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ code: 'OK', data: [{ id: 1, title: 'HashMap 默认负载因子是多少？' }] })
      })
    vi.stubGlobal('fetch', fetchMock)

    await searchQuestions()
    await searchQuestions({ keyword: 'HashMap' })

    expect(fetchMock).toHaveBeenCalledWith('/api/questions', expect.objectContaining({ method: 'GET' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/questions?keyword=HashMap', expect.objectContaining({ method: 'GET' }))
  })

  it('deletes formal questions by id', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ code: 'OK', data: 9 })
    })
    vi.stubGlobal('fetch', fetchMock)

    const deletedId = await deleteQuestion(9)

    expect(deletedId).toBe(9)
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/9', expect.objectContaining({ method: 'DELETE' }))
  })

  it('submits and reviews pending imported questions', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: {
            id: 1,
            submitterUserId: 7,
            title: 'HashMap 默认负载因子是多少？',
            type: 'SINGLE_CHOICE',
            difficulty: 'INTERMEDIATE',
            knowledgePoint: '集合框架',
            answer: 'A',
            analysis: '由导入预览提交审核',
            status: 'PENDING'
          }
        })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ code: 'OK', data: [{ id: 1, title: 'HashMap 默认负载因子是多少？', status: 'PENDING' }] })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ code: 'OK', data: { id: 9, title: 'HashMap 默认负载因子是多少？' } })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ code: 'OK', data: { id: 2, title: '错误题目', status: 'REJECTED' } })
      })
    vi.stubGlobal('fetch', fetchMock)

    const submitted = await submitPendingQuestion({
      submitterUserId: 7,
      title: 'HashMap 默认负载因子是多少？',
      type: 'SINGLE_CHOICE',
      difficulty: 'INTERMEDIATE',
      knowledgePoint: '集合框架',
      answer: 'A',
      analysis: '由导入预览提交审核'
    })
    const pending = await listPendingQuestions()
    await approvePendingQuestion(1)
    const rejected = await rejectPendingQuestion(2)

    expect(submitted.status).toBe('PENDING')
    expect(pending).toHaveLength(1)
    expect(rejected.status).toBe('REJECTED')
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/pending', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/pending', expect.objectContaining({ method: 'GET' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/pending/1/approve', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/pending/2/reject', expect.objectContaining({ method: 'POST' }))
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
    const userFeedback = await listUserFeedback(7)
    const revision = await acceptQuestionFeedback(1, {
      adminUserId: 1,
      changeSummary: '答案从 A 修改为 B',
      reviewNote: '用户反馈属实'
    })

    expect(submitted.status).toBe('PENDING')
    expect(pending).toHaveLength(1)
    expect(userFeedback).toHaveLength(1)
    expect(revision.changeSummary).toContain('答案从 A 修改为 B')
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/feedback', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/feedback/pending', expect.objectContaining({ method: 'GET' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/feedback?userId=7', expect.objectContaining({ method: 'GET' }))
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

  it('updates mistake mastery status', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        code: 'OK',
        data: {
          userId: 7,
          questionId: 1,
          questionTitle: 'HashMap 默认负载因子是多少？',
          knowledgePoint: '集合框架',
          status: 'MASTERED'
        }
      })
    })
    vi.stubGlobal('fetch', fetchMock)

    const updated = await updateMistakeStatus({ userId: 7, questionId: 1, status: 'MASTERED' })

    expect(updated.status).toBe('MASTERED')
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/mistakes/status',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ userId: 7, questionId: 1, status: 'MASTERED' })
      })
    )
  })

  it('posts feedback rejection and needs-review actions', async () => {
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
            status: 'REJECTED'
          }
        })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: {
            id: 2,
            userId: 8,
            questionId: 102,
            type: 'EXPLANATION_ERROR',
            content: '解析需要补充',
            status: 'NEEDS_REVIEW'
          }
        })
      })
    vi.stubGlobal('fetch', fetchMock)

    const rejected = await rejectQuestionFeedback(1, {
      adminUserId: 1,
      reviewNote: '原答案正确'
    })
    const needsReview = await markFeedbackNeedsReview(2, {
      adminUserId: 1,
      reviewNote: '需要二次复核'
    })

    expect(rejected.status).toBe('REJECTED')
    expect(needsReview.status).toBe('NEEDS_REVIEW')
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/feedback/1/reject', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/feedback/2/needs-review', expect.objectContaining({ method: 'POST' }))
  })
})
