import React, { useState } from 'react';
import { CheckCircle2, ChevronDown, ChevronUp, Circle, Loader2, XCircle } from 'lucide-react';

export function dryRunSummaryText(result) {
  if (!result) return '';
  if (result.feedback) return result.feedback;
  const assertions = result.assertions ?? [];
  const failed = assertions.filter((a) => a.result !== 'PASS').length;
  if (failed > 0) return `${failed} assertion${failed === 1 ? '' : 's'} failed`;
  if (assertions.length > 0) return 'All assertions passed';
  return result.result === 'PASS' ? 'Passed' : 'Failed';
}

export function DryRunStatusIcon({ result, running }) {
  if (running) {
    return <Loader2 className="h-3.5 w-3.5 shrink-0 animate-spin text-primary" aria-hidden />;
  }
  if (!result) {
    return <Circle className="h-3.5 w-3.5 shrink-0 text-foreground-disabled" aria-hidden />;
  }
  if (result.result === 'PASS') {
    return <CheckCircle2 className="h-3.5 w-3.5 shrink-0 text-success" aria-hidden />;
  }
  return <XCircle className="h-3.5 w-3.5 shrink-0 text-error" aria-hidden />;
}

export function DryRunResultCard({ result }) {
  if (!result) return null;
  const passed = result.result === 'PASS';
  const [expanded, setExpanded] = useState(!passed);
  const assertions = result.assertions ?? [];
  const hasAssertionRows = assertions.length > 0
    && assertions.some((a) => a.expected_output ?? a.expectedOutput);

  const headerBarClass = passed
    ? 'bg-[var(--success-panel)] text-[var(--success-panel-text)]'
    : 'bg-[var(--error-bg)] text-[var(--error-text)]';
  const bodyCardClass = 'min-w-0 overflow-hidden rounded-lg border border-border-subtle bg-surface-secondary';
  const sectionDividerClass = 'border-t border-border-subtle';
  const sectionLabelClass = 'mb-1 text-[10px] font-semibold uppercase tracking-wide text-foreground-muted';
  const codeBlockClass = 'min-w-0 whitespace-pre-wrap break-words rounded-md bg-surface px-2 py-1.5 font-mono text-xs text-foreground-secondary [overflow-wrap:anywhere]';
  const summaryClass = 'min-w-0 flex-1 break-words text-xs [overflow-wrap:anywhere]';
  const summary = dryRunSummaryText(result);

  if (!expanded) {
    return (
      <div className={`animate-panel-in min-w-0 overflow-hidden rounded-lg ${headerBarClass}`}>
        <div className="flex items-start justify-between gap-2 px-3 py-2 text-sm">
          <div className="flex min-w-0 flex-1 items-start gap-2">
            <span className="shrink-0 text-xs font-semibold">{result.result}</span>
            <span className={summaryClass}>{summary}</span>
          </div>
          <button
            type="button"
            onClick={() => setExpanded(true)}
            className="inline-flex shrink-0 items-center gap-1 text-xs opacity-80 hover:opacity-100"
          >
            Details <ChevronDown className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={`animate-panel-in text-sm ${bodyCardClass}`}>
      <div className={`flex items-start justify-between gap-3 px-3 py-2 ${headerBarClass}`}>
        <div className="flex min-w-0 flex-1 items-start gap-2">
          <span className="shrink-0 text-xs font-semibold">{result.result}</span>
          <span className={summaryClass}>{summary}</span>
        </div>
        <button
          type="button"
          onClick={() => setExpanded(false)}
          className="inline-flex shrink-0 items-center gap-1 text-xs opacity-80 hover:opacity-100"
        >
          Hide <ChevronUp className="h-3.5 w-3.5" />
        </button>
      </div>

      <div className="px-3 py-3">
        {result.input != null && (
          <div className="pb-3">
            <div className={sectionLabelClass}>Input</div>
            <pre className={codeBlockClass}>{result.input}</pre>
          </div>
        )}

        {hasAssertionRows ? (
          <div className={`scrollbar-themed max-h-36 space-y-0 overflow-y-auto pr-1 ${result.input != null ? sectionDividerClass : ''}`}>
            {assertions.map((assertion, index) => {
              const aPassed = assertion.result === 'PASS';
              const expected = assertion.expected_output ?? assertion.expectedOutput ?? '—';
              const actual = assertion.actual_output ?? assertion.actualOutput ?? '—';
              const fieldLabel = assertion.kind === 'FIELD_STATE' && expected.includes('=')
                ? expected.split('=')[0].trim()
                : null;

              return (
                <div
                  key={`${assertion.kind}-${assertion.order_index ?? assertion.orderIndex ?? index}`}
                  className={`py-3 ${index > 0 ? sectionDividerClass : ''}`}
                >
                  <div className="mb-1.5 flex items-center justify-between gap-2">
                    <span className="text-[10px] font-semibold uppercase tracking-wide text-foreground-secondary">
                      {fieldLabel ?? assertion.kind}
                    </span>
                    <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${
                      aPassed ? 'bg-success-bg text-success-text' : 'bg-error-bg text-error-text'
                    }`}
                    >
                      {assertion.result}
                    </span>
                  </div>
                  <div className="grid min-w-0 gap-2 sm:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
                    <div className="min-w-0">
                      <div className="mb-0.5 text-[10px] font-semibold uppercase tracking-wide text-foreground-muted">Expected</div>
                      <pre className={codeBlockClass}>{expected}</pre>
                    </div>
                    <div className="min-w-0">
                      <div className="mb-0.5 text-[10px] font-semibold uppercase tracking-wide text-foreground-muted">Actual</div>
                      <pre className={`min-w-0 whitespace-pre-wrap break-words rounded-md px-2 py-1.5 font-mono text-xs [overflow-wrap:anywhere] ${
                        aPassed
                          ? 'bg-surface text-foreground-secondary'
                          : 'bg-error-bg text-error-text'
                      }`}
                      >
                        {actual}
                      </pre>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className={`grid min-w-0 gap-3 sm:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] ${result.input != null ? `${sectionDividerClass} pt-3` : ''}`}>
            {result.expected_output != null && (
              <div className="min-w-0">
                <div className={sectionLabelClass}>Expected</div>
                <pre className={codeBlockClass}>{result.expected_output}</pre>
              </div>
            )}
            {result.actual_output != null && (
              <div className="min-w-0">
                <div className={sectionLabelClass}>Actual</div>
                <pre className={codeBlockClass}>{result.actual_output}</pre>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
