import { useState, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function RegisterPage() {
  const { register, verifyOtp } = useAuth()
  const navigate = useNavigate()

  // Form state
  const [form, setForm] = useState({ name: '', username: '', email: '', password: '', confirm: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // OTP modal state
  const [showOtp, setShowOtp] = useState(false)
  const [registeredEmail, setRegisteredEmail] = useState('')
  const [otpDigits, setOtpDigits] = useState(['', '', '', '', '', ''])
  const [otpError, setOtpError] = useState('')
  const [otpLoading, setOtpLoading] = useState(false)
  const inputRefs = [useRef(), useRef(), useRef(), useRef(), useRef(), useRef()]

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    if (form.password !== form.confirm) {
      setError('Şifreler eşleşmiyor')
      return
    }
    setLoading(true)
    try {
      const data = await register(form.name, form.username, form.email, form.password)
      if (data?.requiresVerification) {
        setRegisteredEmail(form.email)
        setShowOtp(true)
      } else {
        navigate('/login', { state: { registered: true } })
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Kayıt başarısız. Lütfen tekrar deneyin.')
    } finally {
      setLoading(false)
    }
  }

  const handleOtpChange = (index, value) => {
    if (!/^\d?$/.test(value)) return
    const digits = [...otpDigits]
    digits[index] = value
    setOtpDigits(digits)
    if (value && index < 5) {
      inputRefs[index + 1].current?.focus()
    }
  }

  const handleOtpKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otpDigits[index] && index > 0) {
      inputRefs[index - 1].current?.focus()
    }
  }

  const handleOtpPaste = (e) => {
    e.preventDefault()
    const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6)
    const digits = [...otpDigits]
    pasted.split('').forEach((ch, i) => { digits[i] = ch })
    setOtpDigits(digits)
    const nextEmpty = digits.findIndex(d => !d)
    inputRefs[nextEmpty === -1 ? 5 : nextEmpty].current?.focus()
  }

  const handleOtpSubmit = async (e) => {
    e.preventDefault()
    const code = otpDigits.join('')
    if (code.length < 6) {
      setOtpError('Lütfen 6 haneli kodu eksiksiz girin.')
      return
    }
    setOtpError('')
    setOtpLoading(true)
    try {
      await verifyOtp(registeredEmail, code)
      navigate('/', { replace: true })
    } catch (err) {
      setOtpError(err.response?.data?.message || 'Kod hatalı veya süresi dolmuş.')
      setOtpDigits(['', '', '', '', '', ''])
      inputRefs[0].current?.focus()
    } finally {
      setOtpLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-50 to-purple-50 flex items-center justify-center px-4">

      {/* ──────────── OTP Modal ──────────── */}
      {showOtp && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm px-4">
          <div className="bg-white rounded-3xl shadow-2xl p-8 w-full max-w-sm text-center animate-[fadeIn_0.2s_ease]">
            <div className="text-5xl mb-4">📬</div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">E-Postanızı Doğrulayın</h2>
            <p className="text-gray-500 text-sm mb-6">
              <strong>{registeredEmail}</strong> adresine 6 haneli bir kod gönderdik.
              <br />Kod 15 dakika geçerlidir.
            </p>

            {otpError && (
              <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 mb-5 text-sm">
                {otpError}
              </div>
            )}

            <form onSubmit={handleOtpSubmit}>
              <div className="flex justify-center gap-2 mb-6" onPaste={handleOtpPaste}>
                {otpDigits.map((digit, i) => (
                  <input
                    key={i}
                    ref={inputRefs[i]}
                    type="text"
                    inputMode="numeric"
                    maxLength={1}
                    value={digit}
                    onChange={e => handleOtpChange(i, e.target.value)}
                    onKeyDown={e => handleOtpKeyDown(i, e)}
                    autoFocus={i === 0}
                    className={`w-12 h-14 text-center text-2xl font-bold border-2 rounded-xl transition-all focus:outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200 ${
                      digit ? 'border-indigo-500 bg-indigo-50' : 'border-gray-300'
                    }`}
                  />
                ))}
              </div>

              <button
                type="submit"
                disabled={otpLoading}
                className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-3 rounded-xl transition-colors disabled:opacity-60 text-base"
              >
                {otpLoading ? 'Doğrulanıyor…' : 'Hesabı Doğrula'}
              </button>
            </form>

            <p className="text-xs text-gray-400 mt-5">
              Kod gelmediyse spam klasörünü kontrol edin.
            </p>
          </div>
        </div>
      )}

      {/* ──────────── Register Form ──────────── */}
      <div className="bg-white rounded-2xl shadow-lg p-8 w-full max-w-md">
        <div className="text-center mb-8">
          <div className="text-4xl mb-3">🛍️</div>
          <h1 className="text-2xl font-bold text-gray-900">Hesap Oluştur</h1>
          <p className="text-gray-500 mt-1">ShopApp'a bugün katılın</p>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg px-4 py-3 mb-6 text-sm">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Ad Soyad</label>
            <input type="text" name="name" value={form.name} onChange={handleChange} required autoFocus
              placeholder="Alper Daşgın"
              className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent" />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Kullanıcı Adı <span className="text-gray-400 font-normal">(giriş için)</span>
            </label>
            <input type="text" name="username" value={form.username} onChange={handleChange} required
              placeholder="alperd"
              className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent" />
            <p className="text-xs text-gray-400 mt-1">Harf, rakam, nokta — boşluk kullanmayın</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">E-Posta</label>
            <input type="email" name="email" value={form.email} onChange={handleChange} required
              placeholder="alper@example.com"
              className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent" />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Şifre</label>
            <input type="password" name="password" value={form.password} onChange={handleChange} required
              placeholder="En az 6 karakter"
              className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent" />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Şifre Tekrar</label>
            <input type="password" name="confirm" value={form.confirm} onChange={handleChange} required
              placeholder="••••••••"
              className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent" />
          </div>

          <button type="submit" disabled={loading}
            className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-2.5 rounded-lg transition-colors disabled:opacity-60 mt-2">
            {loading ? 'Hesap oluşturuluyor…' : 'Hesap Oluştur'}
          </button>
        </form>

        <p className="text-center text-sm text-gray-500 mt-6">
          Zaten hesabınız var mı?{' '}
          <Link to="/login" className="text-indigo-600 hover:underline font-medium">Giriş Yap</Link>
        </p>
      </div>
    </div>
  )
}
