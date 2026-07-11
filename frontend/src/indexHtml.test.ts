import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

describe('index.html', () => {
  it('defines Chinese document and mobile viewport metadata', () => {
    const html = readFileSync(resolve(process.cwd(), 'index.html'), 'utf-8')

    expect(html).toContain('<html lang="zh-CN">')
    expect(html).toContain('<meta name="viewport" content="width=device-width, initial-scale=1.0" />')
    expect(html).toContain('<title>StudyCollection - Java 学习平台</title>')
  })
})
