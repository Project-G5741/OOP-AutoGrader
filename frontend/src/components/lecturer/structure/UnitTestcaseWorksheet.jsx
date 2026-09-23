import React from 'react';
import TestcaseAssertionFields from './TestcaseAssertionFields';
import TestcaseParamFields from './TestcaseParamFields';
import {
  allowedAssertionKinds,
  applyUnitTarget,
  defaultExpectedValue,
  emptyAssertion,
  emptyInvocation,
  FIELD_CLASS,
  isObjectReturnStep,
  selectedUnitTarget,
  stepParameters,
  unitMemberBlockedReason,
  unitTargetKey,
  unitTargets,
} from './testcaseAuthoring';

export default function UnitTestcaseWorksheet({ tc, catalog, onUpdate }) {
  const step = tc.invocations?.[0] || tc.invocation || emptyInvocation();
  const target = selectedUnitTarget(step, catalog);
  const blocked = unitMemberBlockedReason(target, catalog);
  const parameters = stepParameters(step, catalog);
  const objectReturn = isObjectReturnStep(step, catalog);

  const patchStep = (nextStep, extra = {}) => {
    onUpdate({
      invocations: [nextStep],
      invocation: nextStep,
      ...extra,
    });
  };

  const patchAssertions = (assertions) => onUpdate({ assertions });

  return (
    <div className="min-w-0 space-y-3">
      <p className="text-xs text-foreground-muted">
        One constructor or method, then assertions. The hidden no-arg receiver is not a step.
      </p>

      <label className="block text-xs text-foreground-muted">
        Member
        <select
          className={FIELD_CLASS}
          value={unitTargetKey(step)}
          onChange={(e) => {
            const nextStep = applyUnitTarget(step, e.target.value, catalog);
            const kinds = allowedAssertionKinds(nextStep, catalog);
            const nextObject = isObjectReturnStep(nextStep, catalog);
            patchStep(nextStep, {
              assertions: (tc.assertions || []).map((assertion) => {
                if (kinds.includes(assertion.assertionKind)) return assertion;
                return {
                  ...assertion,
                  assertionKind: kinds[0],
                  fieldId: kinds[0] === 'FIELD_STATE' ? assertion.fieldId : null,
                  expectedValue: defaultExpectedValue(kinds[0], kinds[0] === 'RETURN_VALUE' && nextObject),
                };
              }),
            });
          }}
        >
          <option value="">Select constructor or method</option>
          {unitTargets(catalog).map((item) => {
            const key = `${item.kind}:${item.id}`;
            const reason = unitMemberBlockedReason(item, catalog);
            return (
              <option key={key} value={key} disabled={Boolean(reason)}>
                {item.label}{reason ? ` — ${reason}` : ''}
              </option>
            );
          })}
        </select>
      </label>

      {blocked && (
        <p className="rounded border border-warning/40 bg-warning-bg px-3 py-2 text-xs text-warning-text">
          {blocked}
        </p>
      )}

      {target?.kind === 'METHOD' && !target.isStatic && !blocked && (
        <p className="text-xs text-foreground-muted">
          Dry-run constructs a hidden no-arg receiver. That construct is not shown as a step.
        </p>
      )}

      <div>
        <div className="mb-1 text-xs font-semibold text-foreground-secondary">Arguments</div>
        <TestcaseParamFields
          parameters={parameters}
          paramsJson={step?.params}
          catalog={catalog}
          allowInstanceRefs={false}
          onChange={(params) => patchStep({ ...step, params })}
        />
      </div>

      <div className="space-y-2">
        <div className="text-xs font-semibold text-foreground-secondary">Assertions</div>
        {(tc.assertions || []).map((assertion, idx) => (
          <TestcaseAssertionFields
            key={assertion.id || idx}
            assertion={{ ...assertion, invocationId: step?.id }}
            step={step}
            catalog={catalog}
            namedInstances={[]}
            allowEquals={false}
            allowFieldInstanceRef={false}
            canRemove={(tc.assertions || []).length > 1}
            onChange={(next) => {
              const assertions = [...(tc.assertions || [])];
              assertions[idx] = { ...next, invocationId: step?.id };
              patchAssertions(assertions);
            }}
            onRemove={() => patchAssertions((tc.assertions || []).filter((_, i) => i !== idx))}
          />
        ))}
        <button
          type="button"
          className="text-xs text-primary"
          onClick={() => {
            const kinds = allowedAssertionKinds(step, catalog);
            const kind = kinds.includes('FIELD_STATE') ? 'FIELD_STATE' : kinds[0];
            patchAssertions([
              ...(tc.assertions || []),
              {
                ...emptyAssertion(step?.id, kind),
                expectedValue: defaultExpectedValue(kind, kind === 'RETURN_VALUE' && objectReturn),
                orderIndex: (tc.assertions || []).length,
              },
            ]);
          }}
        >
          + Add assertion
        </button>
      </div>
    </div>
  );
}
