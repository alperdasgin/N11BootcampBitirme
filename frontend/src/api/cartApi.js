import api from './axios'

// GET /api/cart/{username}
export const getCart = (username) => api.get(`/cart/${username}`)

// POST /api/cart/{username}/add  body: { productId, productName, price, quantity }
export const addToCart = (username, productId, productName, price, quantity) =>
  api.post(`/cart/${username}/add`, { productId, productName, price, quantity })

// DELETE /api/cart/{username}/remove/{productId}
export const removeFromCart = (username, productId) =>
  api.delete(`/cart/${username}/remove/${productId}`)

// PUT /api/cart/{username}/update/{productId}?quantity=N  (quantity is @RequestParam!)
export const updateQuantity = (username, productId, quantity) =>
  api.put(`/cart/${username}/update/${productId}`, null, { params: { quantity } })

// DELETE /api/cart/{username}/clear
export const clearCart = (username) => api.delete(`/cart/${username}/clear`)
