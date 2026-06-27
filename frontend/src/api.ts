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

export type PreviewQuestion = {
  title: string
  answer: string
  knowledgePoint: string
  difficulty: string
}

type ImportPreviewResult = {
  questions: PreviewQuestion[]
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
    correct: boolean
    score: number
    analysis: string
  }>
}

async function request<T>(path: string, options: RequestInit = {}) {
  const response = await fetch(`/api${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  })
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }
  const payload = (await response.json()) as ApiResponse<T>
  if (payload.code !== 'OK') {
    throw new Error(payload.message)
  }
  return payload.data
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

export function searchQuestions() {
  const params = new URLSearchParams({
    knowledgePoint: '集合框架',
    difficulty: 'INTERMEDIATE',
    type: 'SINGLE_CHOICE'
  })
  return request<Question[]>(`/questions?${params.toString()}`)
}

export function previewImport(content: string) {
  return post<ImportPreviewResult>('/imports/preview', { content }).then((preview) => preview.questions)
}

export function submitPractice(answers: PracticeAnswer[]) {
  return post<PracticeResult>('/practice/submit', { answers })
}
