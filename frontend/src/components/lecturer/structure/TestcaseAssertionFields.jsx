import React from 'react';
import { Trash2 } from 'lucide-react';
import {
  allowedAssertionKinds,
  comparisonModeLabel,
  comparisonModesForAssertion,
  normalizeComparisonMode,
  defaultExpectedValue,
  defaultFieldStateExpected,
  displayScalar,
  FIELD_CLASS_COMPACT,
  fieldsForStepAssertion,
  isRubricClassType,
  parseFieldStateExpected,
  parseScalarInput,
  readInstanceExpected,
  writeFieldStateScalar,
  isNumericAssertion,
  isObjectReturnStep,
  readObjectCheck,
  writeObjectCheck,
} from './testcaseAuthoring';

function ExceptionField({ value, onChange }) {
  let text = value || '';
  try {
    const parsed = JSON.parse(value || '""');
    if (typeof parsed === 'string') text = parsed;
  } catch {
    text = String(value || '').replace(/^"|"$/g, '');
  }
  return (
    <input
      className={`${FIELD_CLASS_COMPACT} font-mono sm:col-span-2`}
      value={text}
      onChange={(e) => onChange(JSON.stringify(e.target.value.trim()))}
      placeholder="Exception type"
    />
  );
}

function EqualsExpected({ assertion, namedInstances, onChange }) {
  const current = readObjectCheck(assertion.expectedValue);
  const instance = current?.instance || '';

  return (
    <select
      className={`${FIELD_CLASS_COMPACT} sm:col-span-2`}
      value={instance}
      onChange={(e) => onChange({
        ...assertion,
        expectedValue: writeObjectCheck('EQUALS', { instance: e.target.value }),
      })}
    >
      <option value="">Named instance</option>
      {namedInstances.map((item) => (
        <option key={item.name} value={item.name}>{item.name}</option>
      ))}
    </select>
  );
}

function FieldExpected({
  assertion,
  catalog,
  namedInstances,
  allowFieldInstanceRef,
  onChange,
}) {
  const field = catalog.fieldsById.get(assertion.fieldId);
  const objectField = field && isRubricClassType(field.dataType, catalog);
  const showNamedInstancePicker = allowFieldInstanceRef && objectField;
  const instanceName = readInstanceExpected(assertion.expectedValue) || '';

  if (showNamedInstancePicker) {
    return (
      <select
        className={FIELD_CLASS_COMPACT}
        value={instanceName}
        onChange={(e) => onChange({
          ...assertion,
          expectedValue: JSON.stringify({ $instance: e.target.value }),
        })}
      >
        <option value="">Named instance</option>
        {namedInstances.map((item) => (
          <option key={item.name} value={item.name}>{item.name}</option>
        ))}
      </select>
    );
  }

  const instanceRef = readInstanceExpected(assertion.expectedValue);
  const scalar = instanceRef != null
    ? JSON.parse(defaultFieldStateExpected(field))
    : parseFieldStateExpected(assertion.expectedValue);

  return (
    <input
      className={`${FIELD_CLASS_COMPACT} font-mono`}
      value={displayScalar(scalar)}
      onChange={(e) => onChange({
        ...assertion,
        expectedValue: writeFieldStateScalar(parseScalarInput(e.target.value)),
      })}
      placeholder="value"
    />
  );
}

export default function TestcaseAssertionFields({
  assertion,
  step,
  catalog,
  testcaseType = 'UNIT',
  namedInstances = [],
  allowEquals = false,
  allowFieldInstanceRef = false,
  onChange,
  onRemove,
  canRemove = false,
}) {
  const kinds = allowedAssertionKinds(step, catalog, testcaseType);
  const objectReturn = isObjectReturnStep(step, catalog);
  const fieldOptions = fieldsForStepAssertion(step, catalog);
  const showEqualsReturn = assertion.assertionKind === 'RETURN_VALUE'
    && allowEquals
    && objectReturn;
  const showScalarReturn = assertion.assertionKind === 'RETURN_VALUE' && !showEqualsReturn;
  const showComparison = assertion.assertionKind !== 'EXCEPTION' && !showEqualsReturn;
  const numericComparison = isNumericAssertion(assertion, step, catalog);
  const comparisonModes = comparisonModesForAssertion(assertion, step, catalog);
  const storedComparisonMode = normalizeComparisonMode(assertion.comparisonMode);
  const comparisonMode = comparisonModes.includes(storedComparisonMode)
    ? storedComparisonMode
    : (numericComparison ? 'VALUE_ONLY' : 'EXACT');

  return (
    <div className="grid gap-2 rounded border border-border p-2 sm:grid-cols-3">
      <select
        className={FIELD_CLASS_COMPACT}
        value={kinds.includes(assertion.assertionKind) ? assertion.assertionKind : kinds[0]}
        onChange={(e) => {
          const nextKind = e.target.value;
          const objectEquals = nextKind === 'RETURN_VALUE' && allowEquals && objectReturn;
          onChange({
            ...assertion,
            assertionKind: nextKind,
            fieldId: nextKind === 'FIELD_STATE' ? (assertion.fieldId || null) : null,
            expectedValue: defaultExpectedValue(nextKind, objectEquals),
          });
        }}
      >
        {kinds.map((kind) => (
          <option key={kind} value={kind}>{kind}</option>
        ))}
      </select>

      {assertion.assertionKind === 'FIELD_STATE' && (
        <select
          className={FIELD_CLASS_COMPACT}
          value={assertion.fieldId || ''}
          onChange={(e) => {
            const fieldId = e.target.value || null;
            let expectedValue = assertion.expectedValue;
            if (allowFieldInstanceRef && fieldId) {
              const field = catalog.fieldsById.get(fieldId);
              if (field && isRubricClassType(field.dataType, catalog)) {
                if (readInstanceExpected(expectedValue) == null) {
                  expectedValue = JSON.stringify({ $instance: namedInstances[0]?.name || '' });
                }
              } else if (field && readInstanceExpected(expectedValue) != null) {
                expectedValue = defaultFieldStateExpected(field);
              }
            }
            onChange({ ...assertion, fieldId, expectedValue });
          }}
        >
          <option value="">Field</option>
          {fieldOptions.map((opt) => (
            <option key={opt.id} value={opt.id}>{opt.label}</option>
          ))}
        </select>
      )}

      {showEqualsReturn && (
        <EqualsExpected
          assertion={assertion}
          namedInstances={namedInstances}
          onChange={onChange}
        />
      )}

      {assertion.assertionKind === 'EXCEPTION' && (
        <ExceptionField
          value={assertion.expectedValue}
          onChange={(expectedValue) => onChange({ ...assertion, expectedValue })}
        />
      )}

      {assertion.assertionKind === 'FIELD_STATE' && (
        <FieldExpected
          assertion={assertion}
          catalog={catalog}
          namedInstances={namedInstances}
          allowFieldInstanceRef={allowFieldInstanceRef}
          onChange={onChange}
        />
      )}

      {assertion.assertionKind === 'STDOUT' && (
        <input
          className={`${FIELD_CLASS_COMPACT} font-mono sm:col-span-2`}
          value={assertion.expectedValue || ''}
          onChange={(e) => onChange({ ...assertion, expectedValue: e.target.value })}
          placeholder="Expected stdout JSON"
        />
      )}

      {showScalarReturn && (
        <input
          className={`${FIELD_CLASS_COMPACT} font-mono sm:col-span-2`}
          value={assertion.expectedValue || ''}
          onChange={(e) => onChange({ ...assertion, expectedValue: e.target.value })}
          placeholder="Expected value JSON"
        />
      )}

      {showComparison && (
        <select
          className={FIELD_CLASS_COMPACT}
          value={comparisonMode}
          onChange={(e) => onChange({ ...assertion, comparisonMode: e.target.value })}
        >
          {comparisonModes.map((mode) => (
            <option key={mode} value={mode}>
              {comparisonModeLabel(mode)}
            </option>
          ))}
        </select>
      )}

      {canRemove && (
        <button
          type="button"
          className="justify-self-start text-xs text-error hover:underline"
          onClick={onRemove}
        >
          Remove
        </button>
      )}
    </div>
  );
}
