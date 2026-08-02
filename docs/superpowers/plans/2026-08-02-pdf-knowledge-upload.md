# PDF 学习资料上传实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 支持用户上传不超过 10 MB、含文本层的 PDF 格式 Java 学习资料，生成可编辑题目预览，并对扫描版、损坏、加密和超大 PDF 返回明确中文提示。

**Architecture:** 保留现有 `/imports/knowledge/upload` 与“学习内容生成题库”入口。后端由 `KnowledgeFileTextExtractor` 统一执行扩展名、大小和 PDF 文本检查，再将内容交给现有规则生成器；前端在发请求前做同样的轻量校验，并继续复用现有预览和管理员审核流程。

**Tech Stack:** Java 17、Spring Boot 3.3、Apache PDFBox 2.0.30、JUnit 5、Vue 3、TypeScript、Vitest、MySQL、PowerShell、Cloudflare Tunnel。

---

## 文件结构

- `backend/import-service/src/main/java/com/studycollection/importer/parser/KnowledgeFileTextExtractor.java`：资料文件白名单、10 MB 限制、PDF 文本提取和可理解错误。
- `backend/import-service/src/test/java/com/studycollection/importer/parser/KnowledgeFileTextExtractorTest.java`：资料提取器的 PDF 成功与边界测试。
- `backend/local-app/src/test/java/com/studycollection/local/LocalStudyCollectionApplicationTest.java`：验证损坏 PDF 通过真实 HTTP 接口返回统一 400。
- `frontend/src/pages/ImportPage.vue`：显示 PDF 支持、上传状态及前端文件校验。
- `frontend/src/pages/ImportPage.test.ts`：PDF 选择、大小/格式拦截、预览保留和错误提示测试。
- `README.md`、`docs/api/local-flow.md`：记录 PDF 范围、限制和扫描版处理方式。

### 任务 1：加固后端 PDF 提取和 HTTP 错误响应

**Files:**
- Create: `backend/import-service/src/test/java/com/studycollection/importer/parser/KnowledgeFileTextExtractorTest.java`
- Modify: `backend/import-service/src/main/java/com/studycollection/importer/parser/KnowledgeFileTextExtractor.java`
- Modify: `backend/local-app/src/test/java/com/studycollection/local/LocalStudyCollectionApplicationTest.java`
- Test: `backend/import-service/src/test/java/com/studycollection/importer/api/KnowledgeGenerateControllerTest.java`

- [ ] **Step 1: 写资料提取器失败测试**

创建 `KnowledgeFileTextExtractorTest`，覆盖文本型 PDF、空白 PDF、损坏 PDF、超过 10 MB 和不支持扩展名：

```java
package com.studycollection.importer.parser;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeFileTextExtractorTest {
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024;
    private final KnowledgeFileTextExtractor extractor = new KnowledgeFileTextExtractor();

    @Test
    void extractsTextFromPdfWithTextLayer() throws Exception {
        String text = extractor.extract(file("hashmap.pdf", "application/pdf", pdfBytes("HashMap default load factor is 0.75.")));

        assertThat(text).contains("HashMap default load factor is 0.75");
    }

    @Test
    void rejectsPdfWithoutExtractableText() throws Exception {
        assertThatThrownBy(() -> extractor.extract(file("scan.pdf", "application/pdf", pdfBytes(null))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PDF 未提取到可用文字，请上传可复制文字的 PDF，扫描版暂不支持。");
    }

    @Test
    void rejectsBrokenPdfWithReadableMessage() {
        assertThatThrownBy(() -> extractor.extract(file(
                "broken.pdf",
                "application/pdf",
                "not-a-pdf".getBytes(StandardCharsets.UTF_8)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PDF 无法解析，请确认文件未损坏且未加密。");
    }

    @Test
    void rejectsEncryptedPdfWithReadableMessage() throws Exception {
        assertThatThrownBy(() -> extractor.extract(file(
                "encrypted.pdf",
                "application/pdf",
                encryptedPdfBytes()
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PDF 无法解析，请确认文件未损坏且未加密。");
    }

    @Test
    void rejectsEmptyKnowledgeFile() {
        assertThatThrownBy(() -> extractor.extract(file("empty.pdf", "application/pdf", new byte[0])))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("学习资料文件不能为空。");
    }

    @Test
    void rejectsKnowledgeFileLargerThanTenMegabytes() {
        assertThatThrownBy(() -> extractor.extract(file(
                "large.pdf",
                "application/pdf",
                new byte[MAX_FILE_SIZE + 1]
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("学习资料文件不能超过 10 MB。");
    }

    @Test
    void rejectsUnsupportedKnowledgeFileExtension() {
        assertThatThrownBy(() -> extractor.extract(file(
                "notes.exe",
                "application/octet-stream",
                "HashMap".getBytes(StandardCharsets.UTF_8)
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("学习资料仅支持 PDF、DOCX、XLSX、CSV、MD 和 TXT 格式。");
    }

    private MockMultipartFile file(String name, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", name, contentType, bytes);
    }

    private byte[] pdfBytes(String text) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            if (text != null) {
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(PDType1Font.HELVETICA, 12);
                    content.newLineAtOffset(48, 720);
                    content.showText(text);
                    content.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    private byte[] encryptedPdfBytes() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            AccessPermission accessPermission = new AccessPermission();
            StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    "owner-password",
                    "user-password",
                    accessPermission
            );
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
            document.save(output);
            return output.toByteArray();
        }
    }
}
```

- [ ] **Step 2: 写真实 HTTP 失败测试**

在 `LocalStudyCollectionApplicationTest` 增加以下 imports 和测试，证明损坏 PDF 返回统一业务错误而不是 500：

```java
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
```

```java
@Test
void brokenPdfUploadReturnsUnifiedBadRequest() throws Exception {
    Session user = login("user", "user123");
    HttpHeaders multipartHeaders = new HttpHeaders();
    multipartHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
    multipartHeaders.setBearerAuth(user.token());
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", new ByteArrayResource("not-a-pdf".getBytes(StandardCharsets.UTF_8)) {
        @Override
        public String getFilename() {
            return "broken.pdf";
        }
    });

    ResponseEntity<String> response = restTemplate.exchange(
            url("/imports/knowledge/upload"),
            HttpMethod.POST,
            new HttpEntity<>(body, multipartHeaders),
            String.class
    );

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("\"code\":\"VALIDATION_FAILED\"");
    assertThat(response.getBody()).contains("PDF 无法解析，请确认文件未损坏且未加密。");
}
```

同时增加：

```java
import java.nio.charset.StandardCharsets;
```

- [ ] **Step 3: 运行测试并确认当前实现失败**

Run:

```powershell
Set-Location backend
mvn -pl import-service -Dtest=KnowledgeFileTextExtractorTest test
mvn -pl local-app -am -Dtest=LocalStudyCollectionApplicationTest#brokenPdfUploadReturnsUnifiedBadRequest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 第一条命令至少在空白、损坏、超大和扩展名用例失败；第二条命令收到 500 或非预期错误消息。

- [ ] **Step 4: 实现最小后端校验和 PDF 错误转换**

将 `KnowledgeFileTextExtractor` 的入口改为明确白名单和统一大小限制，保留现有 DOCX/XLSX 提取方法：

```java
private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
private static final String SUPPORTED_FORMAT_MESSAGE =
        "学习资料仅支持 PDF、DOCX、XLSX、CSV、MD 和 TXT 格式。";

public String extract(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) {
        throw new IllegalArgumentException("学习资料文件不能为空。");
    }
    if (file.getSize() > MAX_FILE_SIZE) {
        throw new IllegalArgumentException("学习资料文件不能超过 10 MB。");
    }

    String filename = file.getOriginalFilename() == null
            ? ""
            : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    byte[] bytes = file.getBytes();
    String text;
    if (filename.endsWith(".docx")) {
        text = extractDocx(bytes);
    } else if (filename.endsWith(".pdf")) {
        text = extractPdf(bytes);
    } else if (filename.endsWith(".xlsx")) {
        text = extractXlsx(bytes);
    } else if (filename.endsWith(".txt") || filename.endsWith(".md") || filename.endsWith(".csv")) {
        text = new String(bytes, StandardCharsets.UTF_8);
    } else {
        throw new IllegalArgumentException(SUPPORTED_FORMAT_MESSAGE);
    }

    if (filename.endsWith(".pdf") && text.isBlank()) {
        throw new IllegalArgumentException("PDF 未提取到可用文字，请上传可复制文字的 PDF，扫描版暂不支持。");
    }
    return text;
}

private String extractPdf(byte[] bytes) {
    try (PDDocument document = PDDocument.load(bytes)) {
        return new PDFTextStripper().getText(document).trim();
    } catch (IOException exception) {
        throw new IllegalArgumentException("PDF 无法解析，请确认文件未损坏且未加密。", exception);
    }
}
```

- [ ] **Step 5: 运行后端测试并确认通过**

Run:

```powershell
Set-Location backend
mvn -pl import-service -Dtest=KnowledgeFileTextExtractorTest,KnowledgeGenerateControllerTest test
mvn -pl local-app -am -Dtest=LocalStudyCollectionApplicationTest#brokenPdfUploadReturnsUnifiedBadRequest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: 两条命令均 `BUILD SUCCESS`，新增测试 0 failures、0 errors。

- [ ] **Step 6: 提交后端改动**

```powershell
git add backend/import-service/src/main/java/com/studycollection/importer/parser/KnowledgeFileTextExtractor.java backend/import-service/src/test/java/com/studycollection/importer/parser/KnowledgeFileTextExtractorTest.java backend/local-app/src/test/java/com/studycollection/local/LocalStudyCollectionApplicationTest.java
git commit -m "feat: validate pdf knowledge uploads"
```

### 任务 2：完善前端 PDF 上传体验

**Files:**
- Modify: `frontend/src/pages/ImportPage.test.ts`
- Modify: `frontend/src/pages/ImportPage.vue`

- [ ] **Step 1: 写 PDF 上传和校验失败测试**

在 `ImportPage.test.ts` 中：

1. 将首个渲染测试补充为：

```ts
expect(wrapper.text()).toContain('PDF / DOCX / XLSX / CSV / MD / TXT')
expect(fileInput.attributes('accept')).toContain('application/pdf')
```

2. 新增 PDF 预览测试：

```ts
it('uploads a PDF knowledge file and shows editable preview', async () => {
  vi.mocked(uploadKnowledgeFile).mockResolvedValue([{ 
    title: 'HashMap 默认负载因子是多少？',
    type: 'SINGLE_CHOICE',
    difficulty: 'INTERMEDIATE',
    knowledgePoint: '集合框架',
    answer: 'B',
    analysis: 'PDF 学习资料生成的预览题'
  }])
  const wrapper = mount(ImportPage, {
    global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
  })
  const file = new File(['%PDF-1.4'], 'hashmap.pdf', { type: 'application/pdf' })
  const input = wrapper.get('input[aria-label="上传 Java 学习资料"]')
  Object.defineProperty(input.element, 'files', { configurable: true, value: [file] })

  await input.trigger('change')
  await flushPromises()

  expect(uploadKnowledgeFile).toHaveBeenCalledWith(file)
  expect(submitPendingQuestion).not.toHaveBeenCalled()
  expect(wrapper.text()).toContain('请预览后提交审核')
  expect((wrapper.get('textarea[aria-label="生成题库预览第 1 题题干"]').element as HTMLTextAreaElement).value)
    .toContain('HashMap 默认负载因子')
})
```

3. 新增超大文件和错误保留预览测试：

```ts
it('rejects knowledge files larger than ten megabytes before upload', async () => {
  const wrapper = mount(ImportPage, {
    global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
  })
  const file = new File([new Uint8Array(10 * 1024 * 1024 + 1)], 'large.pdf', { type: 'application/pdf' })
  const input = wrapper.get('input[aria-label="上传 Java 学习资料"]')
  Object.defineProperty(input.element, 'files', { configurable: true, value: [file] })

  await input.trigger('change')

  expect(uploadKnowledgeFile).not.toHaveBeenCalled()
  expect(wrapper.text()).toContain('学习资料文件不能超过 10 MB。')
})

it('rejects unsupported knowledge file extensions before upload', async () => {
  const wrapper = mount(ImportPage, {
    global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
  })
  const file = new File(['binary'], 'notes.exe', { type: 'application/octet-stream' })
  const input = wrapper.get('input[aria-label="上传 Java 学习资料"]')
  Object.defineProperty(input.element, 'files', { configurable: true, value: [file] })

  await input.trigger('change')

  expect(uploadKnowledgeFile).not.toHaveBeenCalled()
  expect(wrapper.text()).toContain('学习资料仅支持 PDF、DOCX、XLSX、CSV、MD 和 TXT 格式。')
})

it('shows PDF parse errors without clearing an existing preview', async () => {
  vi.mocked(uploadKnowledgeFile)
    .mockResolvedValueOnce([{
      title: '保留的预览题', type: 'SHORT_ANSWER', difficulty: 'BEGINNER',
      knowledgePoint: 'Java 综合', answer: '参考答案', analysis: '参考解析'
    }])
    .mockRejectedValueOnce(new Error('PDF 未提取到可用文字，请上传可复制文字的 PDF，扫描版暂不支持。'))
  const wrapper = mount(ImportPage, {
    global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
  })
  const input = wrapper.get('input[aria-label="上传 Java 学习资料"]')

  Object.defineProperty(input.element, 'files', {
    configurable: true,
    value: [new File(['valid'], 'valid.pdf', { type: 'application/pdf' })]
  })
  await input.trigger('change')
  await flushPromises()
  Object.defineProperty(input.element, 'files', {
    configurable: true,
    value: [new File(['blank'], 'scan.pdf', { type: 'application/pdf' })]
  })
  await input.trigger('change')
  await flushPromises()

  expect(wrapper.text()).toContain('PDF 未提取到可用文字')
  expect((wrapper.get('textarea[aria-label="生成题库预览第 1 题题干"]').element as HTMLTextAreaElement).value)
    .toBe('保留的预览题')
})
```

- [ ] **Step 2: 运行页面测试并确认失败**

Run:

```powershell
Set-Location frontend
npm test -- ImportPage.test.ts
```

Expected: 缺少格式说明和 10 MB 前端校验的断言失败。

- [ ] **Step 3: 实现前端格式、大小和状态校验**

在 `ImportPage.vue` 的学习资料上传标签中使用：

```vue
<span>上传 Java 学习资料（PDF / DOCX / XLSX / CSV / MD / TXT，最大 10 MB）</span>
```

在 `<script setup>` 中增加：

```ts
const MAX_KNOWLEDGE_FILE_SIZE = 10 * 1024 * 1024
const KNOWLEDGE_FILE_EXTENSIONS = new Set(['pdf', 'docx', 'xlsx', 'csv', 'md', 'txt'])

function validateKnowledgeFile(file: File) {
  const extension = file.name.includes('.') ? file.name.split('.').pop()?.toLowerCase() ?? '' : ''
  if (!KNOWLEDGE_FILE_EXTENSIONS.has(extension)) {
    return '学习资料仅支持 PDF、DOCX、XLSX、CSV、MD 和 TXT 格式。'
  }
  if (file.size > MAX_KNOWLEDGE_FILE_SIZE) {
    return '学习资料文件不能超过 10 MB。'
  }
  return ''
}
```

将 `uploadKnowledgeMaterial` 调整为先校验、再显示解析状态，并只在成功时替换预览：

```ts
async function uploadKnowledgeMaterial(event: Event) {
  generationStatus.value = ''
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  const validationError = validateKnowledgeFile(file)
  if (validationError) {
    generationStatus.value = validationError
    input.value = ''
    return
  }
  generationStatus.value = `正在解析 ${file.name}...`
  try {
    const questions = await uploadKnowledgeFile(file)
    generatedQuestions.value = questions
    generationStatus.value = `已从 ${file.name} 生成 ${questions.length} 道题，请预览后提交审核。`
  } catch (error) {
    generationStatus.value = error instanceof Error
      ? error.message
      : '上传分析失败，请检查文件格式或本地后端。'
  } finally {
    input.value = ''
  }
}
```

- [ ] **Step 4: 运行前端测试、类型检查和构建**

Run:

```powershell
Set-Location frontend
npm test -- ImportPage.test.ts
npm run typecheck
npm run build
```

Expected: Vitest 全部通过，`vue-tsc` 退出码 0，Vite 构建成功。

- [ ] **Step 5: 提交前端改动**

```powershell
git add frontend/src/pages/ImportPage.vue frontend/src/pages/ImportPage.test.ts
git commit -m "feat: improve pdf upload feedback"
```

### 任务 3：更新使用文档

**Files:**
- Modify: `README.md`
- Modify: `docs/api/local-flow.md`

- [ ] **Step 1: 更新 README 上传说明**

将学习资料上传说明明确为：

```markdown
也可以点击“上传学习资料”选择 `.txt`、`.md`、`.csv`、`.xlsx`、`.docx` 或 `.pdf` 文件。单个文件最大 10 MB；PDF 必须包含可复制的文本层，扫描版/纯图片 PDF 暂不支持。上传成功后，前端展示后端生成的可编辑题库草稿，确认后再提交管理员审核。
```

- [ ] **Step 2: 更新本地接口文档**

将 `/imports/knowledge/upload` 条目更新为：

```markdown
- `POST /imports/knowledge/upload`：上传 Java 学习资料文件，字段名为 `file`，支持 `.txt`、`.md`、`.csv`、`.xlsx`、`.docx`、`.pdf`，单个文件最大 10 MB，返回待预览的题库草稿。PDF 必须包含文本层；扫描版、损坏或加密 PDF 返回明确的 HTTP 400 中文错误。
```

- [ ] **Step 3: 检查文档和代码差异**

Run:

```powershell
git diff --check
rg -n "10 MB|扫描版|application/pdf|PDF / DOCX" README.md docs/api/local-flow.md frontend/src/pages/ImportPage.vue
```

Expected: `git diff --check` 无输出；三个文件均能检索到 PDF 范围和限制。

- [ ] **Step 4: 提交文档改动**

```powershell
git add README.md docs/api/local-flow.md
git commit -m "docs: document pdf knowledge uploads"
```

### 任务 4：完整验证、实际 PDF 上传和重新发布

**Files:**
- Verify: `scripts/verify-local.ps1`
- Verify: `scripts/publish-local.ps1`
- Verify: `.local/public-url.txt`

- [ ] **Step 1: 运行完整自动验证**

Run:

```powershell
.\scripts\verify-local.ps1
git diff --check
```

Expected: Maven 全部模块 0 failures/0 errors，前端类型检查、全部 Vitest 和 Vite 构建通过；`git diff --check` 无输出。

- [ ] **Step 2: 生成临时文本型 PDF 供真实上传验证**

在 PowerShell 运行以下脚本，生成一个带文本层且无需额外依赖的最小 PDF：

```powershell
$pdfPath = Join-Path (Resolve-Path '.\.local') 'studycollection-java-notes.pdf'
$encoding = [System.Text.Encoding]::ASCII
$stream = "BT /F1 12 Tf 48 720 Td (HashMap default load factor is 0.75. JVM stack stores frames.) Tj ET"
$objects = @(
  "1 0 obj`n<< /Type /Catalog /Pages 2 0 R >>`nendobj`n",
  "2 0 obj`n<< /Type /Pages /Kids [3 0 R] /Count 1 >>`nendobj`n",
  "3 0 obj`n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>`nendobj`n",
  "4 0 obj`n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>`nendobj`n",
  "5 0 obj`n<< /Length $($encoding.GetByteCount($stream)) >>`nstream`n$stream`nendstream`nendobj`n"
)
$builder = [System.Text.StringBuilder]::new()
[void]$builder.Append("%PDF-1.4`n")
$offsets = @()
foreach ($object in $objects) {
  $offsets += $encoding.GetByteCount($builder.ToString())
  [void]$builder.Append($object)
}
$xrefOffset = $encoding.GetByteCount($builder.ToString())
[void]$builder.Append("xref`n0 6`n0000000000 65535 f `n")
foreach ($offset in $offsets) {
  [void]$builder.Append(("{0:D10} 00000 n `n" -f $offset))
}
[void]$builder.Append("trailer`n<< /Size 6 /Root 1 0 R >>`nstartxref`n$xrefOffset`n%%EOF`n")
[System.IO.File]::WriteAllBytes($pdfPath, $encoding.GetBytes($builder.ToString()))
Get-Item $pdfPath | Select-Object FullName, Length
```

Expected: `.local\studycollection-java-notes.pdf` 存在且长度大于 500 字节。

- [ ] **Step 3: 重启 MySQL 模式服务并重新发布**

Run:

```powershell
.\scripts\publish-local.ps1 -UseMysql -RestartLocal -TimeoutSeconds 120
$publicUrl = (Get-Content -Raw .\.local\public-url.txt).Trim()
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:5173/ -TimeoutSec 15 | Select-Object StatusCode
Invoke-WebRequest -UseBasicParsing ($publicUrl + '/') -TimeoutSec 15 | Select-Object StatusCode
```

Expected: 本地和公网地址均返回 200，`.local/public-url.txt` 中是本次启动的新 `trycloudflare.com` 地址。

- [ ] **Step 4: 通过浏览器完成真实 PDF 流程**

1. 使用普通账号 `user / user123` 登录本地站点。
2. 打开 `/import`，确认上传控件显示 `PDF / DOCX / XLSX / CSV / MD / TXT，最大 10 MB`。
3. 上传 `.local\studycollection-java-notes.pdf`。
4. 确认页面出现含 `HashMap` 或 `JVM` 的可编辑预览，且尚未自动提交审核。
5. 点击“提交生成题审核”，确认成功提示；管理员进入题库管理后可看到待审核题。
6. 在 1280×720 与 390×844 两种视口检查控件、状态文本和预览表格，确认无页面级横向溢出且控制台无 error。

- [ ] **Step 5: 检查最终状态并推送**

Run:

```powershell
git status --short --branch
git log -5 --oneline
git push origin codex/java-learning-platform-implementation
```

Expected: 工作区干净，当前分支与远端同步，PDF 功能提交已推送。
