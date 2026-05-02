export default function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null

  const pages = []
  const delta = 2
  const left = Math.max(0, page - delta)
  const right = Math.min(totalPages - 1, page + delta)

  for (let i = left; i <= right; i++) {
    pages.push(i)
  }

  return (
    <div className="flex items-center justify-center gap-1 mt-8">
      <button
        onClick={() => onPageChange(page - 1)}
        disabled={page === 0}
        className="px-3 py-2 rounded-lg border border-gray-200 text-sm font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
      >
        ← Prev
      </button>

      {left > 0 && (
        <>
          <button
            onClick={() => onPageChange(0)}
            className="px-3 py-2 rounded-lg border border-gray-200 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors"
          >
            1
          </button>
          {left > 1 && <span className="px-2 text-gray-400">…</span>}
        </>
      )}

      {pages.map((p) => (
        <button
          key={p}
          onClick={() => onPageChange(p)}
          className={`px-3 py-2 rounded-lg border text-sm font-medium transition-colors ${
            p === page
              ? 'bg-indigo-600 border-indigo-600 text-white'
              : 'border-gray-200 text-gray-600 hover:bg-gray-50'
          }`}
        >
          {p + 1}
        </button>
      ))}

      {right < totalPages - 1 && (
        <>
          {right < totalPages - 2 && <span className="px-2 text-gray-400">…</span>}
          <button
            onClick={() => onPageChange(totalPages - 1)}
            className="px-3 py-2 rounded-lg border border-gray-200 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors"
          >
            {totalPages}
          </button>
        </>
      )}

      <button
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
        className="px-3 py-2 rounded-lg border border-gray-200 text-sm font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
      >
        Next →
      </button>
    </div>
  )
}
