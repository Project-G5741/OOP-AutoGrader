import { useState } from 'react';
import { Check, ChevronDown, ChevronUp, X } from 'lucide-react';
import { ScoreSectionHeader, hasScoreToShow } from '../ui/ScorePill';
import { formatStudentTestcaseMessage } from '../../utils/formatters';

function Tick({ ok, error }) {
  if (error) {
    return (
      <span className="inline-flex h-5 w-5 items-center justify-center rounded-full bg-error-bg text-error-text">
        <X className="h-3.5 w-3.5" strokeWidth={3} />
      </span>
    );
  }
  return ok ? (
    <span className="inline-flex h-5 w-5 items-center justify-center rounded-full bg-success-bg text-success-text">
      <Check className="h-3.5 w-3.5" strokeWidth={3} />
    </span>
  ) : (
    <span className="inline-flex h-5 w-5 items-center justify-center rounded-full bg-error-bg text-error-text">
      <X className="h-3.5 w-3.5" strokeWidth={3} />
    </span>
  );
}

function formatIoDisplay(value) {
  if (value == null || value === '') return '—';
  return formatStudentTestcaseMessage(String(value));
}

function statusLabel(tc) {
  if (tc.result === 'ERROR') return 'ERROR';
  if (tc.result === 'SKIPPED') return 'SKIPPED';
  return tc.passed ? 'PASS' : 'FAIL';
}

function statusClass(tc) {
  if (tc.result === 'ERROR') return 'text-error-text';
  if (tc.result === 'SKIPPED') return 'text-foreground-muted';
  return tc.passed ? 'text-success-text' : 'text-error-text';
}

/**
 * Map upload/lab_result testcase DTOs into expandable OT cards (lecturer sees hidden names).
 */
export function mapOperationalTestcases(testcases = []) {
  return (testcases || []).map((testcase, index) => ({
    id: `tc-${index}-${testcase.testcase_name || testcase.name || testcase.id || index}`,
    name: testcase.testcase_name || testcase.name || `Testcase ${index + 1}`,
    isHidden: testcase.is_hidden ?? testcase.isHidden ?? false,
    passed: testcase.result === 'PASS',
    result: testcase.result,
    input: testcase.input ?? '',
    expectedOutput: testcase.expected_output ?? testcase.expectedOutput ?? '',
    actualOutput: testcase.actual_output ?? testcase.actualOutput ?? '',
    assertions: testcase.assertions ?? [],
    feedback: testcase.feedback,
  }));
}

function pillarScore(bundle, key) {
  const raw = bundle?.scores?.[key];
  if (raw == null || Number.isNaN(Number(raw))) return null;
  const pct = Math.floor(Number(raw));
  return { ok: pct, total: 100, pct };
}

/**
 * Lecturer OT breakdown from an ephemeral or fetched challenge bundle.
 */
export default function OperationalTestcaseBreakdown({ bundle }) {
  const [expandedId, setExpandedId] = useState(null);
  const testcases = mapOperationalTestcases(bundle?.testcases);
  const score = pillarScore(bundle, 'testcase');
  const examples = testcases.filter((tc) => !tc.isHidden);
  const others = testcases.filter((tc) => tc.isHidden);

  const renderList = (rows, title) => {
    if (rows.length === 0) return null;
    return (
      <div>
        <p className="mb-3 text-xs font-bold uppercase tracking-wider text-foreground-muted">{title}</p>
        <div className="space-y-2">
          {rows.map((tc) => (
            <div key={tc.id} className="overflow-hidden rounded-xl border border-border">
              <button
                type="button"
                onClick={() => setExpandedId(expandedId === tc.id ? null : tc.id)}
                className="flex w-full items-center justify-between px-4 py-3 text-left transition-colors hover:bg-surface-secondary"
              >
                <div className="flex min-w-0 items-center gap-3">
                  <Tick ok={tc.passed} error={tc.result === 'ERROR'} />
                  <span className="truncate text-sm font-medium text-foreground-secondary">{tc.name}</span>
                </div>
                <div className="flex shrink-0 items-center gap-2">
                  <span className={`text-xs font-semibold ${statusClass(tc)}`}>{statusLabel(tc)}</span>
                  {expandedId === tc.id ? (
                    <ChevronUp className="h-4 w-4 text-foreground-muted" />
                  ) : (
                    <ChevronDown className="h-4 w-4 text-foreground-muted" />
                  )}
                </div>
              </button>
              {expandedId === tc.id && (
                <div className="border-t border-border">
                  <div className="grid grid-cols-1 divide-y divide-border md:grid-cols-3 md:divide-x md:divide-y-0">
                    <div className="p-4">
                      <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-foreground-muted">Input</p>
                      <pre className="whitespace-pre-wrap rounded-lg bg-surface-secondary p-3 text-xs font-mono text-foreground-secondary">
                        {tc.input || '—'}
                      </pre>
                    </div>
                    <div className="p-4">
                      <p className="mb-2 text-[10px] font-bold uppercase tracking-wider text-success">Expected Output</p>
                      <pre className="whitespace-pre-wrap rounded-lg bg-success-bg p-3 text-xs font-mono text-success-text">
                        {tc.expectedOutput || '—'}
                      </pre>
                    </div>
                    <div className="p-4">
                      <div className="mb-2 flex items-center justify-between">
                        <p className="text-[10px] font-bold uppercase tracking-wider text-foreground-muted">Actual Output</p>
                        <Tick ok={tc.passed} error={tc.result === 'ERROR'} />
                      </div>
                      <pre
                        className={`whitespace-pre-wrap rounded-lg p-3 text-xs font-mono ${
                          tc.result === 'ERROR' || !tc.passed
                            ? 'bg-error-bg text-error-text'
                            : 'bg-success-bg text-success-text'
                        }`}
                      >
                        {tc.result === 'ERROR'
                          ? (tc.feedback || formatIoDisplay(tc.actualOutput))
                          : formatIoDisplay(tc.actualOutput)}
                      </pre>
                    </div>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    );
  };

  return (
    <div className="space-y-4">
      <ScoreSectionHeader
        title="I/O Score"
        score={score}
        showPill={hasScoreToShow(score, bundle, 'testcase')}
      />
      {testcases.length === 0 ? (
        <p className="text-sm text-foreground-secondary">No operational testcase results for this challenge.</p>
      ) : (
        <div className="space-y-6">
          {renderList(examples, 'Example Testcases')}
          {renderList(others, 'Other Testcases')}
        </div>
      )}
    </div>
  );
}
