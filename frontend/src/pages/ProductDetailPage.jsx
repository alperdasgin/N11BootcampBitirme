import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getProduct, addReview } from '../api/productApi'
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
  const [activeImage, setActiveImage] = useState(0)

  // Review states
  const [reviewRating, setReviewRating] = useState(5)
  const [reviewComment, setReviewComment] = useState('')
  const [submittingReview, setSubmittingReview] = useState(false)
  const [reviewError, setReviewError] = useState('')

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

  const handleReviewSubmit = async (e) => {
    e.preventDefault()
    if (!user) { navigate('/login'); return }
    setReviewError('')
    setSubmittingReview(true)
    try {
      const res = await addReview(product.id, {
        username: user.username,
        rating: reviewRating,
        comment: reviewComment
      })
      // append new review
      setProduct(prev => ({
        ...prev,
        reviews: [...(prev.reviews || []), res],
        reviewCount: (prev.reviewCount || 0) + 1
      }))
      setReviewComment('')
    } catch (err) {
      const backendMsg = err.response?.data?.error || err.response?.data?.message
      setReviewError(backendMsg || 'Yorum eklenemedi.')
    } finally {
      setSubmittingReview(false)
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
        {/* Images Area */}
        <div className="md:w-96 shrink-0 flex flex-col p-6 bg-gray-50">
          <div className="bg-gradient-to-br from-indigo-50 to-purple-50 rounded-xl flex items-center justify-center text-[80px] overflow-hidden relative aspect-square shadow-inner">
            {product.images && product.images.length > 0 && product.images[activeImage] ? (
              <img 
                src={product.images[activeImage]} 
                alt={product.name} 
                className="w-full h-full object-cover"
                onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'flex'; }}
              />
            ) : null}
            <div className="absolute inset-0 flex items-center justify-center" style={{ display: product.images?.length > 0 && product.images[activeImage] ? 'none' : 'flex' }}>
              📦
            </div>
          </div>
          
          {/* Thumbnails */}
          {product.images && product.images.length > 1 && (
            <div className="flex gap-2 mt-4 overflow-x-auto pb-2">
              {product.images.map((img, idx) => (
                <button 
                  key={idx} 
                  onClick={() => setActiveImage(idx)}
                  className={`w-16 h-16 rounded-lg overflow-hidden shrink-0 border-2 transition-all ${activeImage === idx ? 'border-indigo-600 opacity-100' : 'border-transparent opacity-60 hover:opacity-100'}`}
                >
                  <img src={img} className="w-full h-full object-cover" alt="thumbnail" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Info */}
        <div className="p-8 flex flex-col flex-1">
          <div className="flex items-center justify-between mb-2">
            {product.category && (
              <span className="text-xs text-indigo-600 font-semibold uppercase tracking-wide">
                {product.category}
              </span>
            )}
            {product.viewCount !== undefined && (
              <span className="text-sm text-gray-500 flex items-center gap-1">
                👁️ {product.viewCount} kez görüntülendi
              </span>
            )}
          </div>
          <h1 className="text-2xl font-bold text-gray-900 mb-3">{product.name}</h1>
          <p className="text-gray-500 leading-relaxed mb-6">{product.description}</p>

          <div className="text-3xl font-bold text-indigo-600 mb-6">
            ₺{Number(product.price).toFixed(2)}
          </div>

          {product.stock > 0 ? (
            <>
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
                    onClick={() => setQuantity((q) => Math.min(product.stock, q + 1))}
                    className="px-3 py-2 bg-gray-50 hover:bg-gray-100 text-gray-700 font-bold transition-colors"
                  >
                    +
                  </button>
                </div>
                <span className="text-xs text-gray-500">({product.stock} stokta)</span>
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
            </>
          ) : (
            <div className="bg-red-50 border border-red-200 text-red-600 rounded-xl p-4 text-center font-semibold mb-6">
              Bu ürün maalesef tükendi.
            </div>
          )}
        </div>
      </div>

      {/* Reviews Section */}
      <div className="mt-10 bg-white rounded-2xl shadow-sm border border-gray-100 p-8">
        <h2 className="text-2xl font-bold text-gray-900 mb-6">Müşteri Yorumları</h2>
        
        {/* Average Rating Banner */}
        <div className="flex items-center gap-4 mb-8 bg-gray-50 p-4 rounded-xl border border-gray-200">
          <div className="text-4xl font-bold text-yellow-500">
            {product.averageRating > 0 ? product.averageRating : '-'} 
          </div>
          <div className="flex flex-col">
            <div className="flex text-yellow-400 text-xl">
              {'★'.repeat(Math.round(product.averageRating || 0))}{'☆'.repeat(5 - Math.round(product.averageRating || 0))}
            </div>
            <span className="text-sm text-gray-500">{product.reviewCount || 0} Değerlendirme</span>
          </div>
        </div>

        {/* Review Form */}
        <div className="mb-10 bg-indigo-50/50 p-6 rounded-xl border border-indigo-100">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">Bu ürünü değerlendir</h3>
          {reviewError && <div className="text-sm text-red-600 mb-3 bg-red-50 p-2 rounded">{reviewError}</div>}
          <form onSubmit={handleReviewSubmit} className="flex flex-col gap-4">
            <div className="flex items-center gap-2">
              <label className="text-sm font-medium text-gray-700">Puanınız:</label>
              <select 
                value={reviewRating} 
                onChange={e => setReviewRating(Number(e.target.value))}
                className="border border-gray-300 rounded-lg p-2 text-sm focus:ring-2 focus:ring-indigo-500"
              >
                <option value="5">5 - Mükemmel</option>
                <option value="4">4 - Çok İyi</option>
                <option value="3">3 - İyi</option>
                <option value="2">2 - İdare Eder</option>
                <option value="1">1 - Kötü</option>
              </select>
            </div>
            <textarea
              placeholder="Ürün hakkındaki düşüncelerinizi paylaşın..."
              rows="3"
              value={reviewComment}
              onChange={e => setReviewComment(e.target.value)}
              className="w-full border border-gray-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-indigo-500"
              required
            ></textarea>
            <button 
              type="submit" 
              disabled={submittingReview}
              className="self-end px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium text-sm transition-colors disabled:opacity-50"
            >
              {submittingReview ? 'Gönderiliyor...' : 'Yorum Yap'}
            </button>
          </form>
        </div>

        {/* Reviews List */}
        <div className="space-y-6">
          {product.reviews && product.reviews.length > 0 ? (
            product.reviews.map(review => (
              <div key={review.id} className="border-b border-gray-100 pb-6 last:border-0 last:pb-0">
                <div className="flex items-center gap-3 mb-2">
                  <div className="w-10 h-10 bg-indigo-100 text-indigo-700 font-bold rounded-full flex items-center justify-center uppercase">
                    {review.username.charAt(0)}
                  </div>
                  <div>
                    <div className="font-semibold text-gray-900 text-sm">{review.username}</div>
                    <div className="flex items-center gap-2">
                      <div className="text-yellow-400 text-xs">
                        {'★'.repeat(review.rating)}{'☆'.repeat(5 - review.rating)}
                      </div>
                      <span className="text-xs text-gray-400">
                        {new Date(review.createdAt).toLocaleDateString('tr-TR')}
                      </span>
                    </div>
                  </div>
                </div>
                <p className="text-gray-600 text-sm pl-13">{review.comment}</p>
              </div>
            ))
          ) : (
            <div className="text-center text-gray-500 py-8">Henüz yorum yapılmamış. İlk yorumu siz yapın!</div>
          )}
        </div>
      </div>
    </div>
  )
}
