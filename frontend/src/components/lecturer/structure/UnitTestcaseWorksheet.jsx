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
  fieldsForStepAssertion,
  selectedUnitTarget,
  stepParameters,
  unitMemberBlockedReason,
  unitTargetKey,
  unitSelectableTargets,
} from './testcaseAuthoring';

export default function UnitTestcaseWorksheet({ tc, catalog, onUpdate }) {
  const step = tc.invocations?.[0] || tc.invocation || emptyInvocation();
  const target = selectedUnitTarget(step, catalog);
  const blocked = unitMemberBlockedReason(target, catalog);
  const parameters = stepParameters(step, catalog);

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
        One constructor or method, then assertions. Instance methods use a hidden receiver (not shown as a step).
      </p>

      <label className="block text-xs text-foreground-muted">
        Member
        <select
          className={FIELD_CLASS}
          value={unitTargetKey(step)}
          onChange={(e) => {
            const nextStep = applyUnitTarget(step, e.target.value, catalog);
            const kinds = allowedAssertionKinds(nextStep, catalog, 'UNIT');
            const allowedFields = fieldsForStepAssertion(nextStep, catalog);
            const allowedFieldIds = new Set(allowedFields.map((f) => f.id));
            patchStep(nextStep, {
              assertions: (tc.assertions || []).map((assertion) => {
                const kind = kinds.includes(assertion.assertionKind) ? assertion.assertionKind : kinds[0];
                const fieldId = kind === 'FIELD_STATE' && allowedFieldIds.has(assertion.fieldId)
                  ? assertion.fieldId
                  : (kind === 'FIELD_STATE' ? null : null);
                if (kind === assertion.assertionKind && fieldId === assertion.fieldId) return assertion;
                return {
                  ...assertion,
                  assertionKind: kind,
                  fieldId,
                  expectedValue: defaultExpectedValue(kind, false),
                };
              }),
            });
          }}
        >
          <option value="">Select constructor or method</option>
          {unitSelectableTargets(catalog).map((item) => {
            const key = `${item.kind}:${item.id}`;
            return (
              <option key={key} value={key}>
                {item.label}
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
            testcaseType="UNIT"
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
            const kinds = allowedAssertionKinds(step, catalog, 'UNIT');
            const kind = kinds.includes('FIELD_STATE') ? 'FIELD_STATE' : kinds[0];
            patchAssertions([
              ...(tc.assertions || []),
              {
                ...emptyAssertion(step?.id, kind),
                expectedValue: defaultExpectedValue(kind, false),
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
