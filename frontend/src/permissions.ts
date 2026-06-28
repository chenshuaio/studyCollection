import { getCurrentUser } from './session'

export function isAdmin() {
  return getCurrentUser()?.role === 'ADMIN'
}
