<template>
  <article class="table-panel editable-draft-panel" :aria-label="title">
    <div class="panel-header">
      <div>
        <h2>{{ title }}</h2>
        <span class="panel-count">{{ modelValue.length }} 题</span>
      </div>
      <button class="button-link" type="button" :aria-label="`新增${title}题目`" @click="addQuestion">新增题目</button>
    </div>

    <p v-if="modelValue.length === 0" class="empty-draft">暂无候选题目</p>
    <section v-for="(question, index) in modelValue" :key="index" class="draft-editor-row">
      <div class="draft-row-header">
        <h3>第 {{ index + 1 }} 题</h3>
        <button type="button" :aria-label="`删除${title}第 ${index + 1} 题`" @click="removeQuestion(index)">删除</button>
      </div>
      <label class="draft-title-field">
        题干与选项
        <textarea
          :value="question.title"
          data-draft-field="title"
          :aria-label="fieldLabel(index, '题干')"
          @input="updateField(index, 'title', $event)"
        ></textarea>
      </label>
      <div class="draft-meta-grid">
        <label>
          题型
          <select :value="question.type" :aria-label="fieldLabel(index, '题型')" @change="updateField(index, 'type', $event)">
            <option value="SINGLE_CHOICE">单选题</option>
            <option value="MULTIPLE_CHOICE">多选题</option>
            <option value="TRUE_FALSE">判断题</option>
            <option value="FILL_BLANK">填空题</option>
            <option value="SHORT_ANSWER">简答题</option>
            <option value="PROGRAMMING">编程题</option>
          </select>
        </label>
        <label>
          难度
          <select :value="question.difficulty" :aria-label="fieldLabel(index, '难度')" @change="updateField(index, 'difficulty', $event)">
            <option value="BEGINNER">入门</option>
            <option value="INTERMEDIATE">进阶</option>
            <option value="ADVANCED">精通</option>
          </select>
        </label>
        <label>
          知识点
          <input :value="question.knowledgePoint" :aria-label="fieldLabel(index, '知识点')" @input="updateField(index, 'knowledgePoint', $event)" />
        </label>
      </div>
      <div class="draft-answer-grid">
        <label>
          答案
          <textarea :value="question.answer" :aria-label="fieldLabel(index, '答案')" @input="updateField(index, 'answer', $event)"></textarea>
        </label>
        <label>
          解析
          <textarea :value="question.analysis" :aria-label="fieldLabel(index, '解析')" @input="updateField(index, 'analysis', $event)"></textarea>
        </label>
      </div>
    </section>
  </article>
</template>

<script setup lang="ts">
import type { QuestionPayload } from '../api'

const props = defineProps<{
  modelValue: QuestionPayload[]
  title: string
}>()
const emit = defineEmits<{
  'update:modelValue': [questions: QuestionPayload[]]
}>()

function addQuestion() {
  emit('update:modelValue', [
    ...props.modelValue,
    {
      title: '',
      type: 'SINGLE_CHOICE',
      difficulty: 'BEGINNER',
      knowledgePoint: '',
      answer: '',
      analysis: ''
    }
  ])
}

function removeQuestion(index: number) {
  emit('update:modelValue', props.modelValue.filter((_, questionIndex) => questionIndex !== index))
}

function updateField(index: number, field: keyof QuestionPayload, event: Event) {
  const value = (event.target as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement).value
  emit('update:modelValue', props.modelValue.map((question, questionIndex) => (
    questionIndex === index ? { ...question, [field]: value } : question
  )))
}

function fieldLabel(index: number, field: string) {
  return `${props.title}第 ${index + 1} 题${field}`
}
</script>
