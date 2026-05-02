import { useState, useEffect } from 'react'
import { useParams, useNavigate, useLocation, Link } from 'react-router-dom'
import { getOrder } from '../api/orderApi'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorMessage from '../components/ErrorMessage'

const STATUS_STYLES = {
  COMPLETED: 'bg-green-100 text-green-700 border-green-200',
  CANCELLED: 'bg-red-100 text-red-700 border-red-200',
  CREATED: 'bg-yellow-100 text-yellow-700 border-yellow-200',
  STOCK_DEDUCTED: 'bg-blue-100 text-blue-700 border-blue-200',
  FAILED: 'bg-red-100 text-red-700 border-red-200',
}

const STATUS_EMOJI = {
  COMPLETED: '✅',
  CANCELLED: '❌',
  CREATED: '⏳',
  STOCK_DEDUCTED: '📦',
  FAILED: '⚠️',
}

export default function OrderDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const justCreated = location.state?.success

  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    setLoading(true)
    getOrder(id)
      .then((res) => setOrder(res.data))
      .catch((err) => setError(err.response?.data?.message || 'Order not found'))
      .finally(() => setLoading(false))
  }, [id])

  if (loading) return <LoadingSpinner message="Loading order…" />
  if (error) return <ErrorMessage message={error} onRetry={() => navigate('/orders')} />

  const items = order.items ?? []
  const total = order.totalPrice ?? items.reduce((s, i) => s + i.price * i.quantity, 0)

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <button
        onClick={() => navigate('/orders')}
        className="text-indigo-600 hover:underline text-sm font-medium mb-6 flex items-center gap-1"
      >
        ← Back to orders
      </button>

      {justCreated && (
        <div className="bg-green-50 border border-green-200 rounded-2xl p-5 mb-6 flex items-start gap-4">
          <span className="text-3xl">🎉</span>
          <div>
            <h3 className="font-bold text-green-800">Order Placed Successfully!</h3>
            <p className="text-green-700 text-sm mt-0.5">
              Your order is being processed. You can track its status below.
            </p>
          </div>
        </div>
      )}

      <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        {/* Header */}
        <div className="p-6 border-b border-gray-50 flex items-start justify-between flex-wrap gap-4">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Order #{order.orderId ?? order.id}</h1>
            {order.createdAt && (
              <p className="text-gray-500 text-sm mt-0.5">
                Placed on {new Date(order.createdAt).toLocaleString()}
              </p>
            )}
          </div>
          <span className={`text-sm font-semibold px-4 py-2 rounded-full border ${STATUS_STYLES[order.status] ?? 'bg-gray-100 text-gray-600 border-gray-200'}`}>
            {STATUS_EMOJI[order.status]} {order.status}
          </span>
        </div>

        {/* Items */}
        <div className="p-6">
          <h2 className="font-semibold text-gray-900 mb-4">Items</h2>
          <div className="space-y-3">
            {items.map((item, idx) => (
              <div key={idx} className="flex items-center gap-4 py-2">
                <div className="bg-gradient-to-br from-indigo-50 to-purple-50 rounded-xl w-12 h-12 flex items-center justify-center text-2xl shrink-0">
                  📦
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900 text-sm truncate">{item.productName}</p>
                  <p className="text-gray-500 text-xs">Qty: {item.quantity}</p>
                </div>
                <p className="font-semibold text-gray-900 text-sm">
                  ₺{(item.price * item.quantity).toFixed(2)}
                </p>
              </div>
            ))}
          </div>

          <div className="border-t border-gray-100 mt-4 pt-4 flex justify-between">
            <span className="font-bold text-gray-900">Total</span>
            <span className="font-bold text-indigo-600 text-xl">₺{Number(total).toFixed(2)}</span>
          </div>
        </div>
      </div>

      <div className="flex gap-3 mt-6">
        <Link
          to="/products"
          className="flex-1 text-center bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-3 rounded-xl transition-colors"
        >
          Continue Shopping
        </Link>
        <Link
          to="/orders"
          className="flex-1 text-center border-2 border-indigo-600 text-indigo-600 hover:bg-indigo-50 font-semibold py-3 rounded-xl transition-colors"
        >
          All Orders
        </Link>
      </div>
    </div>
  )
}
