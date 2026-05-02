import api from './axios'

// POST /api/orders
export const createOrder = (data) => api.post('/orders', data)

// GET /api/orders/user/{username}
export const getMyOrders = (username) => api.get(`/orders/user/${username}`)

// GET /api/orders/{id}
export const getOrder = (id) => api.get(`/orders/${id}`)
