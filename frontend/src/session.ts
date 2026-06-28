import type { LoginResult, RegisterResult } from './api'

const SESSION_KEY = 'studyCollectionUser'

export function saveLoggedInUser(user: LoginResult) {
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(user))
}

export function saveRegisteredUser(user: RegisterResult) {
  window.localStorage.setItem(
    SESSION_KEY,
    JSON.stringify({
      userId: user.id,
      username: user.username,
      displayName: user.displayName,
      role: user.role
    })
  )
}

export function clearCurrentUser() {
  window.localStorage.removeItem(SESSION_KEY)
}
