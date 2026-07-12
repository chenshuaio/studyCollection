import { clearCurrentUser, getCurrentUser } from './session'

type ApiResponse<T> = {
  code: string
  message: string
  data: T
}

export type LoginPayload = {
  username: string
  password: string
}

export type LoginResult = {
  token: string
  userId: number
  username: string
  role: string
  displayName: string
}

export type RegisterPayload = {
  username: string
  password: string
  displayName: string
}

export type RegisterResult = {
  token: string
  userId: number
  username: string
  displayName: string
  role: string
}

export type UserSummary = {
  id: number
  username: string
  displayName: string
  role: string
}

export type KnowledgePoint = {
  id: number
  name: string
  description: string
  enabled: boolean
}

export type CreateKnowledgePointPayload = {
  name: string
  description: string
}

export type QuestionPayload = {
  title: string
  type: string
  difficulty: string
  knowledgePoint: string
  answer: string
  analysis: string
}

export type Question = QuestionPayload & {
  id: number
}

export type PendingQuestionPayload = QuestionPayload

export type PendingQuestion = PendingQuestionPayload & {
  id: number
  submitterUserId: number
  status: string
}

export type QuestionSearchParams = {
  keyword?: string
  knowledgePoint?: string
  difficulty?: string
  type?: string
}

export type PreviewQuestion = QuestionPayload

type ImportPreviewResult = {
  questions: PreviewQuestion[]
}

type GeneratedQuestionBank = {
  questions: QuestionPayload[]
}

export type PracticeAnswer = {
  questionId: number
  answer: string
}

export type PracticeResult = {
  score: number
  totalScore: number
  items: Array<{
    questionId: number
    submittedAnswer: string
    correctAnswer: string
    autoGraded: boolean
    correct: boolean | null
    score: number
    analysis: string
  }>
}

export type PracticeStats = {
  userId: number
  answeredQuestionCount: number
  gradedQuestionCount: number
  correctQuestionCount: number
}

export type RecentPracticeSummary = {
  referenceId: string
  attemptedAt: string
  answeredQuestionCount: number
  gradedQuestionCount: number
  correctQuestionCount: number
  accuracy: number
  knowledgePoints: string[]
}

export type PracticeGeneratePayload = {
  knowledgePoint?: string
  difficulty?: string
  type?: string
  count: number
}

export type GeneratedPracticeQuestion = {
  id: number
  title: string
  type: string
  difficulty: string
  knowledgePoint: string
}

export type GeneratedPractice = {
  requestedCount: number
  actualCount: number
  questions: GeneratedPracticeQuestion[]
}

export type QuestionFeedbackPayload = {
  questionId: number
  type: string
  content: string
  submittedAnswer?: string
  sourceContext?: 'PRACTICE' | 'EXAM' | 'MISTAKE_BOOK' | 'QUESTION_DETAIL' | 'UNKNOWN'
  sourceReference?: string
}

export type QuestionFeedback = QuestionFeedbackPayload & {
  id: number
  userId: number
  status: string
  createdAt?: string
  reviewedBy?: number | null
  reviewNote?: string
  reviewedAt?: string | null
}

export type QuestionFeedbackGroup = {
  questionId: number
  questionTitle: string
  questionType: string
  difficulty: string
  knowledgePoint: string
  currentAnswer: string
  currentAnalysis: string
  questionSource: string
  type: string
  feedbackCount: number
  latestAt: string
  items: QuestionFeedback[]
}

export type AcceptFeedbackPayload = {
  changeSummary: string
  reviewNote: string
  correctedTitle?: string
  correctedType?: string
  correctedDifficulty?: string
  correctedKnowledgePoint?: string
  correctedAnswer?: string
  correctedAnalysis?: string
}

export type ReviewFeedbackPayload = {
  reviewNote: string
}

export type AcceptFeedbackGroupPayload = AcceptFeedbackPayload & {
  feedbackIds: number[]
}

export type QuestionSnapshot = {
  id: number
  title: string
  type: string
  difficulty: string
  knowledgePoint: string
  answer: string
  analysis: string
  source: string
}

export type QuestionRevision = {
  id: number
  questionId: number
  feedbackId: number
  adminUserId: number
  changeSummary: string
  reviewNote: string
  relatedFeedbackIds?: number[]
  beforeQuestion?: QuestionSnapshot
  afterQuestion?: QuestionSnapshot
  scoringAffected?: boolean
  revisedAt?: string
}

export type CustomExamPayload = {
  name: string
  durationMinutes: number
  questionIds: number[]
}

export type ExamQuestion = {
  id: number
  title: string
  type: string
  difficulty: string
  knowledgePoint: string
  submittedAnswer: string
  autoGraded: boolean
  correct: boolean | null
  score: number
  correctAnswer: string
  analysis: string
  options?: Array<{
    value: string
    label: string
  }>
}

export type ExamSession = {
  id: number
  name: string
  durationMinutes: number
  status: 'IN_PROGRESS' | 'SUBMITTED'
  startedAt: string
  expiresAt: string
  submittedAt: string | null
  remainingSeconds: number
  score: number | null
  totalScore: number | null
  questions: ExamQuestion[]
}

export type ExamSummary = {
  id: number
  name: string
  durationMinutes: number
  status: 'IN_PROGRESS' | 'SUBMITTED'
  questionCount: number
  answeredCount: number
  startedAt: string
  expiresAt: string
  submittedAt: string | null
  score: number | null
  totalScore: number | null
}

export type CustomExamPaper = ExamSession

export type LearningReportPayload = {
  mode: 'ONLINE_MODEL' | 'OFFLINE_RULES'
  revisedQuestionPolicy: 'EXCLUDE_REVISED' | 'RECALCULATE_REVISED'
}

export type PerformanceBreakdown = {
  label: string
  answeredQuestionCount: number
  gradedQuestionCount: number
  correctQuestionCount: number
  accuracy: number
}

export type ReportTrendPoint = {
  date: string
  gradedQuestionCount: number
  correctQuestionCount: number
  accuracy: number
}

export type StrengtheningQuestion = {
  id: number
  title: string
  type: string
  difficulty: string
  knowledgePoint: string
}

export type LearningReport = {
  id: number
  createdAt: string
  weakestKnowledgePoint: string
  recommendation: string
  adviceSource: string
  adviceContent: string
  answeredQuestionCount: number
  gradedQuestionCount: number
  correctQuestionCount: number
  accuracy: number
  knowledgePointPerformance: PerformanceBreakdown[]
  questionTypePerformance: PerformanceBreakdown[]
  recentTrend: ReportTrendPoint[]
  strengtheningQuestions: StrengtheningQuestion[]
  revisionPolicy: string
  revisedAttemptCount: number
}

export type MistakeRecord = {
  userId: number
  questionId: number
  questionTitle: string
  questionType: string
  knowledgePoint: string
  lastSubmittedAnswer: string
  sourceContext: string
  status: string
  wrongCount: number
  firstWrongAt: string
  lastWrongAt: string
}

export type RecordMistakePayload = {
  questionId: number
  submittedAnswer: string
  sourceContext: 'PRACTICE' | 'EXAM' | 'MISTAKE_RETRY'
}

export type MistakeFilters = {
  knowledgePoint?: string
  questionType?: string
  status?: string
  wrongFrom?: string
  wrongTo?: string
}

export type UpdateMistakeStatusPayload = {
  questionId: number
  status: string
}

async function parseApiResponse<T>(response: Response) {
  let payload: ApiResponse<T> | null = null
  try {
    payload = (await response.json()) as ApiResponse<T>
  } catch {
    payload = null
  }
  if (!response.ok) {
    if (response.status === 401) {
      clearCurrentUser()
    }
    throw new Error(payload?.message ?? `请求失败：${response.status}`)
  }
  if (!payload) {
    throw new Error('响应数据格式错误')
  }
  if (payload.code !== 'OK') {
    throw new Error(payload.message)
  }
  return payload.data
}

async function request<T>(path: string, options: RequestInit = {}) {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  }
  if (options.headers) {
    new Headers(options.headers).forEach((value, key) => {
      headers[key] = value
    })
  }
  const token = getCurrentUser()?.token
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  const response = await fetch(`/api${path}`, {
    ...options,
    headers
  })
  return parseApiResponse<T>(response)
}

function post<T>(path: string, body: unknown) {
  return request<T>(path, {
    method: 'POST',
    body: JSON.stringify(body)
  })
}

function put<T>(path: string, body: unknown) {
  return request<T>(path, {
    method: 'PUT',
    body: JSON.stringify(body)
  })
}

export function login(payload: LoginPayload) {
  return post<LoginResult>('/auth/login', payload)
}

export function register(payload: RegisterPayload) {
  return post<RegisterResult>('/auth/register', payload)
}

export function listUsers() {
  return request<UserSummary[]>('/users', { method: 'GET' })
}

export function listKnowledgePoints() {
  return request<KnowledgePoint[]>('/knowledge-points', { method: 'GET' })
}

export function createKnowledgePoint(payload: CreateKnowledgePointPayload) {
  return post<KnowledgePoint>('/knowledge-points', payload)
}

export function disableKnowledgePoint(id: number) {
  return post<KnowledgePoint>(`/knowledge-points/${id}/disable`, {})
}

export function createQuestion(payload: QuestionPayload) {
  return post<Question>('/questions', payload)
}

export function deleteQuestion(id: number) {
  return request<number>(`/questions/${id}`, { method: 'DELETE' })
}

export function submitPendingQuestion(payload: PendingQuestionPayload) {
  return post<PendingQuestion>('/questions/pending', payload)
}

export function listPendingQuestions() {
  return request<PendingQuestion[]>('/questions/pending', { method: 'GET' })
}

export function approvePendingQuestion(id: number) {
  return post<Question>(`/questions/pending/${id}/approve`, {})
}

export function rejectPendingQuestion(id: number) {
  return post<PendingQuestion>(`/questions/pending/${id}/reject`, {})
}

export function searchQuestions(filters: QuestionSearchParams = {}) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value) {
      params.set(key, value)
    }
  })
  const query = params.toString()
  return request<Question[]>(query ? `/questions?${query}` : '/questions', { method: 'GET' })
}

export function previewImport(content: string) {
  return post<ImportPreviewResult>('/imports/preview', { content }).then((preview) => preview.questions)
}

export function generateKnowledgeQuestions(content: string) {
  return post<GeneratedQuestionBank>('/imports/knowledge/generate', { content }).then((bank) => bank.questions)
}

export function uploadKnowledgeFile(file: File) {
  const body = new FormData()
  body.append('file', file)
  return fetch('/api/imports/knowledge/upload', {
    method: 'POST',
    headers: authorizationHeaders(),
    body
  })
    .then((response) => parseApiResponse<GeneratedQuestionBank>(response))
    .then((bank) => bank.questions)
}

export function uploadQuestionFile(file: File) {
  const body = new FormData()
  body.append('file', file)
  return fetch('/api/imports/questions/upload', {
    method: 'POST',
    headers: authorizationHeaders(),
    body
  })
    .then((response) => parseApiResponse<ImportPreviewResult>(response))
    .then((preview) => preview.questions)
}

export function submitPractice(answers: PracticeAnswer[]) {
  return post<PracticeResult>('/practice/submit', { answers })
}

export function submitUserPractice(answers: PracticeAnswer[]) {
  return post<PracticeResult>('/practice/submit', { answers })
}

export function generatePractice(payload: PracticeGeneratePayload) {
  const body: PracticeGeneratePayload = { count: payload.count }
  if (payload.knowledgePoint) {
    body.knowledgePoint = payload.knowledgePoint
  }
  if (payload.difficulty) {
    body.difficulty = payload.difficulty
  }
  if (payload.type) {
    body.type = payload.type
  }
  return post<GeneratedPractice>('/practice/generate', body)
}

export function getPracticeStats() {
  return request<PracticeStats>('/practice/stats', { method: 'GET' })
}

export function getRecentPractices(limit = 3) {
  return request<RecentPracticeSummary[]>(`/practice/recent?limit=${limit}`, { method: 'GET' })
}

export function submitQuestionFeedback(payload: QuestionFeedbackPayload) {
  return post<QuestionFeedback>('/questions/feedback', payload)
}

export function listPendingFeedback() {
  return request<QuestionFeedback[]>('/questions/feedback/pending', { method: 'GET' })
}

export function listPendingFeedbackGroups() {
  return request<QuestionFeedbackGroup[]>('/questions/feedback/pending/groups', { method: 'GET' })
}

export function listUserFeedback() {
  return request<QuestionFeedback[]>('/questions/feedback', { method: 'GET' })
}

export function acceptQuestionFeedback(feedbackId: number, payload: AcceptFeedbackPayload) {
  return post<QuestionRevision>(`/questions/feedback/${feedbackId}/accept`, payload)
}

export function acceptQuestionFeedbackGroup(payload: AcceptFeedbackGroupPayload) {
  return post<QuestionRevision>('/questions/feedback/groups/accept', payload)
}

export function listQuestionRevisions(questionId: number) {
  return request<QuestionRevision[]>(`/questions/feedback/revisions/${questionId}`, { method: 'GET' })
}

export function rejectQuestionFeedback(feedbackId: number, payload: ReviewFeedbackPayload) {
  return post<QuestionFeedback>(`/questions/feedback/${feedbackId}/reject`, payload)
}

export function markFeedbackNeedsReview(feedbackId: number, payload: ReviewFeedbackPayload) {
  return post<QuestionFeedback>(`/questions/feedback/${feedbackId}/needs-review`, payload)
}

export function composeCustomExam(payload: CustomExamPayload) {
  return post<CustomExamPaper>('/exams/custom', payload)
}

export function listExamSessions() {
  return request<ExamSummary[]>('/exams', { method: 'GET' })
}

export function getExamSession(sessionId: number) {
  return request<ExamSession>(`/exams/${sessionId}`, { method: 'GET' })
}

export function saveExamAnswer(sessionId: number, questionId: number, answer: string) {
  return put<ExamSession>(`/exams/${sessionId}/answers/${questionId}`, { answer })
}

export function submitExamSession(sessionId: number) {
  return post<ExamSession>(`/exams/${sessionId}/submit`, {})
}

export function generateLearningReport(payload: LearningReportPayload) {
  return post<LearningReport>('/reports/learning', payload)
}

export function listLearningReports() {
  return request<LearningReport[]>('/reports/learning', { method: 'GET' })
}

export function recordMistake(payload: RecordMistakePayload) {
  return post<MistakeRecord>('/mistakes', payload)
}

export function updateMistakeStatus(payload: UpdateMistakeStatusPayload) {
  return post<MistakeRecord>('/mistakes/status', payload)
}

export function listMistakes(filters: MistakeFilters = {}) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value) params.set(key, value)
  })
  const query = params.toString()
  return request<MistakeRecord[]>(`/mistakes${query ? `?${query}` : ''}`, { method: 'GET' })
}

function authorizationHeaders() {
  const token = getCurrentUser()?.token
  return token ? { Authorization: `Bearer ${token}` } : undefined
}
