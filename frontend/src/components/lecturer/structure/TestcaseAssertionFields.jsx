import React from 'react';
import { Trash2 } from 'lucide-react';
import Combobox from '../../ui/Combobox';
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
  isLocalDateType,
  isOrdinaryRubricClassType,
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
    <div className="sm:col-span-2">
      <Combobox
        aria-label="Equals named instance"
        placeholder="Named instance"
        emptyLabel="Named instance"
        allowEmpty
        size="sm"
        value={instance}
        onChange={(val) => onChange({
          ...assertion,
          expectedValue: writeObjectCheck('EQUALS', { instance: val }),
        })}
        options={namedInstances.map((item) => ({ value: item.name, label: item.name }))}
        className="w-full min-w-0"
      />
    </div>
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
  const objectField = field && isOrdinaryRubricClassType(field.dataType, catalog);
  const showNamedInstancePicker = allowFieldInstanceRef && objectField;
  const instanceName = readInstanceExpected(assertion.expectedValue) || '';

  if (showNamedInstancePicker) {
    return (
      <Combobox
        aria-label="Field named instance"
        placeholder="Named instance"
        emptyLabel="Named instance"
        allowEmpty
        size="sm"
        value={instanceName}
        onChange={(val) => onChange({
          ...assertion,
          expectedValue: JSON.stringify({ $instance: val }),
        })}
        options={namedInstances.map((item) => ({ value: item.name, label: item.name }))}
        className="w-full min-w-0"
      />
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
      placeholder={isLocalDateType(field?.dataType) ? 'yyyy-MM-dd' : 'value'}
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
      <Combobox
        aria-label="Assertion kind"
        value={kinds.includes(assertion.assertionKind) ? assertion.assertionKind : kinds[0]}
        allowEmpty={false}
        size="sm"
        onChange={(nextKind) => {
          const objectEquals = nextKind === 'RETURN_VALUE' && allowEquals && objectReturn;
          onChange({
            ...assertion,
            assertionKind: nextKind,
            fieldId: nextKind === 'FIELD_STATE' ? (assertion.fieldId || null) : null,
            expectedValue: defaultExpectedValue(nextKind, objectEquals),
          });
        }}
        options={kinds.map((kind) => ({ value: kind, label: kind }))}
        className="w-full min-w-0"
      />

      {assertion.assertionKind === 'FIELD_STATE' && (
        <Combobox
          aria-label="Field"
          placeholder="Field"
          emptyLabel="Field"
          allowEmpty
          size="sm"
          value={assertion.fieldId || ''}
          onChange={(val) => {
            const fieldId = val || null;
            let expectedValue = assertion.expectedValue;
            if (allowFieldInstanceRef && fieldId) {
              const field = catalog.fieldsById.get(fieldId);
              if (field && isOrdinaryRubricClassType(field.dataType, catalog)) {
                if (readInstanceExpected(expectedValue) == null) {
                  expectedValue = JSON.stringify({ $instance: namedInstances[0]?.name || '' });
                }
              } else if (field && readInstanceExpected(expectedValue) != null) {
                expectedValue = defaultFieldStateExpected(field);
              }
            }
            onChange({ ...assertion, fieldId, expectedValue });
          }}
          options={fieldOptions.map((opt) => ({ value: opt.id, label: opt.label }))}
          className="w-full min-w-0"
        />
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
        <Combobox
          aria-label="Comparison mode"
          value={comparisonMode}
          allowEmpty={false}
          size="sm"
          onChange={(mode) => onChange({ ...assertion, comparisonMode: mode })}
          options={comparisonModes.map((mode) => ({
            value: mode,
            label: comparisonModeLabel(mode),
          }))}
          className="w-full min-w-0"
        />
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
