import { describe, expect, it } from 'vitest'
import { resolveRouteAccess } from './router'

describe('route access control', () => {
  it('redirects anonymous users from protected pages to login', () => {
    expect(resolveRouteAccess({ name: 'dashboard', meta: { requiresAuth: true } }, null)).toBe('login')
  })

  it('allows normal users to use learning pages', () => {
    expect(resolveRouteAccess(
      { name: 'practice', meta: { requiresAuth: true } },
      { role: 'USER', displayName: '学习用户' }
    )).toBe(true)
  })

  it('keeps normal users out of admin-only pages', () => {
    expect(resolveRouteAccess(
      { name: 'feedback', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      { role: 'USER', displayName: '学习用户' }
    )).toBe('dashboard')
  })

  it('keeps normal users out of question bank management', () => {
    expect(resolveRouteAccess(
      { name: 'questions', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      { role: 'USER', displayName: '学习用户' }
    )).toBe('dashboard')
  })

  it('allows admins to use admin-only pages', () => {
    expect(resolveRouteAccess(
      { name: 'feedback', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      { role: 'ADMIN', displayName: '系统管理员' }
    )).toBe(true)
  })

  it('allows admins to use question bank management', () => {
    expect(resolveRouteAccess(
      { name: 'questions', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      { role: 'ADMIN', displayName: '系统管理员' }
    )).toBe(true)
  })
})
