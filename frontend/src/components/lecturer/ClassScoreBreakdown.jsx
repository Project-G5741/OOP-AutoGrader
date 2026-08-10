import { useState } from 'react';
import { CheckCircle2, XCircle, ChevronDown, ChevronUp } from 'lucide-react';
import { formatPercent } from '../../utils/formatters';

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

function mapClassData(classData) {
  return (Array.isArray(classData) ? classData : []).map((cls) => {
    const fields = (cls.fields ?? []).map((f) => ({ ...f, ok: f.ok ?? f.isCorrect }));
    const constructors = (cls.constructors ?? []).map((c) => ({ ...c, ok: c.ok ?? c.isCorrect }));
    const methods = (cls.methods ?? []).map((m) => ({ ...m, ok: m.ok ?? m.isCorrect }));
    return {
      name: cls.name,
      type: cls.type || 'CLASS',
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
  const passCount = allItems.filter((item) => item.ok).length;
  const totalCount = allItems.length;
  const overallPct = totalCount ? Math.round((passCount / totalCount) * 100) : 0;

  return (
    <div>
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-sm font-semibold text-gray-700 dark:text-gray-200">Class Score</p>
        </div>
        {totalCount > 0 && (
          <ScorePill ok={passCount} total={totalCount} pct={overallPct} />
        )}
        {overallScore != null && totalCount === 0 && (
          <span className="text-sm font-semibold text-gray-900 dark:text-white">{formatPercent(overallScore)}</span>
        )}
      </div>

      {classes.length === 0 ? (
        <div className="rounded-xl border border-dashed border-gray-200 p-8 text-center text-sm text-gray-500 dark:border-gray-700 dark:text-gray-400">
          No class detail data is available.
        </div>
      ) : (
        <div className="space-y-3">
          {classes.map((cls) => {
            const items = [...cls.fields, ...cls.constructors, ...cls.methods];
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
                  <div>
                    <span className="text-[10px] uppercase tracking-wider text-gray-400 dark:text-gray-500">{cls.type}</span>
                    <p className="mt-1 font-mono text-sm font-bold text-gray-900 dark:text-white">{cls.name}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <ScorePill ok={clsPass} total={items.length || 1} pct={clsPct} />
                    {isOpen ? <ChevronUp className="h-4 w-4 text-gray-400" /> : <ChevronDown className="h-4 w-4 text-gray-400" />}
                  </div>
                </button>

                {isOpen && (
                  <div className="divide-y divide-gray-100 border-t border-gray-200 dark:divide-gray-800 dark:border-gray-700">
                    {cls.fields.length > 0 && (
                      <div className="px-4 py-3">
                        <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-blue-500">Fields</p>
                        <div className="space-y-2">
                          {cls.fields.map((field, index) => (
                            <div
                              key={`${field.name}-${index}`}
                              className={`flex items-center justify-between rounded-lg px-3 py-2 ${field.ok ? 'bg-gray-50 dark:bg-[#0d1117]/40' : 'border border-red-200 bg-red-50 dark:border-red-800/40 dark:bg-red-900/10'}`}
                            >
                              <div>
                                <p className="text-xs font-mono font-semibold text-blue-600 dark:text-blue-400">{field.name}: {field.dataType}</p>
                                <p className="mt-0.5 text-[10px] text-gray-400">{field.scope || '—'}</p>
                              </div>
                              <Tick ok={field.ok} />
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {cls.constructors.length > 0 && (
                      <div className="px-4 py-3">
                        <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-orange-500">Constructors</p>
                        <div className="space-y-2">
                          {cls.constructors.map((ctor, index) => (
                            <div
                              key={`${ctor.name}-${index}`}
                              className={`flex items-center justify-between rounded-lg px-3 py-2 ${ctor.ok ? 'bg-gray-50 dark:bg-[#0d1117]/40' : 'border border-red-200 bg-red-50 dark:border-red-800/40 dark:bg-red-900/10'}`}
                            >
                              <div>
                                <p className="text-xs font-mono font-semibold text-orange-500 dark:text-orange-400">{ctor.name}({ctor.params || ''})</p>
                                <p className="mt-0.5 text-[10px] text-gray-400">{ctor.scope || '—'}</p>
                              </div>
                              <Tick ok={ctor.ok} />
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {cls.methods.length > 0 && (
                      <div className="px-4 py-3">
                        <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-green-500">Methods</p>
                        <div className="space-y-2">
                          {cls.methods.map((method, index) => (
                            <div
                              key={`${method.name}-${index}`}
                              className={`flex items-center justify-between rounded-lg px-3 py-2 ${method.ok ? 'bg-gray-50 dark:bg-[#0d1117]/40' : 'border border-red-200 bg-red-50 dark:border-red-800/40 dark:bg-red-900/10'}`}
                            >
                              <div>
                                <p className="text-xs font-mono font-semibold text-green-600 dark:text-green-400">{method.name}(): {method.returnType}</p>
                                <p className="mt-0.5 text-[10px] text-gray-400">{method.scope || '—'}</p>
                              </div>
                              <Tick ok={method.ok} />
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
