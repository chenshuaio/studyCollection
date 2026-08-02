import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ImportPage from './ImportPage.vue'
import { submitPendingQuestion, uploadKnowledgeFile, uploadQuestionFile, type QuestionPayload } from '../api'

vi.mock('../api', () => ({
  createQuestion: vi.fn(),
  generateKnowledgeQuestions: vi.fn(),
  previewImport: vi.fn(),
  submitPendingQuestion: vi.fn(),
  uploadKnowledgeFile: vi.fn(),
  uploadQuestionFile: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('ImportPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
  })

  it('renders import preview and knowledge generation workflow', () => {
    const wrapper = mount(ImportPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    expect(wrapper.text()).toContain('题目导入')
    expect(wrapper.text()).toContain('个人题库')
    expect(wrapper.text()).toContain('申请公开')
    expect((wrapper.get('input[value="PERSONAL"]').element as HTMLInputElement).checked).toBe(true)
    expect(wrapper.text()).toContain('结构化题目导入')
    expect(wrapper.text()).toContain('解析预览')
    expect(wrapper.text()).toContain('提交审核')
    expect(wrapper.text()).toContain('学习内容生成题库')
    expect(wrapper.text()).toContain('上传 Java 学习资料')
    expect(wrapper.text()).toContain('分析生成题库')
    const questionFileInput = wrapper.find('input[aria-label="上传结构化题目文件"]')
    expect(questionFileInput.exists()).toBe(true)
    expect(questionFileInput.attributes('accept')).toContain('.json')
    expect(questionFileInput.attributes('accept')).toContain('.xlsx')
    const knowledgeEditor = wrapper.find('textarea[aria-label="Java 学习知识内容"]').element as HTMLTextAreaElement
    expect(knowledgeEditor.value).toContain('HashMap 默认负载因子是 0.75')
    const fileInput = wrapper.find('input[aria-label="上传 Java 学习资料"]')
    expect(fileInput.exists()).toBe(true)
    expect(wrapper.text()).toContain('PDF / DOCX / XLSX / CSV / MD / TXT')
    expect(wrapper.text()).toContain('最大 10 MB')
    expect(fileInput.attributes('accept')).toContain('.txt')
    expect(fileInput.attributes('accept')).toContain('.md')
    expect(fileInput.attributes('accept')).toContain('.csv')
    expect(fileInput.attributes('accept')).toContain('.xlsx')
    expect(fileInput.attributes('accept')).toContain('.docx')
    expect(fileInput.attributes('accept')).toContain('.pdf')
    expect(fileInput.attributes('accept')).toContain('application/pdf')
  })

  it('submits parsed preview questions for administrator review', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'user-token', userId: 7, username: 'alice', displayName: 'Alice', role: 'USER' })
    )
    vi.mocked(submitPendingQuestion).mockResolvedValue({
      id: 1,
      submitterUserId: 7,
      title: 'Java 中 int 默认值是多少？',
      type: 'SINGLE_CHOICE',
      difficulty: 'BEGINNER',
      knowledgePoint: 'Java 基础',
      answer: 'A',
      analysis: '由导入预览提交审核',
      targetScope: 'PUBLIC',
      status: 'PENDING'
    })

    const wrapper = mount(ImportPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await wrapper.get('input[value="PUBLIC"]').setValue(true)
    await wrapper.get('textarea[aria-label="解析预览第 1 题题干"]').setValue('说明 Java 中 final 的作用。')
    await wrapper.get('select[aria-label="解析预览第 1 题题型"]').setValue('SHORT_ANSWER')
    await wrapper.get('select[aria-label="解析预览第 1 题难度"]').setValue('INTERMEDIATE')
    await wrapper.get('input[aria-label="解析预览第 1 题知识点"]').setValue('Java 基础')
    await wrapper.get('textarea[aria-label="解析预览第 1 题答案"]').setValue('可修饰类、方法和变量。')
    await wrapper.get('textarea[aria-label="解析预览第 1 题解析"]').setValue('根据修饰目标含义不同。')
    await wrapper.get('button[aria-label="提交预览题审核"]').trigger('click')
    await flushPromises()

    expect(submitPendingQuestion).toHaveBeenCalledWith({
      title: '说明 Java 中 final 的作用。',
      type: 'SHORT_ANSWER',
      difficulty: 'INTERMEDIATE',
      knowledgePoint: 'Java 基础',
      answer: '可修饰类、方法和变量。',
      analysis: '根据修饰目标含义不同。',
      targetScope: 'PUBLIC'
    })
    expect(wrapper.text()).toContain('已提交管理员审核')
    expect(wrapper.text()).toContain('申请公开')
  })

  it('previews a structured question file without submitting it', async () => {
    vi.mocked(uploadQuestionFile).mockResolvedValue([
      {
        title: '下列哪个是 Java 关键字？\nA. class\nB. hello',
        type: 'SINGLE_CHOICE',
        difficulty: 'BEGINNER',
        knowledgePoint: 'Java 基础',
        answer: 'A',
        analysis: 'class 是关键字。'
      }
    ])
    const wrapper = mount(ImportPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    const file = new File(['[]'], 'questions.json', { type: 'application/json' })
    const input = wrapper.get('input[aria-label="上传结构化题目文件"]')
    Object.defineProperty(input.element, 'files', { configurable: true, value: [file] })

    await input.trigger('change')
    await flushPromises()

    expect(uploadQuestionFile).toHaveBeenCalledWith(file)
    expect(submitPendingQuestion).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('解析 1 道题')
    expect((wrapper.get('textarea[aria-label="解析预览第 1 题题干"]').element as HTMLTextAreaElement).value)
      .toContain('Java 关键字')
  })

  it('adds and removes editable preview rows', async () => {
    const wrapper = mount(ImportPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })

    await wrapper.get('button[aria-label="新增解析预览题目"]').trigger('click')
    expect(wrapper.findAll('textarea[data-draft-field="title"]')).toHaveLength(2)
    await wrapper.get('button[aria-label="删除解析预览第 1 题"]').trigger('click')
    expect(wrapper.findAll('textarea[data-draft-field="title"]')).toHaveLength(1)
  })

  it('previews questions generated from a PDF without submitting them for review', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'user-token', userId: 7, username: 'alice', displayName: 'Alice', role: 'USER' })
    )
    vi.mocked(uploadKnowledgeFile).mockResolvedValue([
      {
        title: 'HashMap 的默认负载因子是多少？',
        type: 'SHORT_ANSWER',
        difficulty: 'INTERMEDIATE',
        knowledgePoint: 'Java 集合',
        answer: '0.75',
        analysis: '上传资料生成的预览题'
      }
    ])
    vi.mocked(submitPendingQuestion).mockResolvedValue({
      id: 9,
      submitterUserId: 7,
      title: 'HashMap 的默认负载因子是多少？',
      type: 'SHORT_ANSWER',
      difficulty: 'INTERMEDIATE',
      knowledgePoint: 'Java 集合',
      answer: '0.75',
      analysis: '上传资料生成的预览题',
      targetScope: 'PERSONAL',
      status: 'PENDING'
    })

    const wrapper = mount(ImportPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })
    const file = new File(['HashMap 默认负载因子是 0.75。'], 'hashmap.pdf', {
      type: 'application/pdf'
    })
    const fileInput = wrapper.find('input[aria-label="上传 Java 学习资料"]')
    Object.defineProperty(fileInput.element, 'files', {
      configurable: true,
      value: [file]
    })

    await fileInput.trigger('change')
    await flushPromises()

    expect(uploadKnowledgeFile).toHaveBeenCalledWith(file)
    expect(submitPendingQuestion).not.toHaveBeenCalled()
    expect((wrapper.get('textarea[aria-label="生成题库预览第 1 题题干"]').element as HTMLTextAreaElement).value)
      .toContain('HashMap 的默认负载因子是多少？')
    expect(wrapper.text()).toContain('已从 hashmap.pdf 生成 1 道题，请预览后提交审核。')

    await wrapper.get('textarea[aria-label="生成题库预览第 1 题解析"]').setValue('用户已修正的 HashMap 解析')

    await wrapper.find('button[aria-label="提交生成题审核"]').trigger('click')
    await flushPromises()

    expect(submitPendingQuestion).toHaveBeenCalledWith(expect.objectContaining({
      title: 'HashMap 的默认负载因子是多少？',
      knowledgePoint: 'Java 集合',
      analysis: '用户已修正的 HashMap 解析',
      targetScope: 'PERSONAL'
    }))
  })

  it('rejects a PDF larger than 10 MB before uploading it', async () => {
    const wrapper = mount(ImportPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    const file = new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'oversized.pdf', {
      type: 'application/pdf'
    })
    const fileInput = wrapper.get('input[aria-label="上传 Java 学习资料"]')
    Object.defineProperty(fileInput.element, 'files', { configurable: true, value: [file] })

    await fileInput.trigger('change')
    await flushPromises()

    expect(uploadKnowledgeFile).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('学习资料文件不能超过 10 MB。')
  })

  it('rejects an unsupported knowledge file before uploading it', async () => {
    const wrapper = mount(ImportPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    const file = new File(['not a learning document'], 'notes.exe', {
      type: 'application/vnd.microsoft.portable-executable'
    })
    const fileInput = wrapper.get('input[aria-label="上传 Java 学习资料"]')
    Object.defineProperty(fileInput.element, 'files', { configurable: true, value: [file] })

    await fileInput.trigger('change')
    await flushPromises()

    expect(uploadKnowledgeFile).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('学习资料仅支持 PDF、DOCX、XLSX、CSV、MD 和 TXT 格式。')
  })

  it('rejects an extensionless knowledge file whose name matches an allowed extension', async () => {
    const wrapper = mount(ImportPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    const file = new File(['content'], 'pdf', { type: 'application/pdf' })
    const fileInput = wrapper.get('input[aria-label="上传 Java 学习资料"]')
    Object.defineProperty(fileInput.element, 'files', { configurable: true, value: [file] })

    await fileInput.trigger('change')
    await flushPromises()

    expect(uploadKnowledgeFile).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('学习资料仅支持 PDF、DOCX、XLSX、CSV、MD 和 TXT 格式。')
  })

  it('keeps an existing generated preview when a later PDF upload fails', async () => {
    vi.mocked(uploadKnowledgeFile)
      .mockResolvedValueOnce([
        {
          title: '保留的预览题',
          type: 'SHORT_ANSWER',
          difficulty: 'BEGINNER',
          knowledgePoint: 'Java 基础',
          answer: '保留的答案',
          analysis: '保留的解析'
        }
      ])
      .mockRejectedValueOnce(new Error('扫描版 PDF 暂不支持，请上传可复制文本的 PDF。'))
    const wrapper = mount(ImportPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    const fileInput = wrapper.get('input[aria-label="上传 Java 学习资料"]')
    const firstFile = new File(['Java 基础'], 'first.pdf', { type: 'application/pdf' })
    Object.defineProperty(fileInput.element, 'files', { configurable: true, value: [firstFile] })

    await fileInput.trigger('change')
    await flushPromises()

    const scannedFile = new File(['scanned content'], 'scanned.pdf', { type: 'application/pdf' })
    Object.defineProperty(fileInput.element, 'files', { configurable: true, value: [scannedFile] })
    await fileInput.trigger('change')
    await flushPromises()

    expect(uploadKnowledgeFile).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('扫描版 PDF 暂不支持，请上传可复制文本的 PDF。')
    expect((wrapper.get('textarea[aria-label="生成题库预览第 1 题题干"]').element as HTMLTextAreaElement).value)
      .toBe('保留的预览题')
  })

  it('shows the PDF parsing status while the upload request is pending', async () => {
    let resolveUpload: ((questions: QuestionPayload[]) => void) | undefined
    vi.mocked(uploadKnowledgeFile).mockImplementation(() => new Promise<QuestionPayload[]>((resolve) => {
      resolveUpload = resolve
    }))
    const wrapper = mount(ImportPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    const file = new File(['HashMap'], 'hashmap.pdf', { type: 'application/pdf' })
    const fileInput = wrapper.get('input[aria-label="上传 Java 学习资料"]')
    Object.defineProperty(fileInput.element, 'files', { configurable: true, value: [file] })

    await fileInput.trigger('change')

    expect(wrapper.text()).toContain('正在解析 hashmap.pdf...')

    resolveUpload?.([])
    await flushPromises()
  })
})
