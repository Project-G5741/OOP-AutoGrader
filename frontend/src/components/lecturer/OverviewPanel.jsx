export default function OverviewPanel({ overviewCards }) {
  return (
    <div className="grid grid-cols-1 gap-6 md:grid-cols-2 xl:grid-cols-4">
      {overviewCards.map((card) => (
        <div key={card.title} className={`rounded-3xl border p-5 shadow-sm transition-colors ${card.accent} dark:border-gray-700 dark:bg-[#1e2530]`}>
          <div className="flex items-center justify-between gap-4">
            <div>
              <p className="text-sm font-medium text-gray-500 dark:text-gray-400">{card.title}</p>
              <p className="mt-3 text-3xl font-semibold text-gray-900 dark:text-white">{card.value}</p>
            </div>
            <div className="rounded-2xl bg-white/70 p-3 text-gray-700 dark:bg-white/10 dark:text-white">
              {card.icon}
            </div>
          </div>
          <p className="mt-4 text-sm text-gray-500 dark:text-gray-400">{card.subtitle}</p>
        </div>
      ))}
    </div>
  );
}
