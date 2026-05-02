import api from './axios'

// GET /api/products?page=0&size=12&sortBy=createdAt
// Response: PageResponse { content, pageNumber, pageSize, totalElements, totalPages, last }
export const getProducts = (page = 0, size = 12, sortBy = 'createdAt') =>
  api.get('/products', { params: { page, size, sortBy } })

// GET /api/products/category/{category}?page=0&size=12
export const getProductsByCategory = (category, page = 0, size = 12) =>
  api.get(`/products/category/${encodeURIComponent(category)}`, { params: { page, size } })

// GET /api/products/{id}
export const getProduct = (id) => api.get(`/products/${id}`)

// GET /api/products/categories  → string[]
export const getCategories = () => api.get('/products/categories')

// GET /api/products/search?keyword=…&page=0&size=12
export const searchProducts = (keyword, page = 0, size = 12) =>
  api.get('/products/search', { params: { keyword, page, size } })
