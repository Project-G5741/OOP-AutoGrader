export default function OverviewPanel({ overviewCards }) {
  return (
    <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
      {overviewCards.map((card) => (
        <div
          key={card.title}
          className={`rounded-3xl border border-border-subtle p-5 transition-colors ${card.accent}`}
        >
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-medium text-foreground-secondary">{card.title}</p>
              <p className="mt-3 text-3xl font-semibold text-foreground">{card.value}</p>
            </div>
            <div className="rounded-2xl bg-surface/60 p-3 text-foreground-secondary dark:bg-black/20">
              {card.icon}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
