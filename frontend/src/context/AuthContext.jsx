import { createContext, useContext, useState, useCallback } from 'react'
import { login as loginApi, register as registerApi, verifyOtp as verifyOtpApi } from '../api/authApi'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('user')
      return stored ? JSON.parse(stored) : null
    } catch {
      return null
    }
  })

  const login = useCallback(async (usernameOrEmail, password) => {
    const res = await loginApi({ username: usernameOrEmail, password })
    const { token, userId, name, username, email, role } = res.data
    const userData = { userId, name, username: username || usernameOrEmail, email, role }
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(userData))
    setUser(userData)
    return userData
  }, [])

  const register = useCallback(async (name, username, email, password) => {
    const res = await registerApi({ name, username, email, password })
    // requiresVerification: true ise OTP ekranına gideceğiz
    return res.data
  }, [])

  const verifyOtp = useCallback(async (email, code) => {
    const res = await verifyOtpApi({ email, code })
    const { token, userId, name, username, role } = res.data
    const userData = { userId, name, username, email, role }
    localStorage.setItem('token', token)
    localStorage.setItem('user', JSON.stringify(userData))
    setUser(userData)
    return userData
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, register, verifyOtp, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
