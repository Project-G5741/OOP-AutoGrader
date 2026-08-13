import { CheckCircle2, XCircle } from 'lucide-react';

export default function ResultList({ title, actionText, items }) {
  return (
    <div className="bg-surface rounded-xl p-6 shadow-sm dark:shadow-none transition-colors">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-foreground font-medium">{title}</h2>
        <span className="text-primary dark:text-primary text-sm cursor-pointer hover:underline">
          {actionText}
        </span>
      </div>

      <div className="space-y-3">
        {items.map((item, index) => (
          <div key={index} className="flex items-center justify-between py-2 border-b border-border/50 last:border-0">
            <span className="text-foreground-secondary">{item.name}</span>
            {item.status === "success" ? (
              <CheckCircle2 className="w-5 h-5 text-success" />
            ) : (
              <XCircle className="w-5 h-5 text-error" />
            )}
          </div>
        ))}
      </div>
    </div>
  );
}