import { afterEach, describe, expect, it, vi } from 'vitest'
import { generateKnowledgeQuestions, login, previewImport, submitPractice } from './api'

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

  it('posts import preview, knowledge generation and practice submissions', async () => {
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
        json: async () => ({ code: 'OK', data: { score: 10, totalScore: 10, items: [] } })
      })
    vi.stubGlobal('fetch', fetchMock)

    const preview = await previewImport('题目: Java 中 int 默认值是多少？')
    const generated = await generateKnowledgeQuestions('HashMap 默认负载因子是 0.75。')
    await submitPractice([{ questionId: 1, answer: 'A' }])

    expect(preview[0].title).toBe('Java 中 int 默认值是多少？')
    expect(generated[0].title).toBe('HashMap 默认负载因子是多少？')
    expect(fetchMock).toHaveBeenCalledWith('/api/imports/preview', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/imports/knowledge/generate', expect.objectContaining({ method: 'POST' }))
    expect(fetchMock).toHaveBeenCalledWith('/api/practice/submit', expect.objectContaining({ method: 'POST' }))
  })
})
