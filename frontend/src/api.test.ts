import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  acceptQuestionFeedback,
  acceptQuestionFeedbackGroup,
  approvePendingQuestion,
  composeCustomExam,
  createExamRule,
  createKnowledgePoint,
  deleteExamRule,
  deleteQuestion,
  disableKnowledgePoint,
  generatePractice,
  generateKnowledgeQuestions,
  generateLearningReport,
  getExamSession,
  getRecentPractices,
  getPracticeStats,
  listExamSessions,
  listAdminExamRules,
  listPublishedExamRules,
  listLearningReports,
  listKnowledgePoints,
  listPendingFeedback,
  listPendingFeedbackGroups,
  listPendingQuestions,
  listMistakes,
  listUsers,
  listUserFeedback,
  listQuestionRevisions,
  login,
  markFeedbackNeedsReview,
  publishExamRule,
  previewImport,
  recordMistake,
  rejectPendingQuestion,
  rejectQuestionFeedback,
  searchQuestions,
  saveExamAnswer,
  submitPendingQuestion,
  submitPractice,
  submitUserPractice,
  submitQuestionFeedback,
  submitExamSession,
  startSimulationExam,
  unpublishExamRule,
  updateExamRule,
  updateMistakeStatus,
  uploadKnowledgeFile,
  uploadQuestionFile
} from './api'

describe('api client', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('posts login requests through the local api proxy', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        code: 'OK',
        data: { token: 'token-1', userId: 2, username: 'user', role: 'USER', displayName: '学习用户' }
      })
    })
    vi.stubGlobal('fetch', fetchMock)

    const response = await login({ username: 'user', password: 'user123' })

    expect(fetchMock).toHaveBeenCalledWith('/api/auth/login', expect.objectContaining({ method: 'POST' }))
    expect(response.token).toBe('token-1')
    expect(response.userId).toBe(2)
  })

  it('sends the logged-in bearer token with protected requests', async () => {
    window.localStorage.setItem('studyCollectionUser', JSON.stringify({
      token: 'signed-token',
      userId: 2,
      username: 'user',
      role: 'USER',
      displayName: '学习用户'
    }))
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ code: 'OK', data: [] })
    })
    vi.stubGlobal('fetch', fetchMock)

    await searchQuestions()

    expect(fetchMock).toHaveBeenCalledWith('/api/questions', expect.objectContaining({
      headers: expect.objectContaining({ Authorization: 'Bearer signed-token' })
    }))
  })

  it('surfaces api error messages from non-OK responses', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ code: 'VALIDATION_FAILED', message: '账号或密码错误', data: null })
    })
    vi.stubGlobal('fetch', fetchMock)

    await expect(login({ username: 'user', password: 'bad-password' })).rejects.toThrow('账号或密码错误')
  })

  it('lists administrator user summaries', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        code: 'OK',
        data: [
          { id: 1, username: 'admin', displayName: '系统管理员', role: 'ADMIN' },
          { id: 2, username: 'alice', displayName: 'Alice', role: 'USER' }
        ]
      })
    })
    vi.stubGlobal('fetch', fetchMock)

    const users = await listUsers()

    expect(users).toHaveLength(2)
    expect(users[0].displayName).toBe('系统管理员')
    expect(JSON.stringify(users)).not.toContain('password')
    expect(fetchMock).toHaveBeenCalledWith('/api/users', expect.objectContaining({ method: 'GET' }))
  })

  it('manages knowledge point categories', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: [{ id: 1, name: 'JVM', description: '运行时内存与类加载', enabled: true }]
        })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: { id: 2, name: '并发编程', description: '线程与锁', enabled: true }
        })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: { id: 1, name: 'JVM', description: '运行时内存与类加载', enabled: false }
        })
      })
    vi.stubGlobal('fetch', fetchMock)

    const points = await listKnowledgePoints()
    const created = await createKnowledgePoint({ name: '并发编程', description: '线程与锁' })
    const disabled = await disableKnowledgePoint(1)

    expect(points[0].name).toBe('JVM')
    expect(created.enabled).toBe(true)
    expect(disabled.enabled).toBe(false)
    expect(fetchMock).toHaveBeenCalledWith('/api/knowledge-points', expect.objectContaining({ method: 'GET' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/knowledge-points', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/knowledge-points/1/disable', expect.objectContaining({ method: 'POST' }))
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
        json: async () => ({ code: 'OK', data: { userId: 7, answeredQuestionCount: 2, gradedQuestionCount: 1, correctQuestionCount: 1 } })
      })
    vi.stubGlobal('fetch', fetchMock)

    const preview = await previewImport('题目: Java 中 int 默认值是多少？')
    const generated = await generateKnowledgeQuestions('HashMap 默认负载因子是 0.75。')
    const uploaded = await uploadKnowledgeFile(new File(['HashMap 默认负载因子是 0.75。'], 'hashmap.md', { type: 'text/markdown' }))
    await submitPractice([{ questionId: 1, answer: 'A' }])
    await submitUserPractice([{ questionId: 2, answer: 'B' }])
    const stats = await getPracticeStats()

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
          answers: [{ questionId: 2, answer: 'B' }]
        })
      })
    )
    expect(fetchMock).toHaveBeenCalledWith('/api/practice/stats', expect.objectContaining({ method: 'GET' }))
    expect(stats.answeredQuestionCount).toBe(2)
    expect(stats.gradedQuestionCount).toBe(1)
  })

  it('uploads a structured question file for editable preview', async () => {
    window.localStorage.setItem('studyCollectionUser', JSON.stringify({
      token: 'signed-token', userId: 7, username: 'alice', role: 'USER', displayName: 'Alice'
    }))
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        code: 'OK',
        data: {
          questions: [{
            title: 'Java 的入口方法是什么？',
            type: 'SHORT_ANSWER',
            difficulty: 'BEGINNER',
            knowledgePoint: 'Java 基础',
            answer: 'main 方法',
            analysis: 'public static void main'
          }]
        }
      })
    })
    vi.stubGlobal('fetch', fetchMock)
    const file = new File(['[]'], 'questions.json', { type: 'application/json' })

    const questions = await uploadQuestionFile(file)

    expect(questions).toHaveLength(1)
    expect(questions[0].type).toBe('SHORT_ANSWER')
    expect(fetchMock).toHaveBeenCalledWith('/api/imports/questions/upload', expect.objectContaining({
      method: 'POST',
      body: expect.any(FormData),
      headers: { Authorization: 'Bearer signed-token' }
    }))
  })

  it('generates a filtered practice without sending empty filters', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        code: 'OK',
        data: {
          requestedCount: 3,
          actualCount: 1,
          questions: [{ id: 9, title: 'HashMap 的默认负载因子是多少？', type: 'SINGLE_CHOICE', difficulty: 'INTERMEDIATE', knowledgePoint: '集合框架' }]
        }
      })
    })
    vi.stubGlobal('fetch', fetchMock)

    const generated = await generatePractice({
      knowledgePoint: '集合框架',
      difficulty: 'INTERMEDIATE',
      type: '',
      count: 3,
      scope: 'PERSONAL'
    })

    expect(generated.actualCount).toBe(1)
    expect(fetchMock).toHaveBeenCalledWith('/api/practice/generate', expect.objectContaining({ method: 'POST' }))
    expect(JSON.parse(fetchMock.mock.calls[0][1].body)).toEqual({
      count: 3,
      knowledgePoint: '集合框架',
      difficulty: 'INTERMEDIATE',
      scope: 'PERSONAL'
    })
  })

  it('loads recent practice batches for the current authenticated user', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        code: 'OK',
        data: [{
          referenceId: 'practice-1',
          attemptedAt: '2026-07-12T01:00:00Z',
          answeredQuestionCount: 3,
          gradedQuestionCount: 2,
          correctQuestionCount: 1,
          accuracy: 0.5,
          knowledgePoints: ['JVM', '集合框架']
        }]
      })
    })
    vi.stubGlobal('fetch', fetchMock)

    const recent = await getRecentPractices(3)

    expect(recent).toHaveLength(1)
    expect(recent[0].accuracy).toBe(0.5)
    expect(fetchMock).toHaveBeenCalledWith('/api/practice/recent?limit=3', expect.objectContaining({ method: 'GET' }))
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
    await searchQuestions({ keyword: 'HashMap', scope: 'PERSONAL' })

    expect(fetchMock).toHaveBeenCalledWith('/api/questions', expect.objectContaining({ method: 'GET' }))
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/questions?keyword=HashMap&scope=PERSONAL',
      expect.objectContaining({ method: 'GET' })
    )
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
      title: 'HashMap 默认负载因子是多少？',
      type: 'SINGLE_CHOICE',
      difficulty: 'INTERMEDIATE',
      knowledgePoint: '集合框架',
      answer: 'A',
      analysis: '由导入预览提交审核',
      targetScope: 'PERSONAL'
    })
    const pending = await listPendingQuestions()
    await approvePendingQuestion(1)
    const rejected = await rejectPendingQuestion(2)

    expect(submitted.status).toBe('PENDING')
    expect(pending).toHaveLength(1)
    expect(rejected.status).toBe('REJECTED')
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/pending', expect.objectContaining({
      method: 'POST',
      body: expect.stringContaining('"targetScope":"PERSONAL"')
    }))
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
      questionId: 101,
      type: 'ANSWER_ERROR',
      content: '标准答案应为 B'
    })
    const pending = await listPendingFeedback()
    const userFeedback = await listUserFeedback()
    const revision = await acceptQuestionFeedback(1, {
      changeSummary: '答案从 A 修改为 B',
      reviewNote: '用户反馈属实'
    })

    expect(submitted.status).toBe('PENDING')
    expect(pending).toHaveLength(1)
    expect(userFeedback).toHaveLength(1)
    expect(revision.changeSummary).toContain('答案从 A 修改为 B')
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/feedback', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/feedback/pending', expect.objectContaining({ method: 'GET' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/feedback', expect.objectContaining({ method: 'GET' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/feedback/1/accept', expect.objectContaining({ method: 'POST' }))
  })

  it('lists grouped feedback, accepts duplicates together and reads revisions', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: [] }) })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: { id: 11, questionId: 101, feedbackId: 2, relatedFeedbackIds: [2, 1] }
        })
      })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: [] }) })
    vi.stubGlobal('fetch', fetchMock)

    await listPendingFeedbackGroups()
    await acceptQuestionFeedbackGroup({
      feedbackIds: [2, 1],
      changeSummary: '答案从 A 修改为 B',
      reviewNote: '重复反馈已核验',
      correctedAnswer: 'B',
      correctedAnalysis: '新解析'
    })
    await listQuestionRevisions(101)

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      '/api/questions/feedback/pending/groups',
      expect.objectContaining({ method: 'GET' })
    )
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/questions/feedback/groups/accept',
      expect.objectContaining({
        method: 'POST',
        body: expect.stringContaining('"feedbackIds":[2,1]')
      })
    )
    expect(fetchMock).toHaveBeenNthCalledWith(
      3,
      '/api/questions/feedback/revisions/101',
      expect.objectContaining({ method: 'GET' })
    )
  })

  it('creates, lists, restores, saves and submits persisted exam sessions', async () => {
    const session = {
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
          title: 'HashMap 默认负载因子是多少？',
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
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: session }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: [{ ...session, questionCount: 1, answeredCount: 0, questions: undefined }] }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: session }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: { ...session, questions: [{ ...session.questions[0], submittedAnswer: 'A' }] } }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: { ...session, status: 'SUBMITTED', score: 10, totalScore: 10 } }) })
    vi.stubGlobal('fetch', fetchMock)

    const created = await composeCustomExam({
      name: '集合专项测试',
      durationMinutes: 45,
      questionIds: [1, 2, 3]
    })
    const history = await listExamSessions()
    const restored = await getExamSession(91)
    const saved = await saveExamAnswer(91, 1, 'A')
    const submitted = await submitExamSession(91)

    expect(created.id).toBe(91)
    expect(history[0].questionCount).toBe(1)
    expect(restored.remainingSeconds).toBe(2700)
    expect(saved.questions[0].submittedAnswer).toBe('A')
    expect(submitted.status).toBe('SUBMITTED')
    expect(fetchMock).toHaveBeenCalledWith('/api/exams/custom', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/exams', expect.objectContaining({ method: 'GET' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/exams/91', expect.objectContaining({ method: 'GET' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/exams/91/answers/1', expect.objectContaining({
      method: 'PUT',
      body: JSON.stringify({ answer: 'A' })
    }))
    expect(fetchMock).toHaveBeenCalledWith('/api/exams/91/submit', expect.objectContaining({ method: 'POST' }))
  })

  it('manages exam rules and starts a published simulation exam', async () => {
    window.localStorage.setItem('studyCollectionUser', JSON.stringify({
      token: 'admin-token',
      userId: 1,
      username: 'admin',
      role: 'ADMIN',
      displayName: '系统管理员'
    }))
    const rule = {
      id: 5,
      name: 'Java 入门模拟考试',
      description: '检验基础语法',
      durationMinutes: 20,
      totalQuestions: 1,
      knowledgePoints: ['Java 基础'],
      typeQuotas: { SINGLE_CHOICE: 1 },
      difficultyQuotas: { BEGINNER: 1 },
      status: 'DRAFT',
      createdBy: 1,
      createdAt: '2026-07-12T03:00:00Z',
      updatedAt: '2026-07-12T03:00:00Z'
    }
    const session = {
      id: 91,
      name: rule.name,
      durationMinutes: 20,
      status: 'IN_PROGRESS',
      startedAt: '2026-07-12T03:00:00Z',
      expiresAt: '2026-07-12T03:20:00Z',
      submittedAt: null,
      remainingSeconds: 1200,
      score: null,
      totalScore: null,
      questions: []
    }
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: [{ ...rule, status: 'PUBLISHED' }] }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: [rule] }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: rule }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: rule }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: { ...rule, status: 'PUBLISHED' } }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: rule }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: 5 }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: session }) })
    vi.stubGlobal('fetch', fetchMock)
    const payload = {
      name: rule.name,
      description: rule.description,
      durationMinutes: rule.durationMinutes,
      totalQuestions: rule.totalQuestions,
      knowledgePoints: rule.knowledgePoints,
      typeQuotas: rule.typeQuotas,
      difficultyQuotas: rule.difficultyQuotas
    }

    await listPublishedExamRules()
    await listAdminExamRules()
    await createExamRule(payload)
    await updateExamRule(5, payload)
    await publishExamRule(5)
    await unpublishExamRule(5)
    await deleteExamRule(5)
    const started = await startSimulationExam(5)

    expect(started.id).toBe(91)
    expect(fetchMock).toHaveBeenCalledWith('/api/exam-rules', expect.objectContaining({ method: 'GET' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/exam-rules/admin', expect.objectContaining({ method: 'GET' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/exam-rules', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/exam-rules/5', expect.objectContaining({ method: 'PUT' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/exam-rules/5/publish', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/exam-rules/5/unpublish', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/exam-rules/5', expect.objectContaining({ method: 'DELETE' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/exam-rules/5/start', expect.objectContaining({ method: 'POST' }))
  })

  it('generates and lists trusted learning reports without client result samples', async () => {
    const report = {
      id: 21,
      createdAt: '2026-07-11T09:00:00Z',
      weakestKnowledgePoint: 'JVM',
      recommendation: '建议优先强化 JVM',
      adviceSource: 'RULES',
      adviceContent: '规则分析建议：请针对 JVM 继续练习。',
      answeredQuestionCount: 4,
      gradedQuestionCount: 3,
      correctQuestionCount: 1,
      accuracy: 1 / 3,
      knowledgePointPerformance: [],
      questionTypePerformance: [],
      recentTrend: [],
      strengtheningQuestions: [],
      revisionPolicy: 'EXCLUDE_REVISED',
      revisedAttemptCount: 0
    }
    const fetchMock = vi.fn()
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: report }) })
      .mockResolvedValueOnce({ ok: true, json: async () => ({ code: 'OK', data: [report] }) })
    vi.stubGlobal('fetch', fetchMock)

    const generated = await generateLearningReport({
      mode: 'OFFLINE_RULES',
      revisedQuestionPolicy: 'EXCLUDE_REVISED'
    })
    const history = await listLearningReports()

    expect(generated.weakestKnowledgePoint).toBe('JVM')
    expect(history).toHaveLength(1)
    expect(fetchMock).toHaveBeenCalledWith('/api/reports/learning', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ mode: 'OFFLINE_RULES', revisedQuestionPolicy: 'EXCLUDE_REVISED' })
    }))
    expect(fetchMock).toHaveBeenCalledWith('/api/reports/learning', expect.objectContaining({ method: 'GET' }))
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
            questionType: 'SINGLE_CHOICE',
            knowledgePoint: '集合框架',
            lastSubmittedAnswer: 'A',
            sourceContext: 'PRACTICE',
            status: 'PENDING',
            wrongCount: 1,
            firstWrongAt: '2026-07-10T08:00:00Z',
            lastWrongAt: '2026-07-10T08:00:00Z'
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
              questionType: 'SINGLE_CHOICE',
              knowledgePoint: '集合框架',
              lastSubmittedAnswer: 'A',
              sourceContext: 'PRACTICE',
              status: 'PENDING',
              wrongCount: 1,
              firstWrongAt: '2026-07-10T08:00:00Z',
              lastWrongAt: '2026-07-10T08:00:00Z'
            }
          ]
        })
      })
    vi.stubGlobal('fetch', fetchMock)

    const recorded = await recordMistake({
      questionId: 1,
      submittedAnswer: 'A',
      sourceContext: 'PRACTICE'
    })
    const mistakes = await listMistakes({
      knowledgePoint: '集合框架',
      questionType: 'SINGLE_CHOICE',
      status: 'PENDING',
      wrongFrom: '2026-07-10',
      wrongTo: '2026-07-12'
    })

    expect(recorded.status).toBe('PENDING')
    expect(mistakes).toHaveLength(1)
    expect(fetchMock).toHaveBeenCalledWith('/api/mistakes', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/mistakes?knowledgePoint=%E9%9B%86%E5%90%88%E6%A1%86%E6%9E%B6&questionType=SINGLE_CHOICE&status=PENDING&wrongFrom=2026-07-10&wrongTo=2026-07-12',
      expect.objectContaining({ method: 'GET' })
    )
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

    const updated = await updateMistakeStatus({ questionId: 1, status: 'MASTERED' })

    expect(updated.status).toBe('MASTERED')
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/mistakes/status',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ questionId: 1, status: 'MASTERED' })
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
      reviewNote: '原答案正确'
    })
    const needsReview = await markFeedbackNeedsReview(2, {
      reviewNote: '需要二次复核'
    })

    expect(rejected.status).toBe('REJECTED')
    expect(needsReview.status).toBe('NEEDS_REVIEW')
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/feedback/1/reject', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/questions/feedback/2/needs-review', expect.objectContaining({ method: 'POST' }))
  })
})
