import { describe, expect, it } from 'vitest'
import { resolveRouteAccess, router } from './router'

function currentUser(role: 'USER' | 'ADMIN') {
  return {
    token: `${role.toLowerCase()}-token`,
    userId: role === 'ADMIN' ? 1 : 2,
    username: role.toLowerCase(),
    role,
    displayName: role === 'ADMIN' ? '系统管理员' : '学习用户'
  }
}

describe('route access control', () => {
  it('redirects anonymous users from protected pages to login', () => {
    expect(resolveRouteAccess({ name: 'dashboard', meta: { requiresAuth: true } }, null)).toBe('login')
  })

  it('allows normal users to use learning pages', () => {
    expect(resolveRouteAccess(
      { name: 'practice', meta: { requiresAuth: true } },
      currentUser('USER')
    )).toBe(true)
  })

  it('keeps normal users out of admin-only pages', () => {
    expect(resolveRouteAccess(
      { name: 'feedback', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      currentUser('USER')
    )).toBe('dashboard')
  })

  it('keeps normal users out of question bank management', () => {
    expect(resolveRouteAccess(
      { name: 'questions', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      currentUser('USER')
    )).toBe('dashboard')
  })

  it('keeps normal users out of user management', () => {
    expect(resolveRouteAccess(
      { name: 'users', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      currentUser('USER')
    )).toBe('dashboard')
  })

  it('keeps normal users out of knowledge point management', () => {
    expect(resolveRouteAccess(
      { name: 'knowledge-points', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      currentUser('USER')
    )).toBe('dashboard')
  })

  it('allows admins to use admin-only pages', () => {
    expect(resolveRouteAccess(
      { name: 'feedback', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      currentUser('ADMIN')
    )).toBe(true)
  })

  it('allows admins to use user management', () => {
    expect(resolveRouteAccess(
      { name: 'users', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      currentUser('ADMIN')
    )).toBe(true)
  })

  it('allows admins to use knowledge point management', () => {
    expect(resolveRouteAccess(
      { name: 'knowledge-points', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      currentUser('ADMIN')
    )).toBe(true)
  })

  it('registers user management as an administrator-only route', () => {
    const usersRoute = router.getRoutes().find((route) => route.name === 'users')

    expect(usersRoute?.path).toBe('/users')
    expect(usersRoute?.meta).toEqual(expect.objectContaining({ requiresAuth: true, requiredRole: 'ADMIN' }))
  })

  it('registers knowledge point management as an administrator-only route', () => {
    const knowledgePointRoute = router.getRoutes().find((route) => route.name === 'knowledge-points')

    expect(knowledgePointRoute?.path).toBe('/knowledge-points')
    expect(knowledgePointRoute?.meta).toEqual(expect.objectContaining({ requiresAuth: true, requiredRole: 'ADMIN' }))
  })

  it('registers persisted exam taking with a session id', () => {
    const examTakingRoute = router.getRoutes().find((route) => route.name === 'exam-taking')

    expect(examTakingRoute?.path).toBe('/exams/:examId/take')
    expect(examTakingRoute?.meta).toEqual(expect.objectContaining({ requiresAuth: true }))
  })

  it('registers exam rule management as an administrator-only route', () => {
    const examRuleRoute = router.getRoutes().find((route) => route.name === 'exam-rule-management')

    expect(examRuleRoute?.path).toBe('/exam-rules/manage')
    expect(examRuleRoute?.meta).toEqual(expect.objectContaining({ requiresAuth: true, requiredRole: 'ADMIN' }))
    expect(resolveRouteAccess(
      { name: 'exam-rule-management', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      currentUser('USER')
    )).toBe('dashboard')
  })
})
