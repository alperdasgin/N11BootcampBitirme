import { useState, useEffect, useCallback } from 'react'
import { getProducts, createProduct, updateProduct, deleteProduct } from '../api/productApi'
import { getAllStocks } from '../api/stockApi'
import { useAuth } from '../context/AuthContext'
import { useNavigate } from 'react-router-dom'
import LoadingSpinner from '../components/LoadingSpinner'

export default function AdminDashboard() {
  const { user } = useAuth()
  const navigate = useNavigate()
  
  const [products, setProducts] = useState([])
  const [stockMap, setStockMap] = useState({}) // productId → { availableQuantity, reservedQuantity }
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [formData, setFormData] = useState({
    name: '', description: '', price: '', stock: '', category: '', images: ['']
  })

  useEffect(() => {
    // Güvenlik kontrolü (Sadece Admin girebilir)
    if (!user || user.role !== 'ADMIN') {
      navigate('/')
      return
    }
    fetchProducts()
  }, [user, navigate])

  const fetchProducts = useCallback(async () => {
    setLoading(true)
    try {
      const [productsRes, stocksRes] = await Promise.all([
        getProducts(0, 100),
        getAllStocks()
      ])
      setProducts(productsRes.data.content)

      // productId → stok verisi map'i oluştur
      const map = {}
      ;(stocksRes.data || []).forEach(s => {
        map[s.productId] = {
          availableQuantity: s.availableQuantity,
          reservedQuantity: s.reservedQuantity
        }
      })
      setStockMap(map)
    } catch (err) {
      console.error(err)
    } finally {
      setLoading(false)
    }
  }, [])

  const handleInputChange = (e) => {
    const { name, value } = e.target
    setFormData((prev) => ({ ...prev, [name]: value }))
  }

  const openAddModal = () => {
    setEditingId(null)
    setFormData({ name: '', description: '', price: '', stock: '', category: '', images: [''] })
    setShowModal(true)
  }

  const openEditModal = (product) => {
    setEditingId(product.id)
    setFormData({
      name: product.name,
      description: product.description,
      price: product.price,
      stock: product.stock,
      category: product.category,
      images: product.images?.length > 0 ? product.images : ['']
    })
    setShowModal(true)
  }

  const handleImageChange = (index, value) => {
    const newImages = [...formData.images]
    newImages[index] = value
    setFormData({ ...formData, images: newImages })
  }

  const addImageField = () => {
    setFormData({ ...formData, images: [...formData.images, ''] })
  }

  const removeImageField = (index) => {
    const newImages = formData.images.filter((_, i) => i !== index)
    if (newImages.length === 0) newImages.push('')
    setFormData({ ...formData, images: newImages })
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Bu ürünü silmek istediğinize emin misiniz?')) return
    try {
      await deleteProduct(id)
      fetchProducts()
    } catch (err) {
      alert('Hata oluştu')
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    try {
      if (editingId) {
        await updateProduct(editingId, formData)
      } else {
        await createProduct(formData)
      }
      setShowModal(false)
      fetchProducts()
    } catch (err) {
      alert('Ürün kaydedilirken hata oluştu')
    }
  }

  if (loading) return <LoadingSpinner message="Ürünler yükleniyor..." />

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Admin Panel</h1>
          <p className="text-gray-500 mt-1">Ürün Envanteri Yönetimi</p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={fetchProducts}
            className="bg-gray-100 hover:bg-gray-200 text-gray-700 px-4 py-2.5 rounded-xl font-semibold shadow-sm transition-colors"
          >
            ↻ Stokları Yenile
          </button>
          <button
            onClick={openAddModal}
            className="bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2.5 rounded-xl font-semibold shadow-sm transition-colors"
          >
            + Yeni Ürün Ekle
          </button>
        </div>
      </div>

      {/* Product Table */}
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
        <table className="min-w-full divide-y divide-gray-200">
          <thead className="bg-gray-50">
            <tr>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">ID / Ürün</th>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Kategori</th>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Fiyat</th>
              <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Stok (Mevcut / Rezerve)</th>
              <th className="px-6 py-3 text-right text-xs font-semibold text-gray-500 uppercase tracking-wider">İşlemler</th>
            </tr>
          </thead>
          <tbody className="bg-white divide-y divide-gray-200">
            {products.map((product) => (
              <tr key={product.id} className="hover:bg-gray-50 transition-colors">
                <td className="px-6 py-4 whitespace-nowrap">
                  <div className="flex flex-col">
                    <span className="text-sm font-medium text-gray-900">#{product.id} {product.name}</span>
                    <span className="text-xs text-gray-400 truncate max-w-xs">{product.description}</span>
                  </div>
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  <span className="px-2.5 py-1 text-xs font-semibold bg-indigo-50 text-indigo-700 rounded-full">
                    {product.category}
                  </span>
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-sm font-semibold text-gray-900">
                  ₺{Number(product.price).toFixed(2)}
                </td>
                <td className="px-6 py-4 whitespace-nowrap">
                  {stockMap[product.id] !== undefined ? (
                    <div className="flex items-center gap-2">
                      <span className={`px-2 py-1 text-xs font-bold rounded-md ${
                        stockMap[product.id].availableQuantity > 10
                          ? 'bg-green-100 text-green-700'
                          : stockMap[product.id].availableQuantity > 0
                          ? 'bg-yellow-100 text-yellow-700'
                          : 'bg-red-100 text-red-700'
                      }`}>
                        {stockMap[product.id].availableQuantity} Mevcut
                      </span>
                      {stockMap[product.id].reservedQuantity > 0 && (
                        <span className="px-2 py-1 text-xs font-bold rounded-md bg-blue-100 text-blue-700">
                          {stockMap[product.id].reservedQuantity} Rezerve
                        </span>
                      )}
                    </div>
                  ) : (
                    <span className="px-2 py-1 text-xs font-bold rounded-md bg-gray-100 text-gray-500">
                      Stok kaydı yok
                    </span>
                  )}
                </td>
                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <button onClick={() => openEditModal(product)} className="text-indigo-600 hover:text-indigo-900 mr-4">Düzenle</button>
                  <button onClick={() => handleDelete(product.id)} className="text-red-600 hover:text-red-900">Sil</button>
                </td>
              </tr>
            ))}
            {products.length === 0 && (
              <tr>
                <td colSpan="5" className="px-6 py-10 text-center text-gray-500">
                  Kayıtlı ürün bulunamadı.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Add/Edit Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50 backdrop-blur-sm p-4">
          <div className="bg-white rounded-2xl shadow-xl max-w-lg w-full max-h-[90vh] overflow-y-auto">
            <div className="p-6 border-b border-gray-100 flex justify-between items-center">
              <h2 className="text-xl font-bold text-gray-900">{editingId ? 'Ürün Düzenle' : 'Yeni Ürün Ekle'}</h2>
              <button onClick={() => setShowModal(false)} className="text-gray-400 hover:text-gray-600 font-bold text-xl">&times;</button>
            </div>
            
            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Ürün Adı</label>
                <input required name="name" value={formData.name} onChange={handleInputChange} className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none" />
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Açıklama</label>
                <textarea required name="description" value={formData.description} onChange={handleInputChange} rows="3" className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none"></textarea>
              </div>

              <div className="flex gap-4">
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Fiyat (₺)</label>
                  <input required type="number" step="0.01" name="price" value={formData.price} onChange={handleInputChange} className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none" />
                </div>
                <div className="flex-1">
                  <label className="block text-sm font-medium text-gray-700 mb-1">Stok Adedi</label>
                  <input required type="number" name="stock" value={formData.stock} onChange={handleInputChange} className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none" />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Kategori</label>
                <input required name="category" value={formData.category} onChange={handleInputChange} className="w-full px-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none" />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Resim URL'leri</label>
                {formData.images.map((img, index) => (
                  <div key={index} className="flex gap-2 mb-2">
                    <input 
                      value={img} 
                      onChange={(e) => handleImageChange(index, e.target.value)} 
                      placeholder="https://..." 
                      className="flex-1 px-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-indigo-500 outline-none" 
                    />
                    <button type="button" onClick={() => removeImageField(index)} className="px-3 py-2 text-red-500 hover:bg-red-50 rounded-xl transition-colors">
                      Sil
                    </button>
                  </div>
                ))}
                <button type="button" onClick={addImageField} className="text-sm text-indigo-600 font-semibold mt-1 hover:underline">
                  + Yeni Resim Ekle
                </button>
              </div>

              <div className="pt-4 flex justify-end gap-3">
                <button type="button" onClick={() => setShowModal(false)} className="px-5 py-2.5 text-sm font-semibold text-gray-600 hover:bg-gray-100 rounded-xl transition-colors">İptal</button>
                <button type="submit" className="px-5 py-2.5 text-sm font-semibold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl shadow-sm transition-colors">
                  {editingId ? 'Güncelle' : 'Kaydet'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
