export default function OverviewPanel({ overviewCards, onSelect }) {
  return (
    <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
      {overviewCards.map((card) => (
        <button
          key={card.id}
          type="button"
          onClick={() => onSelect(card.id)}
          className={`cursor-pointer rounded-3xl border border-border-subtle p-5 text-left transition hover:shadow-md focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2 dark:border-transparent dark:shadow-none ${card.accent}`}
        >
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-medium text-foreground-secondary">{card.title}</p>
              <p className="mt-3 text-2xl font-semibold text-foreground sm:text-3xl">{card.value}</p>
              <p className="mt-2 text-xs font-medium text-foreground-secondary">View list</p>
            </div>
            <div className="rounded-2xl bg-surface/60 p-3 text-foreground-secondary dark:bg-black/20">
              {card.icon}
            </div>
          </div>
        </button>
      ))}
    </div>
  );
}
