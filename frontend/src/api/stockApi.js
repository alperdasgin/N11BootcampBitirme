import api from './axios'

// GET /api/stocks → tüm stokları döner
export const getAllStocks = () => api.get('/stocks')

// GET /api/stocks/{productId} → tek ürün stok bilgisi
export const getStock = (productId) => api.get(`/stocks/${productId}`)
