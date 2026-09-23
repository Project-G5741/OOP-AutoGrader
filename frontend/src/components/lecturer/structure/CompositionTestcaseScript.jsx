import React from 'react';
import TestcaseAssertionFields from './TestcaseAssertionFields';
import TestcaseParamFields from './TestcaseParamFields';
import {
  allowedAssertionKinds,
  compositionNameRole,
  defaultExpectedValue,
  emptyAssertion,
  emptyInvocation,
  FIELD_CLASS,
  invocationForKindChange,
  MAX_STEPS,
  namedInstancesBefore,
  parseParamsArray,
  serializeParamsArray,
  stepParameters,
} from './testcaseAuthoring';

function CompositionStepEditor({
  step,
  index,
  steps,
  catalog,
  assertions,
  onChange,
  onRemove,
  onAssertionsChange,
  canRemove,
}) {
  const namedBefore = namedInstancesBefore(steps, catalog, index);
  const parameters = stepParameters(step, catalog);
  const nameRole = compositionNameRole(step, catalog);
  const method = catalog.methodsById.get(step.methodId);
  const receiverOptions = method && !method.isStatic
    ? namedBefore.filter((item) => item.className === method.className)
    : [];
  const objectReturn = nameRole === 'product' && step.invocationKind === 'METHOD';

  return (
    <div className="space-y-3 rounded border border-border p-3">
      <div className="flex items-center justify-between gap-2">
        <span className="text-[10px] font-semibold uppercase tracking-wide text-foreground-secondary">
          Step {index + 1}
        </span>
        {canRemove && (
          <button type="button" className="text-xs text-error hover:underline" onClick={onRemove}>
            Remove
          </button>
        )}
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <label className="block text-xs text-foreground-muted">
          Invocation kind
          <select
            className={FIELD_CLASS}
            value={step.invocationKind}
            onChange={(e) => onChange(invocationForKindChange(step, e.target.value))}
          >
            <option value="CONSTRUCTOR">CONSTRUCTOR</option>
            <option value="METHOD">METHOD</option>
          </select>
        </label>

        {step.invocationKind === 'CONSTRUCTOR' ? (
          <label className="block text-xs text-foreground-muted">
            Constructor
            <select
              className={FIELD_CLASS}
              value={step.constructorId || ''}
              onChange={(e) => {
                const constructorId = e.target.value || null;
                const ctor = catalog.constructorsById.get(constructorId);
                onChange({
                  ...step,
                  constructorId,
                  params: serializeParamsArray(parseParamsArray('[]', ctor?.parameters?.length || 0)),
                });
              }}
            >
              <option value="">Select constructor</option>
              {catalog.constructors.map((opt) => (
                <option key={opt.id} value={opt.id}>{opt.label}</option>
              ))}
            </select>
          </label>
        ) : (
          <label className="block text-xs text-foreground-muted">
            Method
            <select
              className={FIELD_CLASS}
              value={step.methodId || ''}
              onChange={(e) => {
                const methodId = e.target.value || null;
                const nextMethod = catalog.methodsById.get(methodId);
                onChange({
                  ...step,
                  methodId,
                  params: serializeParamsArray(parseParamsArray('[]', nextMethod?.parameters?.length || 0)),
                  instanceName: '',
                });
              }}
            >
              <option value="">Select method</option>
              {catalog.methods.map((opt) => (
                <option key={opt.id} value={opt.id}>{opt.label}</option>
              ))}
            </select>
          </label>
        )}

        {nameRole === 'product' && (
          <label className="block text-xs text-foreground-muted sm:col-span-2">
            {objectReturn ? 'Product name' : 'Instance name'}
            <input
              className={`${FIELD_CLASS} font-mono`}
              value={step.instanceName || ''}
              onChange={(e) => onChange({ ...step, instanceName: e.target.value })}
              placeholder={objectReturn ? 'e.g. copy' : 'e.g. account'}
            />
          </label>
        )}

        {nameRole === 'receiver' && (
          <label className="block text-xs text-foreground-muted sm:col-span-2">
            Receiver
            <select
              className={FIELD_CLASS}
              value={step.instanceName || ''}
              onChange={(e) => onChange({ ...step, instanceName: e.target.value })}
            >
              <option value="">Named instance</option>
              {receiverOptions.map((item) => (
                <option key={item.name} value={item.name}>{item.name}</option>
              ))}
            </select>
            {receiverOptions.length === 0 && (
              <span className="mt-1 block text-xs text-warning-text">
                Construct this class in an earlier step first.
              </span>
            )}
          </label>
        )}

        <div className="sm:col-span-2">
          <div className="mb-1 text-xs text-foreground-muted">Arguments</div>
          <TestcaseParamFields
            parameters={parameters}
            paramsJson={step.params}
            catalog={catalog}
            namedInstances={namedBefore}
            allowInstanceRefs
            onChange={(params) => onChange({ ...step, params })}
          />
        </div>
      </div>

      <div className="space-y-2">
        <div className="text-xs font-semibold text-foreground-secondary">Assertions (optional on this step)</div>
        {assertions.map((assertion, idx) => (
          <TestcaseAssertionFields
            key={assertion.id || idx}
            assertion={assertion}
            step={step}
            catalog={catalog}
            namedInstances={namedBefore}
            allowEquals
            allowFieldInstanceRef
            canRemove
            onChange={(next) => {
              const nextAssertions = assertions.map((row, i) => (i === idx ? { ...next, invocationId: step.id } : row));
              onAssertionsChange(nextAssertions);
            }}
            onRemove={() => onAssertionsChange(assertions.filter((_, i) => i !== idx))}
          />
        ))}
        <button
          type="button"
          className="text-xs text-primary"
          onClick={() => {
            const kinds = allowedAssertionKinds(step, catalog);
            const kind = kinds[0];
            onAssertionsChange([
              ...assertions,
              {
                ...emptyAssertion(step.id, kind),
                expectedValue: defaultExpectedValue(kind, kind === 'RETURN_VALUE' && (step.invocationKind === 'CONSTRUCTOR' || objectReturn)),
                orderIndex: assertions.length,
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

export default function CompositionTestcaseScript({ tc, catalog, onUpdate }) {
  const steps = tc.invocations || [];
  const assertions = tc.assertions || [];

  const patchSteps = (nextSteps, nextAssertions = assertions) => {
    onUpdate({
      invocations: nextSteps,
      invocation: nextSteps[0] || null,
      assertions: nextAssertions,
    });
  };

  const replaceStepAssertions = (stepId, nextForStep) => {
    const others = assertions.filter((assertion) => assertion.invocationId !== stepId);
    patchSteps(steps, [
      ...others,
      ...nextForStep.map((assertion, idx) => ({ ...assertion, invocationId: stepId, orderIndex: idx })),
    ]);
  };

  return (
    <div className="min-w-0 space-y-3">
      <p className="text-xs text-foreground-muted">
        Ordered named-object script. Name constructor results and static object returns. Instance methods use a receiver name.
      </p>

      {assertions.length === 0 && (
        <p className="rounded border border-warning/40 bg-warning-bg px-3 py-2 text-xs text-warning-text">
          Add at least one assertion on the testcase before save.
        </p>
      )}

      <div className="space-y-2">
        <div className="text-xs font-semibold text-foreground-secondary">Steps</div>
        {steps.map((step, idx) => (
          <CompositionStepEditor
            key={step.id || idx}
            step={step}
            index={idx}
            steps={steps}
            catalog={catalog}
            assertions={assertions.filter((assertion) => assertion.invocationId === step.id)}
            onChange={(next) => patchSteps(steps.map((row, i) => (i === idx ? next : row)))}
            onRemove={() => {
              const nextSteps = steps.filter((_, i) => i !== idx);
              const remainingIds = new Set(nextSteps.map((row) => row.id));
              patchSteps(
                nextSteps,
                assertions.filter((assertion) => remainingIds.has(assertion.invocationId)),
              );
            }}
            onAssertionsChange={(nextForStep) => replaceStepAssertions(step.id, nextForStep)}
            canRemove={steps.length > 1}
          />
        ))}
        <button
          type="button"
          className="text-xs text-primary disabled:opacity-50"
          disabled={steps.length >= MAX_STEPS}
          onClick={() => patchSteps([...steps, emptyInvocation()])}
        >
          + Add step
        </button>
      </div>
    </div>
  );
}
