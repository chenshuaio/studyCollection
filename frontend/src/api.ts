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
  role: string
  displayName: string
}

export type RegisterPayload = {
  username: string
  password: string
  displayName: string
}

export type RegisterResult = {
  id: number
  username: string
  displayName: string
  role: string
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

export type PendingQuestionPayload = QuestionPayload & {
  submitterUserId: number
}

export type PendingQuestion = PendingQuestionPayload & {
  id: number
  status: string
}

export type QuestionSearchParams = {
  keyword?: string
  knowledgePoint?: string
  difficulty?: string
  type?: string
}

export type PreviewQuestion = {
  title: string
  answer: string
  knowledgePoint: string
  difficulty: string
}

type ImportPreviewResult = {
  questions: PreviewQuestion[]
}

type GeneratedQuestionBank = {
  questions: QuestionPayload[]
}

export type PracticeAnswer = {
  questionId: number
  answer: string
  correctAnswer?: string
  analysis?: string
}

export type PracticeResult = {
  score: number
  totalScore: number
  items: Array<{
    questionId: number
    submittedAnswer: string
    correctAnswer: string
    correct: boolean
    score: number
    analysis: string
  }>
}

export type PracticeStats = {
  userId: number
  answeredQuestionCount: number
  correctQuestionCount: number
}

export type QuestionFeedbackPayload = {
  userId: number
  questionId: number
  type: string
  content: string
}

export type QuestionFeedback = QuestionFeedbackPayload & {
  id: number
  status: string
}

export type AcceptFeedbackPayload = {
  adminUserId: number
  changeSummary: string
  reviewNote: string
}

export type ReviewFeedbackPayload = {
  adminUserId: number
  reviewNote: string
}

export type QuestionRevision = AcceptFeedbackPayload & {
  id: number
  questionId: number
  feedbackId: number
}

export type CustomExamPayload = {
  name: string
  durationMinutes: number
  questionIds: number[]
}

export type CustomExamPaper = CustomExamPayload

export type LearningReportPayload = {
  mode: 'ONLINE_MODEL' | 'OFFLINE_RULES'
  results: Array<{
    knowledgePoint: string
    correct: boolean
  }>
}

export type LearningReport = {
  weakestKnowledgePoint: string
  recommendation: string
  adviceSource: string
  adviceContent: string
}

export type MistakeRecord = {
  userId: number
  questionId: number
  questionTitle: string
  knowledgePoint: string
  status: string
}

async function parseApiResponse<T>(response: Response) {
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }
  const payload = (await response.json()) as ApiResponse<T>
  if (payload.code !== 'OK') {
    throw new Error(payload.message)
  }
  return payload.data
}

async function request<T>(path: string, options: RequestInit = {}) {
  const response = await fetch(`/api${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  })
  return parseApiResponse<T>(response)
}

function post<T>(path: string, body: unknown) {
  return request<T>(path, {
    method: 'POST',
    body: JSON.stringify(body)
  })
}

export function login(payload: LoginPayload) {
  return post<LoginResult>('/auth/login', payload)
}

export function register(payload: RegisterPayload) {
  return post<RegisterResult>('/auth/register', payload)
}

export function createQuestion(payload: QuestionPayload) {
  return post<Question>('/questions', payload)
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
    body
  })
    .then((response) => parseApiResponse<GeneratedQuestionBank>(response))
    .then((bank) => bank.questions)
}

export function submitPractice(answers: PracticeAnswer[]) {
  return post<PracticeResult>('/practice/submit', { answers })
}

export function submitUserPractice(userId: number, answers: PracticeAnswer[]) {
  return post<PracticeResult>('/practice/submit', { userId, answers })
}

export function getPracticeStats(userId: number) {
  const params = new URLSearchParams({ userId: String(userId) })
  return request<PracticeStats>(`/practice/stats?${params.toString()}`, { method: 'GET' })
}

export function submitQuestionFeedback(payload: QuestionFeedbackPayload) {
  return post<QuestionFeedback>('/questions/feedback', payload)
}

export function listPendingFeedback() {
  return request<QuestionFeedback[]>('/questions/feedback/pending', { method: 'GET' })
}

export function listUserFeedback(userId: number) {
  const params = new URLSearchParams({ userId: String(userId) })
  return request<QuestionFeedback[]>(`/questions/feedback?${params.toString()}`, { method: 'GET' })
}

export function acceptQuestionFeedback(feedbackId: number, payload: AcceptFeedbackPayload) {
  return post<QuestionRevision>(`/questions/feedback/${feedbackId}/accept`, payload)
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

export function generateLearningReport(payload: LearningReportPayload) {
  return post<LearningReport>('/reports/learning', payload)
}

export function recordMistake(payload: MistakeRecord) {
  return post<MistakeRecord>('/mistakes', payload)
}

export function listMistakes(userId: number) {
  const params = new URLSearchParams({ userId: String(userId) })
  return request<MistakeRecord[]>(`/mistakes?${params.toString()}`, { method: 'GET' })
}
