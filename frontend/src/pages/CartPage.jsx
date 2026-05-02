import { useCart } from '../context/CartContext'
import { useNavigate, Link } from 'react-router-dom'
import LoadingSpinner from '../components/LoadingSpinner'

export default function CartPage() {
  const { cart, loading, remove, update } = useCart()
  const navigate = useNavigate()

  if (loading) return <LoadingSpinner message="Loading cart…" />

  const items = cart?.items ?? []
  // totalPrice comes from CartResponse; fall back to client-side calc if absent
  const total = cart?.totalPrice != null
    ? Number(cart.totalPrice)
    : items.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0)

  if (items.length === 0) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-20 text-center">
        <div className="text-6xl mb-4">🛒</div>
        <h2 className="text-2xl font-bold text-gray-900 mb-2">Your cart is empty</h2>
        <p className="text-gray-500 mb-6">Add some products to get started.</p>
        <Link
          to="/products"
          className="bg-indigo-600 hover:bg-indigo-700 text-white px-8 py-3 rounded-xl font-semibold inline-block transition-colors"
        >
          Browse Products
        </Link>
      </div>
    )
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h1 className="text-3xl font-bold text-gray-900 mb-8">Shopping Cart</h1>

      <div className="flex flex-col lg:flex-row gap-8">
        {/* Cart Items */}
        <div className="flex-1 space-y-4">
          {items.map((item) => (
            <div key={item.productId} className="bg-white rounded-2xl border border-gray-100 shadow-sm p-5 flex items-center gap-5">
              <div className="bg-gradient-to-br from-indigo-50 to-purple-50 rounded-xl w-16 h-16 flex items-center justify-center text-3xl shrink-0">
                📦
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="font-semibold text-gray-900 text-sm truncate">{item.productName}</h3>
                <p className="text-indigo-600 font-bold mt-0.5">₺{Number(item.price).toFixed(2)}</p>

              </div>
              {/* Quantity control */}
              <div className="flex items-center border border-gray-200 rounded-lg overflow-hidden">
                <button
                  onClick={() => update(item.productId, item.quantity - 1)}
                  className="px-2.5 py-1.5 bg-gray-50 hover:bg-gray-100 text-gray-700 font-bold text-sm transition-colors"
                >
                  −
                </button>
                <span className="px-4 py-1.5 text-sm font-semibold text-gray-900">{item.quantity}</span>
                <button
                  onClick={() => update(item.productId, item.quantity + 1)}
                  className="px-2.5 py-1.5 bg-gray-50 hover:bg-gray-100 text-gray-700 font-bold text-sm transition-colors"
                >
                  +
                </button>
              </div>
              <div className="text-sm font-bold text-gray-900 w-20 text-right">
                ₺{(Number(item.price) * item.quantity).toFixed(2)}
              </div>
              <button
                onClick={() => remove(item.productId)}
                className="text-gray-300 hover:text-red-500 transition-colors ml-2 text-xl"
                title="Remove"
              >
                ×
              </button>
            </div>
          ))}
        </div>

        {/* Order Summary */}
        <div className="lg:w-80 shrink-0">
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 sticky top-24">
            <h2 className="font-bold text-gray-900 text-lg mb-4">Order Summary</h2>
            <div className="space-y-3 mb-4">
              {items.map((item) => (
                <div key={item.productId} className="flex justify-between text-sm text-gray-600">
                  <span className="truncate mr-2">{item.productName} × {item.quantity}</span>
                  <span className="shrink-0">₺{(Number(item.price) * item.quantity).toFixed(2)}</span>
                </div>
              ))}
            </div>
            <div className="border-t border-gray-100 pt-4 flex justify-between font-bold text-gray-900">
              <span>Total</span>
              <span className="text-indigo-600 text-xl">₺{total.toFixed(2)}</span>
            </div>
            <button
              onClick={() => navigate('/checkout')}
              className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-3 rounded-xl mt-6 transition-colors"
            >
              Proceed to Checkout →
            </button>
            <Link
              to="/products"
              className="block text-center text-sm text-gray-500 hover:text-indigo-600 mt-3 transition-colors"
            >
              Continue Shopping
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
