import type { LoginResult, RegisterResult } from './api'

const SESSION_KEY = 'studyCollectionUser'

export type CurrentUser = {
  role: string
  displayName: string
  token: string
  userId: number
  username: string
}

export function saveLoggedInUser(user: LoginResult) {
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(user))
}

export function saveRegisteredUser(user: RegisterResult) {
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(user))
}

export function clearCurrentUser() {
  window.localStorage.removeItem(SESSION_KEY)
}

export function getCurrentUser(): CurrentUser | null {
  const raw = window.localStorage.getItem(SESSION_KEY)
  if (!raw) {
    return null
  }

  try {
    const currentUser = JSON.parse(raw) as Partial<CurrentUser>
    if (!currentUser.token || !currentUser.userId || !currentUser.username || !currentUser.role || !currentUser.displayName) {
      clearCurrentUser()
      return null
    }
    return currentUser as CurrentUser
  } catch {
    clearCurrentUser()
    return null
  }
}
