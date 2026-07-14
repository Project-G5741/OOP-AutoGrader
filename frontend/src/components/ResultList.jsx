import { CheckCircle2, XCircle } from 'lucide-react';

export default function ResultList({ title, actionText, items }) {
  return (
    <div className="bg-white dark:bg-[#13131A] rounded-xl p-6 shadow-sm dark:shadow-none transition-colors">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-gray-900 dark:text-white font-medium">{title}</h2>
        <span className="text-purple-500 dark:text-purple-400 text-sm cursor-pointer hover:underline">
          {actionText}
        </span>
      </div>

      <div className="space-y-3">
        {items.map((item, index) => (
          <div key={index} className="flex items-center justify-between py-2 border-b border-gray-50 dark:border-gray-800/50 last:border-0">
            <span className="text-gray-700 dark:text-gray-300">{item.name}</span>
            {item.status === "success" ? (
              <CheckCircle2 className="w-5 h-5 text-green-500" />
            ) : (
              <XCircle className="w-5 h-5 text-red-500" />
            )}
          </div>
        ))}
      </div>
    </div>
  );
}