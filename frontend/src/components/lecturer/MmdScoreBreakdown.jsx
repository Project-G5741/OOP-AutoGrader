import { useState } from 'react';
import { CheckCircle2, XCircle, ChevronDown, ChevronUp, GitMerge } from 'lucide-react';
import { statusClasses } from '../../theme/statusClasses';
import { ScorePill, ScoreSectionHeader } from '../ui/ScorePill';
import { formatMmdRelationType } from '../../utils/formatters';

function Tick({ ok }) {
  return ok
    ? <CheckCircle2 className="h-4 w-4 shrink-0 text-success" />
    : <XCircle className="h-4 w-4 shrink-0 text-error" />;
}

function relationTypeStyle(type) {
  const normalized = String(type ?? '').toLowerCase();
  if (normalized.includes('extends')) {
    return 'bg-chart-blue/10 text-chart-blue';
  }
  if (normalized.includes('implements')) {
    return 'bg-chart-amber/10 text-chart-amber';
  }
  if (normalized.includes('uses') || normalized.includes('depends')) {
    return 'bg-chart-teal/10 text-chart-teal';
  }
  if (normalized.includes('associates') || normalized.includes('aggregates')) {
    return 'bg-primary-light text-primary';
  }
  return 'bg-surface-secondary text-foreground-secondary';
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
  if (normalized === 'field') return 'text-chart-blue';
  if (normalized === 'method') return 'text-success';
  if (normalized === 'constructor') return 'text-chart-amber';
  return 'text-foreground-secondary';
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
      <p className="text-sm text-warning-text">{mmdError}</p>
    );
  }

  return (
    <div>
      <ScoreSectionHeader
        title="MMD Score"
        score={{ ok: attrPass, total: attrTotal, pct: attrPct }}
        showPill={attrTotal > 0}
      />

      {classes.length === 0 ? (
        <div className="rounded-xl border border-dashed border-border p-8 text-center text-sm text-foreground-secondary">
          No MMD class data is available.
        </div>
      ) : (
        <div className="space-y-3">
          {classes.map((cls) => {
            const items = cls.attributes;
            const clsPass = items.filter((item) => item.ok).length;
            const clsPct = items.length ? Math.round((clsPass / items.length) * 100) : 100;
            const isOpen = expandedClassName === cls.name;
            return (
              <div key={cls.name} className="overflow-hidden rounded-xl bg-surface shadow-sm">
                <button
                  type="button"
                  onClick={() => setExpandedClassName((current) => (current === cls.name ? null : cls.name))}
                  className="flex w-full items-center justify-between gap-3 bg-surface-secondary px-4 py-3 text-left transition hover:bg-surface-secondary"
                >
                  <p className="font-mono text-sm font-bold text-foreground">{cls.name}</p>
                  <div className="flex items-center gap-2">
                    <ScorePill ok={clsPass} total={items.length || 1} pct={clsPct} />
                    {isOpen ? <ChevronUp className="h-4 w-4 text-foreground-muted" /> : <ChevronDown className="h-4 w-4 text-foreground-muted" />}
                  </div>
                </button>

                {isOpen && (
                  <div className="divide-y divide-border border-t border-border">
                    <div className="px-4 py-3">
                      <div className="space-y-2">
                        {items.map((attr, index) => (
                          <div
                            key={`${attr.name}-${index}`}
                            className={`flex items-start justify-between gap-2 rounded-lg px-3 py-2 ${attr.ok ? statusClasses('correct') : statusClasses('incorrect')}`}
                          >
                            <div className="min-w-0 flex-1">
                              <p className={`text-xs font-mono font-semibold break-words ${attributeTypeColor(attr.type)}`}>
                                {attr.name}
                              </p>
                              {!attr.ok && attr.error && (
                                <p className="mt-1 text-[10px] text-error-text">{attr.error}</p>
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

      {relations.length > 0 && (
        <div className="mt-4 overflow-hidden rounded-xl border border-border">
          <button
            type="button"
            onClick={() => setRelationsOpen((open) => !open)}
            className="flex w-full items-center justify-between gap-3 bg-surface-secondary px-4 py-3 text-left transition hover:bg-surface-secondary"
          >
            <div className="flex items-center gap-2 text-foreground-secondary">
              <GitMerge className="h-4 w-4" />
              <span className="text-xs font-semibold uppercase tracking-[0.2em]">Relations</span>
            </div>
            <div className="flex items-center gap-2">
              <ScorePill ok={relationPass} total={relations.length} pct={relationPct} />
              {relationsOpen ? <ChevronUp className="h-4 w-4 text-foreground-muted" /> : <ChevronDown className="h-4 w-4 text-foreground-muted" />}
            </div>
          </button>

          {relationsOpen && (
            <>
              <div className="grid grid-cols-4 items-center gap-4 border-t border-border px-4 py-3 text-[11px] uppercase tracking-[0.25em] text-foreground-muted">
                <span className="font-semibold">From</span>
                <div className="flex justify-center"><span className="font-semibold">Relation</span></div>
                <span className="font-semibold">To</span>
                <span className="font-semibold text-center">Status</span>
              </div>
              <div className="divide-y divide-border">
                {relations.map((r, index) => (
                  <div key={index}>
                    <div className="grid grid-cols-4 items-center gap-4 px-4 py-3 text-sm text-foreground">
                      <span className="font-mono text-primary">{r.from}</span>
                      <div className="flex justify-center">
                        <span className={`inline-flex items-center justify-center rounded-full px-3 py-1 text-[11px] font-semibold ${relationTypeStyle(r.relType)}`}>
                          {formatMmdRelationType(r.relType)}
                        </span>
                      </div>
                      <span className="font-mono text-primary">{r.to}</span>
                      <div className="flex justify-center">
                        <Tick ok={r.ok} />
                      </div>
                    </div>
                    {!r.ok && r.error && (
                      <div className="px-4 py-2 text-xs font-mono bg-error-bg text-error-text">
                        {r.from} → {r.to}: {r.error}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      )}
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
      'Incorrect Item': `${r.from} → ${formatMmdRelationType(r.relType)} → ${r.to}`,
      Error: r.error || '',
    });
  });

  return rows;
}
