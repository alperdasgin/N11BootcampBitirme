export default function ErrorMessage({ message, onRetry }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 gap-4">
      <div className="text-5xl">⚠️</div>
      <p className="text-red-600 font-medium text-center max-w-md">{message}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="bg-indigo-600 hover:bg-indigo-700 text-white px-6 py-2 rounded-lg font-medium transition-colors"
        >
          Try Again
        </button>
      )}
    </div>
  )
}
