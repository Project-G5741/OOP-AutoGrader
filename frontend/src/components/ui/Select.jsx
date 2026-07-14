export default function Select({ label, options, className = "" }) {
  return (
    <div className={`bg-white dark:bg-[#13131A] rounded-xl p-4 shadow-sm dark:shadow-none ${className}`}>
      {label && <label className="block text-gray-500 dark:text-gray-400 text-sm mb-2">{label}</label>}
      <select className="w-full bg-gray-50 dark:bg-[#1A1A24] text-gray-900 dark:text-white px-4 py-2.5 rounded-lg border border-gray-200 dark:border-gray-700 focus:border-purple-500 focus:outline-none transition-colors">
        {options.map((opt, idx) => (
          <option key={idx} value={opt}>{opt}</option>
        ))}
      </select>
    </div>
  );
}