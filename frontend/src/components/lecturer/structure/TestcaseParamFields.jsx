import React from 'react';
import {
  coreTypeName,
  displayScalar,
  FIELD_CLASS,
  isArrayOrListType,
  isInstanceRef,
  isRubricClassType,
  namedInstanceMatchesParamType,
  parseParamsArray,
  parseScalarInput,
  serializeParamsArray,
} from './testcaseAuthoring';

export default function TestcaseParamFields({
  parameters = [],
  paramsJson,
  onChange,
  catalog,
  namedInstances = [],
  allowInstanceRefs = false,
}) {
  if (!parameters.length) {
    return <p className="text-xs text-foreground-muted">No parameters.</p>;
  }

  const values = parseParamsArray(paramsJson, parameters.length);

  const setValue = (index, nextValue) => {
    const next = [...values];
    next[index] = nextValue;
    onChange(serializeParamsArray(next));
  };

  return (
    <div className="space-y-2">
      {parameters.map((param, index) => {
        const label = param.name ? `${param.name}: ${param.dataType || '?'}` : (param.dataType || `arg ${index + 1}`);
        const value = values[index];
        const objectArray = isArrayOrListType(param.dataType) && isRubricClassType(param.dataType, catalog);
        const objectArg = !isArrayOrListType(param.dataType) && isRubricClassType(param.dataType, catalog);

        if (objectArray) {
          return (
            <p key={param.id || index} className="text-xs text-warning-text">
              {label} — arrays of objects cannot be passed as one argument.
            </p>
          );
        }

        if (objectArg && !allowInstanceRefs) {
          return (
            <p key={param.id || index} className="text-xs text-warning-text">
              {label} — object arguments belong in Composition.
            </p>
          );
        }

        if (objectArg) {
          const matching = namedInstances.filter((item) => namedInstanceMatchesParamType(
            item.className,
            param.dataType,
            catalog,
          ));
          const selected = isInstanceRef(value) ? value.$instance : '';
          return (
            <label key={param.id || index} className="block text-xs text-foreground-muted">
              {label}
              <select
                className={FIELD_CLASS}
                value={selected}
                onChange={(e) => setValue(index, e.target.value ? { $instance: e.target.value } : null)}
              >
                <option value="">Named instance</option>
                {matching.map((item) => (
                  <option key={item.name} value={item.name}>{item.name}</option>
                ))}
              </select>
            </label>
          );
        }

        return (
          <label key={param.id || index} className="block text-xs text-foreground-muted">
            {label}
            <input
              className={`${FIELD_CLASS} font-mono`}
              value={displayScalar(value)}
              onChange={(e) => setValue(index, parseScalarInput(e.target.value))}
              placeholder={isArrayOrListType(param.dataType) ? '[1, 2]' : 'value'}
            />
          </label>
        );
      })}
    </div>
  );
}
