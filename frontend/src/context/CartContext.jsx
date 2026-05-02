import { createContext, useContext, useState, useCallback, useEffect } from 'react'
import { getCart, addToCart, removeFromCart, updateQuantity, clearCart } from '../api/cartApi'
import { useAuth } from './AuthContext'

const CartContext = createContext(null)

export function CartProvider({ children }) {
  const { user } = useAuth()
  const [cart, setCart] = useState(null)
  const [loading, setLoading] = useState(false)

  const fetchCart = useCallback(async () => {
    if (!user) { setCart(null); return }
    try {
      setLoading(true)
      const res = await getCart(user.username)
      setCart(res.data)
    } catch {
      setCart(null)
    } finally {
      setLoading(false)
    }
  }, [user])

  useEffect(() => {
    fetchCart()
  }, [fetchCart])

  // product: { id, name, price }
  const add = useCallback(async (product, quantity = 1) => {
    if (!user) return
    const res = await addToCart(user.username, product.id, product.name, product.price, quantity)
    setCart(res.data)
  }, [user])

  const remove = useCallback(async (productId) => {
    if (!user) return
    const res = await removeFromCart(user.username, productId)
    setCart(res.data)
  }, [user])

  const update = useCallback(async (productId, quantity) => {
    if (!user) return
    if (quantity <= 0) return remove(productId)
    const res = await updateQuantity(user.username, productId, quantity)
    setCart(res.data)
  }, [user, remove])

  const clear = useCallback(async () => {
    if (!user) return
    await clearCart(user.username)
    setCart(null)
  }, [user])

  const itemCount = cart?.items?.reduce((sum, i) => sum + i.quantity, 0) ?? 0

  return (
    <CartContext.Provider value={{ cart, loading, fetchCart, add, remove, update, clear, itemCount }}>
      {children}
    </CartContext.Provider>
  )
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used within CartProvider')
  return ctx
}
