import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useCart } from '../context/CartContext'
import { useState, useEffect } from 'react'

export default function Navbar() {
  const { user, logout } = useAuth()
  const { itemCount } = useCart()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [searchQuery, setSearchQuery] = useState('')

  useEffect(() => {
    // URL'deki search query değiştiğinde input'u güncelle
    const q = searchParams.get('search')
    if (q) setSearchQuery(q)
    else setSearchQuery('')
  }, [searchParams])

  const handleSearchSubmit = (e) => {
    e.preventDefault()
    if (searchQuery.trim()) {
      navigate(`/products?search=${encodeURIComponent(searchQuery.trim())}`)
    } else {
      navigate(`/products`)
    }
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <nav className="bg-white border-b border-gray-200 sticky top-0 z-50 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2">
            <span className="text-2xl font-bold text-indigo-600">🛍️ ShopApp</span>
          </Link>

          {/* Nav links & Search */}
          <div className="flex items-center gap-6">
            
            {/* Search Bar */}
            <form onSubmit={handleSearchSubmit} className="hidden md:flex relative">
              <input
                type="text"
                placeholder="Search products..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-10 pr-4 py-2 border border-gray-300 rounded-full focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent text-sm w-64 transition-all"
              />
              <span className="absolute left-3 top-2.5 text-gray-400">
                🔍
              </span>
            </form>

            <Link to="/products" className="text-gray-600 hover:text-indigo-600 font-medium transition-colors">
              Products
            </Link>

            {user ? (
              <>
                {user.role === 'ADMIN' && (
                  <Link to="/admin" className="text-indigo-600 hover:text-indigo-800 font-bold transition-colors">
                    ⚙️ Admin Panel
                  </Link>
                )}
                <Link to="/orders" className="text-gray-600 hover:text-indigo-600 font-medium transition-colors">
                  My Orders
                </Link>
                <Link to="/cart" className="relative text-gray-600 hover:text-indigo-600 transition-colors">
                  <span className="text-xl">🛒</span>
                  {itemCount > 0 && (
                    <span className="absolute -top-2 -right-2 bg-indigo-600 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold">
                      {itemCount > 9 ? '9+' : itemCount}
                    </span>
                  )}
                </Link>
                <div className="flex items-center gap-3">
                  <span className="text-sm text-gray-500">Hi, <strong>{user.name || user.username}</strong></span>
                  <button
                    onClick={handleLogout}
                    className="bg-gray-100 hover:bg-gray-200 text-gray-700 px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                  >
                    Logout
                  </button>
                </div>
              </>
            ) : (
              <div className="flex items-center gap-2">
                <Link to="/login" className="text-gray-600 hover:text-indigo-600 font-medium transition-colors px-4 py-2">
                  Login
                </Link>
                <Link
                  to="/register"
                  className="bg-indigo-600 hover:bg-indigo-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                >
                  Register
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </nav>
  )
}
