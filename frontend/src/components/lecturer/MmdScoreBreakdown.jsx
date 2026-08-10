import { useState } from 'react';
import { CheckCircle2, XCircle, ChevronDown, ChevronUp, GitMerge } from 'lucide-react';

function Tick({ ok }) {
  return ok
    ? <CheckCircle2 className="h-4 w-4 shrink-0 text-green-500" />
    : <XCircle className="h-4 w-4 shrink-0 text-red-500" />;
}

function ScorePill({ ok, total, pct }) {
  const color = pct >= 80
    ? 'bg-green-500/15 text-green-600 dark:text-green-400 border-green-300 dark:border-green-700'
    : pct >= 60
      ? 'bg-yellow-500/15 text-yellow-600 dark:text-yellow-400 border-yellow-300 dark:border-yellow-700'
      : 'bg-red-500/15 text-red-600 dark:text-red-400 border-red-300 dark:border-red-700';

  return (
    <span className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-semibold ${color}`}>
      {ok}/{total} · {pct}%
    </span>
  );
}

function relationTypeStyle(type) {
  const normalized = String(type ?? '').toLowerCase();
  if (normalized.includes('extends')) {
    return 'bg-blue-500/10 text-blue-500 dark:bg-blue-500/15 dark:text-blue-300';
  }
  if (normalized.includes('implements')) {
    return 'bg-orange-500/10 text-orange-500 dark:bg-orange-500/15 dark:text-orange-300';
  }
  if (normalized.includes('uses') || normalized.includes('depends')) {
    return 'bg-teal-500/10 text-teal-500 dark:bg-teal-500/15 dark:text-teal-300';
  }
  if (normalized.includes('associates') || normalized.includes('aggregates')) {
    return 'bg-purple-500/10 text-purple-500 dark:bg-purple-500/15 dark:text-purple-300';
  }
  return 'bg-slate-100 text-slate-700 dark:bg-gray-800 dark:text-slate-200';
}

function attributeTypeLabel(type) {
  const normalized = String(type ?? '').toLowerCase();
  if (normalized === 'field') return 'Field';
  if (normalized === 'method') return 'Method';
  if (normalized === 'constructor') return 'Constructor';
  if (normalized === 'stereotype') return 'Stereotype';
  return 'Attribute';
}

function attributeTypeColor(type) {
  const normalized = String(type ?? '').toLowerCase();
  if (normalized === 'field') return 'text-blue-600 dark:text-blue-400';
  if (normalized === 'method') return 'text-green-600 dark:text-green-400';
  if (normalized === 'constructor') return 'text-orange-500 dark:text-orange-400';
  return 'text-gray-700 dark:text-gray-300';
}

function mapMmdData(mmdData) {
  return (Array.isArray(mmdData) ? mmdData : []).map((cls) => ({
    name: cls.name,
    attributes: (cls.attributes ?? []).map((a) => ({ ...a, ok: a.ok ?? false })),
    relations: (cls.relations ?? []).map((r) => ({ ...r, ok: r.ok ?? false })),
  }));
}

export default function MmdScoreBreakdown({ mmdData = [], mmdError = null }) {
  const [expandedClassName, setExpandedClassName] = useState(null);
  const [relationsOpen, setRelationsOpen] = useState(false);
  const classes = mapMmdData(mmdData);

  const relations = classes.flatMap((cls) => cls.relations ?? []);
  const relationPass = relations.filter((r) => r.ok).length;
  const relationPct = relations.length ? Math.round((relationPass / relations.length) * 100) : 100;

  const allAttributes = classes.flatMap((cls) => cls.attributes);
  const attrPass = allAttributes.filter((a) => a.ok).length;
  const attrTotal = allAttributes.length;
  const attrPct = attrTotal ? Math.round((attrPass / attrTotal) * 100) : 0;

  if (mmdError) {
    return (
      <p className="text-sm text-amber-700 dark:text-amber-300">{mmdError}</p>
    );
  }

  return (
    <div>
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-gray-700 dark:text-gray-200">MMD Score</p>
        </div>
        {attrTotal > 0 && (
          <ScorePill ok={attrPass} total={attrTotal} pct={attrPct} />
        )}
      </div>

      {classes.length === 0 ? (
        <div className="rounded-xl border border-dashed border-gray-200 p-8 text-center text-sm text-gray-500 dark:border-gray-700 dark:text-gray-400">
          No MMD class data is available.
        </div>
      ) : (
        <div className="space-y-3">
          {classes.map((cls) => {
            const items = cls.attributes;
            const clsPass = items.filter((item) => item.ok).length;
            const clsPct = items.length ? Math.round((clsPass / items.length) * 100) : 100;
            const isOpen = expandedClassName === cls.name;
            const classTone = clsPct >= 80
              ? 'border-green-300/60 dark:border-green-700/50'
              : clsPct >= 60
                ? 'border-yellow-300/60 dark:border-yellow-700/50'
                : 'border-red-300/60 dark:border-red-700/50';

            return (
              <div key={cls.name} className={`overflow-hidden rounded-xl border bg-white shadow-sm dark:bg-[#1e2530] ${classTone}`}>
                <button
                  type="button"
                  onClick={() => setExpandedClassName((current) => (current === cls.name ? null : cls.name))}
                  className="flex w-full items-center justify-between gap-3 bg-gray-50 px-4 py-3 text-left transition hover:bg-gray-100 dark:bg-[#151b24] dark:hover:bg-[#1a2235]"
                >
                  <p className="font-mono text-sm font-bold text-gray-900 dark:text-white">{cls.name}</p>
                  <div className="flex items-center gap-2">
                    <ScorePill ok={clsPass} total={items.length || 1} pct={clsPct} />
                    {isOpen ? <ChevronUp className="h-4 w-4 text-gray-400" /> : <ChevronDown className="h-4 w-4 text-gray-400" />}
                  </div>
                </button>

                {isOpen && (
                  <div className="divide-y divide-gray-100 border-t border-gray-200 dark:divide-gray-800 dark:border-gray-700">
                    <div className="px-4 py-3">
                      <div className="space-y-2">
                        {items.map((attr, index) => (
                          <div
                            key={`${attr.name}-${index}`}
                            className={`flex items-start justify-between gap-2 rounded-lg px-3 py-2 ${attr.ok ? 'bg-gray-50 dark:bg-[#0d1117]/40' : 'border border-red-200 bg-red-50 dark:border-red-800/40 dark:bg-red-900/10'}`}
                          >
                            <div className="min-w-0 flex-1">
                              <p className={`text-xs font-mono font-semibold break-words ${attributeTypeColor(attr.type)}`}>
                                {attr.name}
                              </p>
                              {!attr.ok && attr.error && (
                                <p className="mt-1 text-[10px] text-red-600 dark:text-red-300">{attr.error}</p>
                              )}
                            </div>
                            <Tick ok={attr.ok} />
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      <div className="mt-4 overflow-hidden rounded-xl border border-gray-200 dark:border-gray-700">
        <button
          type="button"
          onClick={() => setRelationsOpen((open) => !open)}
          className="flex w-full items-center justify-between gap-3 bg-gray-50 px-4 py-3 text-left transition hover:bg-gray-100 dark:bg-[#151b24] dark:hover:bg-[#1a2235]"
        >
          <div className="flex items-center gap-2 text-gray-500 dark:text-gray-400">
            <GitMerge className="h-4 w-4" />
            <span className="text-xs font-semibold uppercase tracking-[0.2em]">Relations</span>
          </div>
          <div className="flex items-center gap-2">
            {relations.length > 0 && (
              <ScorePill ok={relationPass} total={relations.length} pct={relationPct} />
            )}
            {relationsOpen ? <ChevronUp className="h-4 w-4 text-gray-400" /> : <ChevronDown className="h-4 w-4 text-gray-400" />}
          </div>
        </button>

        {relationsOpen && (
          <>
            <div className="grid grid-cols-4 items-center gap-4 border-t border-gray-200 px-4 py-3 text-[11px] uppercase tracking-[0.25em] text-gray-500 dark:border-gray-700 dark:text-gray-400">
              <span className="font-semibold">From</span>
              <div className="flex justify-center"><span className="font-semibold">Relation</span></div>
              <span className="font-semibold">To</span>
              <span className="font-semibold text-center">Status</span>
            </div>
            <div className="divide-y divide-gray-100 dark:divide-gray-800">
              {relations.length > 0 ? relations.map((r, index) => (
                <div key={index}>
                  <div className="grid grid-cols-4 items-center gap-4 px-4 py-3 text-sm text-gray-800 dark:text-gray-200">
                    <span className="font-mono text-purple-600 dark:text-purple-400">{r.from}</span>
                    <div className="flex justify-center">
                      <span className={`inline-flex items-center justify-center rounded-full px-3 py-1 text-[11px] font-semibold ${relationTypeStyle(r.relType)}`}>
                        {r.relType}
                      </span>
                    </div>
                    <span className="font-mono text-purple-600 dark:text-purple-400">{r.to}</span>
                    <div className="flex justify-center">
                      <Tick ok={r.ok} />
                    </div>
                  </div>
                  {!r.ok && r.error && (
                    <div className="px-4 py-2 text-xs font-mono text-red-600 bg-red-50 dark:text-red-300 dark:bg-red-900/10">
                      {r.from} → {r.to}: {r.error}
                    </div>
                  )}
                </div>
              )) : (
                <div className="px-4 py-6 text-center text-xs text-gray-500 dark:text-gray-400">
                  No relation data is available.
                </div>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export function collectIncorrectMmdExportRows(mmdData, studentName) {
  const rows = [];
  mapMmdData(mmdData).forEach((cls) => {
    cls.attributes.filter((attr) => !attr.ok).forEach((attr) => {
      rows.push({
        'Student Name': studentName,
        Source: 'MMD',
        'Incorrect Class': cls.name,
        'Item Type': attributeTypeLabel(attr.type),
        'Incorrect Item': attr.name,
        Error: attr.error || '',
      });
    });
  });

  mapMmdData(mmdData).flatMap((cls) => cls.relations).filter((r) => !r.ok).forEach((r) => {
    rows.push({
      'Student Name': studentName,
      Source: 'MMD',
      'Incorrect Class': r.from,
      'Item Type': 'Relation',
      'Incorrect Item': `${r.from} → ${r.relType} → ${r.to}`,
      Error: r.error || '',
    });
  });

  return rows;
}
