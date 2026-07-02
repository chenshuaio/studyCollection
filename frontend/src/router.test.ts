import { describe, expect, it } from 'vitest'
import { resolveRouteAccess, router } from './router'

describe('route access control', () => {
  it('redirects anonymous users from protected pages to login', () => {
    expect(resolveRouteAccess({ name: 'dashboard', meta: { requiresAuth: true } }, null)).toBe('login')
  })

  it('allows normal users to use learning pages', () => {
    expect(resolveRouteAccess(
      { name: 'practice', meta: { requiresAuth: true } },
      { role: 'USER', displayName: 'user' }
    )).toBe(true)
  })

  it('keeps normal users out of admin-only pages', () => {
    expect(resolveRouteAccess(
      { name: 'feedback', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      { role: 'USER', displayName: 'user' }
    )).toBe('dashboard')
  })

  it('keeps normal users out of question bank management', () => {
    expect(resolveRouteAccess(
      { name: 'questions', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      { role: 'USER', displayName: 'user' }
    )).toBe('dashboard')
  })

  it('keeps normal users out of user management', () => {
    expect(resolveRouteAccess(
      { name: 'users', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      { role: 'USER', displayName: 'user' }
    )).toBe('dashboard')
  })

  it('keeps normal users out of knowledge point management', () => {
    expect(resolveRouteAccess(
      { name: 'knowledge-points', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      { role: 'USER', displayName: 'user' }
    )).toBe('dashboard')
  })

  it('allows admins to use admin-only pages', () => {
    expect(resolveRouteAccess(
      { name: 'feedback', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      { role: 'ADMIN', displayName: 'admin' }
    )).toBe(true)
  })

  it('allows admins to use user management', () => {
    expect(resolveRouteAccess(
      { name: 'users', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      { role: 'ADMIN', displayName: 'admin' }
    )).toBe(true)
  })

  it('allows admins to use knowledge point management', () => {
    expect(resolveRouteAccess(
      { name: 'knowledge-points', meta: { requiresAuth: true, requiredRole: 'ADMIN' } },
      { role: 'ADMIN', displayName: 'admin' }
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
})
