import { Link } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

export default function ProductCard({ product }) {
  const { add } = useCart()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [adding, setAdding] = useState(false)
  const [added, setAdded] = useState(false)

  const handleAddToCart = async (e) => {
    e.preventDefault()
    if (!user) { navigate('/login'); return }
    try {
      setAdding(true)
      await add(product, 1)
      setAdded(true)
      setTimeout(() => setAdded(false), 1500)
    } catch (err) {
      console.error('Add to cart failed', err)
    } finally {
      setAdding(false)
    }
  }

  return (
    <Link to={`/products/${product.id}`} className="group block">
      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm hover:shadow-lg transition-shadow duration-200 overflow-hidden h-full flex flex-col">
        {/* Image */}
        <div className="bg-gradient-to-br from-indigo-50 to-purple-50 h-48 flex items-center justify-center text-6xl overflow-hidden relative group-hover:opacity-90 transition-opacity">
          {product.images && product.images.length > 0 && product.images[0] ? (
            <img 
              src={product.images[0]} 
              alt={product.name} 
              className="w-full h-full object-cover"
              onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'flex'; }}
            />
          ) : null}
          <div className="absolute inset-0 flex items-center justify-center" style={{ display: product.images?.length > 0 && product.images[0] ? 'none' : 'flex' }}>
            {getCategoryEmoji(product.category)}
          </div>
        </div>

        <div className="p-4 flex flex-col flex-1">
          <div className="flex items-center justify-between mb-1">
            {product.category && (
              <span className="text-xs text-indigo-600 font-semibold uppercase tracking-wide">
                {product.category}
              </span>
            )}
            {product.viewCount !== undefined && (
              <span className="text-xs text-gray-400 flex items-center gap-1">
                👁️ {product.viewCount}
              </span>
            )}
          </div>
          <h3 className="font-semibold text-gray-900 text-sm leading-tight mb-1 line-clamp-2 group-hover:text-indigo-600 transition-colors">
            {product.name}
          </h3>
          <p className="text-gray-500 text-xs line-clamp-2 mb-3 flex-1">
            {product.description}
          </p>

          <div className="flex items-center justify-between mt-auto">
            <span className="text-lg font-bold text-indigo-600">
              ₺{Number(product.price).toFixed(2)}
            </span>
            {product.stock > 0 ? (
              <button
                onClick={handleAddToCart}
                disabled={adding}
                className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-all duration-150 ${
                  added
                    ? 'bg-green-500 text-white'
                    : 'bg-indigo-600 hover:bg-indigo-700 text-white'
                } disabled:opacity-60`}
              >
                {added ? '✓ Added' : adding ? '...' : '+ Cart'}
              </button>
            ) : (
              <span className="px-3 py-1.5 rounded-lg text-sm font-medium bg-red-100 text-red-600">
                Tükendi
              </span>
            )}
          </div>
        </div>
      </div>
    </Link>
  )
}

function getCategoryEmoji(category) {
  const map = {
    Electronics: '💻',
    Clothing: '👕',
    Books: '📚',
    Food: '🍎',
    Sports: '⚽',
    Home: '🏠',
    Beauty: '💄',
    Toys: '🧸',
    Automotive: '🚗',
    Garden: '🌱',
  }
  return map[category] ?? '📦'
}
