import api from './axios'

// POST /api/user/signup  →  { username, email, password }
export const register = (data) => api.post('/user/signup', data)

// POST /api/user/signin  →  { username, password }
// Response: { token, tokenType, userId, username, email, role }
export const login = (data) => api.post('/user/signin', data)
