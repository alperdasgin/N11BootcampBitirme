import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { useAuth } from '../context/AuthContext'
import { createOrder } from '../api/orderApi'

const INITIAL_CARD = {
  cardHolderName: '',
  cardNumber: '',
  expireMonth: '',
  expireYear: '',
  cvc: '',
}

export default function CheckoutPage() {
  const { cart, clear } = useCart()
  const { user } = useAuth()
  const navigate = useNavigate()

  const [card, setCard] = useState(INITIAL_CARD)
  const [address, setAddress] = useState({
    firstName: '',
    lastName: '',
    streetAddress: '',
    city: '',
    country: 'Turkey',
    phone: '',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const items = cart?.items ?? []
  const total = cart?.totalPrice != null
    ? Number(cart.totalPrice)
    : items.reduce((sum, i) => sum + Number(i.price) * i.quantity, 0)

  if (items.length === 0) {
    navigate('/cart')
    return null
  }

  const handleCardChange = (e) => setCard({ ...card, [e.target.name]: e.target.value })
  const handleAddressChange = (e) => setAddress({ ...address, [e.target.name]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      // Matches CreateOrderRequest exactly
      const orderPayload = {
        username: user.username,
        items: items.map((i) => ({
          productId: i.productId,
          productName: i.productName,
          quantity: i.quantity,
          price: Number(i.price),
        })),
        firstName: address.firstName,
        lastName: address.lastName,
        streetAddress: address.streetAddress,
        city: address.city,
        country: address.country,
        phone: address.phone,
        email: user.email || '',
        card: {
          cardHolderName: card.cardHolderName,
          cardNumber: card.cardNumber.replace(/\s/g, ''),
          expireMonth: card.expireMonth,
          expireYear: card.expireYear,
          cvc: card.cvc,
        },
      }

      const res = await createOrder(orderPayload)
      await clear()
      navigate(`/orders/${res.data.orderId ?? res.data.id}`, { state: { success: true } })
    } catch (err) {
      setError(err.response?.data?.message || 'Order failed. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const formatCardNumber = (val) =>
    val.replace(/\D/g, '').slice(0, 16).replace(/(.{4})/g, '$1 ').trim()

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <h1 className="text-3xl font-bold text-gray-900 mb-8">Checkout</h1>

      <form onSubmit={handleSubmit} className="flex flex-col lg:flex-row gap-8">
        {/* Left column */}
        <div className="flex-1 space-y-6">
          {/* Shipping Address */}
          <section className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
            <h2 className="font-bold text-gray-900 text-lg mb-5 flex items-center gap-2">
              <span>📍</span> Shipping Address
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">First Name</label>
                <input
                  type="text"
                  name="firstName"
                  value={address.firstName}
                  onChange={handleAddressChange}
                  required
                  className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Last Name</label>
                <input
                  type="text"
                  name="lastName"
                  value={address.lastName}
                  onChange={handleAddressChange}
                  required
                  className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div className="sm:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">Street Address</label>
                <input
                  type="text"
                  name="streetAddress"
                  value={address.streetAddress}
                  onChange={handleAddressChange}
                  required
                  placeholder="123 Main St, Apt 4B"
                  className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">City</label>
                <input
                  type="text"
                  name="city"
                  value={address.city}
                  onChange={handleAddressChange}
                  required
                  className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Country</label>
                <input
                  type="text"
                  name="country"
                  value={address.country}
                  onChange={handleAddressChange}
                  required
                  className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div className="sm:col-span-2">
                <label className="block text-sm font-medium text-gray-700 mb-1">Phone</label>
                <input
                  type="tel"
                  name="phone"
                  value={address.phone}
                  onChange={handleAddressChange}
                  required
                  placeholder="+90 555 000 0000"
                  className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
            </div>
          </section>

          {/* Payment */}
          <section className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6">
            <h2 className="font-bold text-gray-900 text-lg mb-5 flex items-center gap-2">
              <span>💳</span> Payment Information
            </h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Card Holder Name</label>
                <input
                  type="text"
                  name="cardHolderName"
                  value={card.cardHolderName}
                  onChange={handleCardChange}
                  required
                  placeholder="John Doe"
                  className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Card Number</label>
                <input
                  type="text"
                  name="cardNumber"
                  value={card.cardNumber}
                  onChange={(e) => setCard({ ...card, cardNumber: formatCardNumber(e.target.value) })}
                  required
                  placeholder="0000 0000 0000 0000"
                  maxLength={19}
                  className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>
              <div className="grid grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Exp. Month</label>
                  <input
                    type="text"
                    name="expireMonth"
                    value={card.expireMonth}
                    onChange={handleCardChange}
                    required
                    placeholder="12"
                    maxLength={2}
                    className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Exp. Year</label>
                  <input
                    type="text"
                    name="expireYear"
                    value={card.expireYear}
                    onChange={handleCardChange}
                    required
                    placeholder="2026"
                    maxLength={4}
                    className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">CVC</label>
                  <input
                    type="text"
                    name="cvc"
                    value={card.cvc}
                    onChange={handleCardChange}
                    required
                    placeholder="123"
                    maxLength={3}
                    className="w-full border border-gray-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>
            </div>
          </section>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 rounded-xl px-4 py-3 text-sm">
              {error}
            </div>
          )}
        </div>

        {/* Order Summary */}
        <div className="lg:w-80 shrink-0">
          <div className="bg-white rounded-2xl border border-gray-100 shadow-sm p-6 sticky top-24">
            <h2 className="font-bold text-gray-900 text-lg mb-4">Order Summary</h2>
            <div className="space-y-2 mb-4">
              {items.map((item) => (
                <div key={item.productId} className="flex justify-between text-sm text-gray-600">
                  <span className="truncate mr-2">{item.productName} × {item.quantity}</span>
                  <span className="shrink-0">₺{(Number(item.price) * item.quantity).toFixed(2)}</span>
                </div>
              ))}
            </div>
            <div className="border-t border-gray-100 pt-4 flex justify-between font-bold text-gray-900 mb-6">
              <span>Total</span>
              <span className="text-indigo-600 text-xl">₺{total.toFixed(2)}</span>
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-semibold py-3 rounded-xl transition-colors disabled:opacity-60 flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                  Processing…
                </>
              ) : (
                <>🔒 Place Order</>
              )}
            </button>
          </div>
        </div>
      </form>
    </div>
  )
}
