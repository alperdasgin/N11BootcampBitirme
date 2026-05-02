import api from './axios'

// POST /api/user/signup
export const register = (data) => api.post('/user/signup', data)

// POST /api/user/verify  →  { email, code }
export const verifyOtp = (data) => api.post('/user/verify', data)

// POST /api/user/signin
export const login = (data) => api.post('/user/signin', data)
