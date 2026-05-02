import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getProduct } from '../api/productApi'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorMessage from '../components/ErrorMessage'

export default function ProductDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const { add } = useCart()

  const [product, setProduct] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [quantity, setQuantity] = useState(1)
  const [adding, setAdding] = useState(false)
  const [added, setAdded] = useState(false)

  useEffect(() => {
    setLoading(true)
    getProduct(id)
      .then((res) => setProduct(res.data))
      .catch((err) => setError(err.response?.data?.message || 'Product not found'))
      .finally(() => setLoading(false))
  }, [id])

  const handleAddToCart = async () => {
    if (!user) { navigate('/login'); return }
    try {
      setAdding(true)
      await add(product, quantity)
      setAdded(true)
      setTimeout(() => setAdded(false), 2000)
    } catch (err) {
      console.error(err)
    } finally {
      setAdding(false)
    }
  }

  if (loading) return <LoadingSpinner message="Loading product…" />
  if (error) return <ErrorMessage message={error} onRetry={() => navigate(-1)} />

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <button
        onClick={() => navigate(-1)}
        className="text-indigo-600 hover:underline text-sm font-medium mb-6 flex items-center gap-1"
      >
        ← Back to products
      </button>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden md:flex">
        {/* Image */}
        <div className="bg-gradient-to-br from-indigo-50 to-purple-50 md:w-96 flex items-center justify-center text-[120px] p-12 shrink-0">
          📦
        </div>

        {/* Info */}
        <div className="p-8 flex flex-col flex-1">
          {product.category && (
            <span className="text-xs text-indigo-600 font-semibold uppercase tracking-wide mb-2">
              {product.category}
            </span>
          )}
          <h1 className="text-2xl font-bold text-gray-900 mb-3">{product.name}</h1>
          <p className="text-gray-500 leading-relaxed mb-6">{product.description}</p>

          <div className="text-3xl font-bold text-indigo-600 mb-6">
            ₺{Number(product.price).toFixed(2)}
          </div>

          {/* Quantity selector */}
          <div className="flex items-center gap-4 mb-6">
            <label className="text-sm font-medium text-gray-700">Quantity</label>
            <div className="flex items-center border border-gray-200 rounded-lg overflow-hidden">
              <button
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                className="px-3 py-2 bg-gray-50 hover:bg-gray-100 text-gray-700 font-bold transition-colors"
              >
                −
              </button>
              <span className="px-5 py-2 text-sm font-semibold text-gray-900">{quantity}</span>
              <button
                onClick={() => setQuantity((q) => q + 1)}
                className="px-3 py-2 bg-gray-50 hover:bg-gray-100 text-gray-700 font-bold transition-colors"
              >
                +
              </button>
            </div>
          </div>

          <div className="flex gap-3">
            <button
              onClick={handleAddToCart}
              disabled={adding}
              className={`flex-1 py-3 rounded-xl font-semibold text-sm transition-all ${
                added
                  ? 'bg-green-500 text-white'
                  : 'bg-indigo-600 hover:bg-indigo-700 text-white'
              } disabled:opacity-60`}
            >
              {added ? '✓ Added to Cart!' : adding ? 'Adding…' : '🛒 Add to Cart'}
            </button>
            <button
              onClick={() => { handleAddToCart().then(() => navigate('/cart')) }}
              className="px-6 py-3 rounded-xl border-2 border-indigo-600 text-indigo-600 hover:bg-indigo-50 font-semibold text-sm transition-colors"
            >
              Buy Now
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
