import { useState } from 'react';
import { CheckCircle2, XCircle, MinusCircle, ChevronDown, ChevronUp } from 'lucide-react';
import { formatPercent } from '../../utils/formatters';
import { statusClasses } from '../../theme/statusClasses';
import { ScorePill, ScoreSectionHeader } from '../ui/ScorePill';

function Tick({ ok, partial }) {
  if (partial) {
    return <MinusCircle className="h-4 w-4 shrink-0 text-warning" />;
  }
  return ok
    ? <CheckCircle2 className="h-4 w-4 shrink-0 text-success" />
    : <XCircle className="h-4 w-4 shrink-0 text-error" />;
}

function ClassStatusIcon({ status }) {
  if (status === 'success') {
    return <CheckCircle2 className="h-5 w-5 shrink-0 text-success" />;
  }
  if (status === 'warning') {
    return <MinusCircle className="h-5 w-5 shrink-0 text-warning" />;
  }
  if (status === 'error') {
    return <XCircle className="h-5 w-5 shrink-0 text-error" />;
  }
  return null;
}

function classGradeCounts(cls) {
  const fields = cls.fields ?? [];
  const constructors = cls.constructors ?? [];
  const methods = cls.methods ?? [];
  const allItems = [...fields, ...constructors, ...methods];
  const passCount = allItems.filter((item) => item.ok).length;
  const total = allItems.length;
  const pct = total ? Math.round((passCount / total) * 100) : 100;
  return { passCount, total, pct };
}

function mapClassData(classData) {
  return (Array.isArray(classData) ? classData : []).map((cls) => {
    const fields = (cls.fields ?? []).map((f) => ({
      ...f,
      ok: f.ok ?? f.isCorrect,
      partial: f.partial === true,
    }));
    const constructors = (cls.constructors ?? []).map((c) => ({
      ...c,
      ok: c.ok ?? c.isCorrect,
      partial: c.partial === true,
    }));
    const methods = (cls.methods ?? []).map((m) => ({
      ...m,
      ok: m.ok ?? m.isCorrect,
      partial: m.partial === true,
    }));
    return {
      name: cls.name,
      type: cls.type || 'CLASS',
      status: cls.status,
      fields,
      constructors,
      methods,
    };
  });
}

export default function ClassScoreBreakdown({ classData = [], overallScore = null }) {
  const [expandedClassName, setExpandedClassName] = useState(null);
  const classes = mapClassData(classData);

  const allItems = classes.flatMap((cls) => [...cls.fields, ...cls.constructors, ...cls.methods]);
  const passCount = classes.reduce((sum, cls) => sum + classGradeCounts(cls).passCount, 0);
  const totalCount = classes.reduce((sum, cls) => sum + classGradeCounts(cls).total, 0);
  const overallPct = totalCount ? Math.round((passCount / totalCount) * 100) : 0;

  return (
    <div>
      <ScoreSectionHeader
        title="Declaration Score"
        score={{ ok: passCount, total: totalCount, pct: overallPct }}
        showPill={totalCount > 0}
      />
      {overallScore != null && totalCount === 0 && (
        <div className="mb-4 flex items-center justify-end">
          <span className="text-sm font-semibold text-foreground">{formatPercent(overallScore)}</span>
        </div>
      )}

      {classes.length === 0 ? (
        <div className="rounded-xl border border-dashed border-border p-8 text-center text-sm text-foreground-secondary dark:text-foreground-muted">
          No class detail data is available.
        </div>
      ) : (
        <div className="space-y-3">
          {classes.map((cls) => {
            const items = [...cls.fields, ...cls.constructors, ...cls.methods];
            const { passCount: clsPass, total: clsTotal, pct: clsPct } = classGradeCounts(cls);
            const isOpen = expandedClassName === cls.name;
            return (
              <div key={cls.name} className="overflow-hidden rounded-xl bg-surface shadow-sm">
                <button
                  type="button"
                  onClick={() => setExpandedClassName((current) => (current === cls.name ? null : cls.name))}
                  className="flex w-full items-center justify-between gap-3 bg-surface-secondary px-4 py-3 text-left transition hover:bg-surface-secondary"
                >
                  <div className="flex min-w-0 items-start gap-3">
                    <ClassStatusIcon status={cls.status} />
                    <div className="min-w-0">
                      <span className="text-[10px] uppercase tracking-wider text-foreground-muted">{cls.type}</span>
                      <p className="mt-1 font-mono text-sm font-bold text-foreground">{cls.name}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <ScorePill ok={clsPass} total={clsTotal || 1} pct={clsPct} />
                    {isOpen ? <ChevronUp className="h-4 w-4 text-foreground-muted" /> : <ChevronDown className="h-4 w-4 text-foreground-muted" />}
                  </div>
                </button>

                {isOpen && (
                  <div className="divide-y divide-border border-t border-border divide-border">
                    {cls.fields.length > 0 && (
                      <div className="px-4 py-3">
                        <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-chart-blue">Fields</p>
                        <div className="space-y-2">
                          {cls.fields.map((field, index) => (
                            <div
                              key={`${field.name}-${index}`}
                              className={`flex items-center justify-between rounded-lg px-3 py-2 ${field.ok ? statusClasses('correct') : field.partial ? statusClasses('pending') : statusClasses('incorrect')}`}
                            >
                              <div>
                                <p className="text-xs font-mono font-semibold text-chart-blue dark:text-chart-blue">{field.name}: {field.dataType}</p>
                                <p className="mt-0.5 text-[10px] text-foreground-muted">{field.scope || '—'}</p>
                              </div>
                              <Tick ok={field.ok} partial={field.partial} />
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {cls.constructors.length > 0 && (
                      <div className="px-4 py-3">
                        <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-chart-amber">Constructors</p>
                        <div className="space-y-2">
                          {cls.constructors.map((ctor, index) => (
                            <div
                              key={`${ctor.name}-${index}`}
                              className={`flex items-center justify-between rounded-lg px-3 py-2 ${ctor.ok ? statusClasses('correct') : ctor.partial ? statusClasses('pending') : statusClasses('incorrect')}`}
                            >
                              <div>
                                <p className="text-xs font-mono font-semibold text-chart-amber dark:text-chart-amber">{ctor.name}({ctor.params || ''})</p>
                                <p className="mt-0.5 text-[10px] text-foreground-muted">{ctor.scope || '—'}</p>
                              </div>
                              <Tick ok={ctor.ok} partial={ctor.partial} />
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {cls.methods.length > 0 && (
                      <div className="px-4 py-3">
                        <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-success">Methods</p>
                        <div className="space-y-2">
                          {cls.methods.map((method, index) => (
                            <div
                              key={`${method.name}-${index}`}
                              className={`flex items-center justify-between rounded-lg px-3 py-2 ${method.ok ? statusClasses('correct') : method.partial ? statusClasses('pending') : statusClasses('incorrect')}`}
                            >
                              <div>
                                <p className="text-xs font-mono font-semibold text-success">{method.name}(): {method.returnType}</p>
                                <p className="mt-0.5 text-[10px] text-foreground-muted">{method.scope || '—'}</p>
                              </div>
                              <Tick ok={method.ok} partial={method.partial} />
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export function collectIncorrectExportRows(classData, studentName) {
  const rows = [];
  mapClassData(classData).forEach((cls) => {
    cls.methods.filter((method) => !method.ok).forEach((method) => {
      rows.push({
        'Student Name': studentName,
        Source: 'Class',
        'Incorrect Class': cls.name,
        'Item Type': 'Method',
        'Incorrect Item': method.name,
        Error: '',
      });
    });
  });
  return rows;
}
