import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { getMyOrders } from '../api/orderApi'
import { Link } from 'react-router-dom'
import LoadingSpinner from '../components/LoadingSpinner'
import ErrorMessage from '../components/ErrorMessage'

const STATUS_STYLES = {
  COMPLETED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-red-100 text-red-700',
  CREATED: 'bg-yellow-100 text-yellow-700',
  STOCK_DEDUCTED: 'bg-blue-100 text-blue-700',
  PAYMENT_PENDING: 'bg-orange-100 text-orange-700',
  FAILED: 'bg-red-100 text-red-700',
}

const STATUS_EMOJI = {
  COMPLETED: '✅',
  CANCELLED: '❌',
  CREATED: '⏳',
  STOCK_DEDUCTED: '📦',
  PAYMENT_PENDING: '💳',
  FAILED: '⚠️',
}

export default function OrdersPage() {
  const { user } = useAuth()
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const fetchOrders = () => {
    setLoading(true)
    setError('')
    getMyOrders(user.username)
      .then((res) => setOrders(Array.isArray(res.data) ? res.data : []))
      .catch((err) => setError(err.response?.data?.message || 'Failed to load orders'))
      .finally(() => setLoading(false))
  }

  useEffect(() => { fetchOrders() }, [user.username])

  if (loading) return <LoadingSpinner message="Loading your orders…" />
  if (error) return <ErrorMessage message={error} onRetry={fetchOrders} />

  if (orders.length === 0) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center">
        <div className="text-6xl mb-4">📋</div>
        <h2 className="text-2xl font-bold text-gray-900 mb-2">No orders yet</h2>
        <p className="text-gray-500 mb-6">Your order history will appear here.</p>
        <Link
          to="/products"
          className="bg-indigo-600 hover:bg-indigo-700 text-white px-8 py-3 rounded-xl font-semibold inline-block transition-colors"
        >
          Start Shopping
        </Link>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h1 className="text-3xl font-bold text-gray-900 mb-8">My Orders</h1>

      <div className="space-y-4">
        {orders.map((order) => (
          <Link key={order.orderId ?? order.id} to={`/orders/${order.orderId ?? order.id}`}>
            <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between flex-wrap gap-3">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-bold text-gray-900 text-sm">
                      Order #{order.orderId ?? order.id}
                    </span>
                    <span className={`text-xs font-semibold px-2.5 py-1 rounded-full ${STATUS_STYLES[order.status] ?? 'bg-gray-100 text-gray-600'}`}>
                      {STATUS_EMOJI[order.status]} {order.status}
                    </span>
                  </div>
                  <p className="text-gray-500 text-xs">
                    {order.items?.length ?? 0} item(s)
                  </p>
                </div>
                <div className="text-right">
                  <p className="font-bold text-indigo-600 text-lg">
                    ₺{Number(order.totalPrice).toFixed(2)}
                  </p>
                  <p className="text-xs text-gray-400">
                    {order.createdAt ? new Date(order.createdAt).toLocaleDateString() : ''}
                  </p>
                </div>
              </div>

              {order.items && order.items.length > 0 && (
                <div className="mt-4 border-t border-gray-50 pt-4">
                  <div className="flex flex-wrap gap-2">
                    {order.items.slice(0, 3).map((item, idx) => (
                      <span key={idx} className="text-xs bg-gray-50 text-gray-600 px-2.5 py-1 rounded-lg">
                        {item.productName} × {item.quantity}
                      </span>
                    ))}
                    {order.items.length > 3 && (
                      <span className="text-xs bg-gray-50 text-gray-400 px-2.5 py-1 rounded-lg">
                        +{order.items.length - 3} more
                      </span>
                    )}
                  </div>
                </div>
              )}
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}
