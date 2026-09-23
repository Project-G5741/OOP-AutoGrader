import React from 'react';
import { Trash2 } from 'lucide-react';
import {
  allowedAssertionKinds,
  COMPARISON_MODES,
  defaultExpectedValue,
  displayScalar,
  FIELD_CLASS_COMPACT,
  fieldsForClass,
  isObjectReturnStep,
  isRubricClassType,
  parseScalarInput,
  readInstanceExpected,
  readObjectCheck,
  resultClassName,
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

function ObjectCheckFields({
  assertion,
  step,
  catalog,
  namedInstances,
  allowEquals,
  onChange,
}) {
  const current = readObjectCheck(assertion.expectedValue) || { kind: 'TYPE', fields: {}, instance: '' };
  const className = resultClassName(step, catalog);
  const fieldOptions = fieldsForClass(catalog, className);
  const fieldEntries = Object.entries(current.fields || {});

  const patchCheck = (next) => {
    onChange({
      ...assertion,
      expectedValue: writeObjectCheck(next.kind, next),
    });
  };

  return (
    <>
      <select
        className={FIELD_CLASS_COMPACT}
        value={current.kind}
        onChange={(e) => patchCheck({ ...current, kind: e.target.value })}
      >
        <option value="TYPE">Type only</option>
        <option value="FIELDS">Field map</option>
        {allowEquals && <option value="EQUALS">equals()</option>}
      </select>
      {current.kind === 'FIELDS' && (
        <div className="space-y-2 sm:col-span-2">
          {fieldEntries.map(([name, fieldValue], index) => (
            <div key={`${name}-${index}`} className="grid gap-2 sm:grid-cols-2">
              <select
                className={FIELD_CLASS_COMPACT}
                value={name}
                onChange={(e) => {
                  const nextFields = { ...current.fields };
                  delete nextFields[name];
                  nextFields[e.target.value] = fieldValue;
                  patchCheck({ ...current, fields: nextFields });
                }}
              >
                {!fieldOptions.some((field) => field.name === name) && name && (
                  <option value={name}>{name}</option>
                )}
                {fieldOptions.map((field) => (
                  <option key={field.id} value={field.name}>{field.label}</option>
                ))}
              </select>
              <div className="flex gap-2">
                <input
                  className={`${FIELD_CLASS_COMPACT} font-mono`}
                  value={displayScalar(fieldValue)}
                  onChange={(e) => patchCheck({
                    ...current,
                    fields: { ...current.fields, [name]: parseScalarInput(e.target.value) },
                  })}
                  placeholder="literal"
                />
                <button
                  type="button"
                  className="text-foreground-muted hover:text-error"
                  onClick={() => {
                    const nextFields = { ...current.fields };
                    delete nextFields[name];
                    patchCheck({ ...current, fields: nextFields });
                  }}
                  aria-label="Remove field"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
          <button
            type="button"
            className="text-xs text-primary"
            onClick={() => {
              const nextName = fieldOptions.find((field) => !(field.name in (current.fields || {})))?.name
                || `field${fieldEntries.length + 1}`;
              patchCheck({ ...current, fields: { ...current.fields, [nextName]: 0 } });
            }}
          >
            + Add field
          </button>
        </div>
      )}
      {current.kind === 'EQUALS' && allowEquals && (
        <select
          className={`${FIELD_CLASS_COMPACT} sm:col-span-2`}
          value={current.instance}
          onChange={(e) => patchCheck({ ...current, instance: e.target.value })}
        >
          <option value="">Named instance</option>
          {namedInstances.map((item) => (
            <option key={item.name} value={item.name}>{item.name}</option>
          ))}
        </select>
      )}
    </>
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
  const instanceName = readInstanceExpected(assertion.expectedValue);
  const useInstance = allowFieldInstanceRef && objectField && instanceName != null;

  return (
    <>
      {allowFieldInstanceRef && objectField && (
        <select
          className={FIELD_CLASS_COMPACT}
          value={useInstance ? 'instance' : 'literal'}
          onChange={(e) => {
            if (e.target.value === 'instance') {
              onChange({ ...assertion, expectedValue: JSON.stringify({ $instance: namedInstances[0]?.name || '' }) });
            } else {
              onChange({ ...assertion, expectedValue: '0' });
            }
          }}
        >
          <option value="literal">Literal</option>
          <option value="instance">Named instance</option>
        </select>
      )}
      {useInstance ? (
        <select
          className={FIELD_CLASS_COMPACT}
          value={instanceName || ''}
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
      ) : (
        <input
          className={`${FIELD_CLASS_COMPACT} font-mono`}
          value={assertion.expectedValue || ''}
          onChange={(e) => onChange({ ...assertion, expectedValue: e.target.value })}
          placeholder="Expected value JSON"
        />
      )}
    </>
  );
}

export default function TestcaseAssertionFields({
  assertion,
  step,
  catalog,
  namedInstances = [],
  allowEquals = false,
  allowFieldInstanceRef = false,
  onChange,
  onRemove,
  canRemove = false,
}) {
  const kinds = allowedAssertionKinds(step, catalog);
  const objectReturn = isObjectReturnStep(step, catalog);
  const showObjectCheck = assertion.assertionKind === 'RETURN_VALUE' && objectReturn;
  const showComparison = assertion.assertionKind !== 'EXCEPTION' && !showObjectCheck;

  return (
    <div className="grid gap-2 rounded border border-border p-2 sm:grid-cols-3">
      <select
        className={FIELD_CLASS_COMPACT}
        value={kinds.includes(assertion.assertionKind) ? assertion.assertionKind : kinds[0]}
        onChange={(e) => {
          const nextKind = e.target.value;
          onChange({
            ...assertion,
            assertionKind: nextKind,
            fieldId: nextKind === 'FIELD_STATE' ? (assertion.fieldId || null) : null,
            expectedValue: defaultExpectedValue(nextKind, nextKind === 'RETURN_VALUE' && objectReturn),
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
          onChange={(e) => onChange({ ...assertion, fieldId: e.target.value || null })}
        >
          <option value="">Field</option>
          {catalog.fields.map((opt) => (
            <option key={opt.id} value={opt.id}>{opt.label}</option>
          ))}
        </select>
      )}

      {showObjectCheck && (
        <ObjectCheckFields
          assertion={assertion}
          step={step}
          catalog={catalog}
          namedInstances={namedInstances}
          allowEquals={allowEquals}
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

      {assertion.assertionKind === 'RETURN_VALUE' && !showObjectCheck && (
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
          value={assertion.comparisonMode || 'EXACT'}
          onChange={(e) => onChange({ ...assertion, comparisonMode: e.target.value })}
        >
          {COMPARISON_MODES.map((mode) => (
            <option key={mode} value={mode}>{mode}</option>
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
