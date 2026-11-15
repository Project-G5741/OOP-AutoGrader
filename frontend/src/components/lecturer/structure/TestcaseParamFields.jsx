import React from 'react';
import Combobox from '../../ui/Combobox';
import {
  coreTypeName,
  displayScalar,
  FIELD_CLASS,
  isArrayOrListType,
  isInstanceRef,
  isLocalDateType,
  isOrdinaryRubricClassType,
  namedInstanceMatchesParamType,
  parseParamsArray,
  parseScalarInput,
  parameterFieldLabel,
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
        const label = parameterFieldLabel(param, index);
        const value = values[index];
        const objectArray = isArrayOrListType(param.dataType)
          && isOrdinaryRubricClassType(param.dataType, catalog);
        const objectArg = !isArrayOrListType(param.dataType)
          && isOrdinaryRubricClassType(param.dataType, catalog);

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
            <div key={param.id || index} className="block text-xs text-foreground-muted">
              <span className="mb-1 block">{label}</span>
              <Combobox
                aria-label={label}
                value={selected}
                placeholder="Named instance"
                emptyLabel="Named instance"
                allowEmpty
                size="sm"
                onChange={(val) => setValue(index, val ? { $instance: val } : null)}
                options={matching.map((item) => ({ value: item.name, label: item.name }))}
                className="w-full min-w-0"
              />
            </div>
          );
        }

        return (
          <label key={param.id || index} className="block text-xs text-foreground-muted">
            {label}
            <input
              className={`${FIELD_CLASS} font-mono`}
              value={displayScalar(value)}
              onChange={(e) => setValue(index, parseScalarInput(e.target.value))}
              placeholder={
                isLocalDateType(param.dataType)
                  ? 'yyyy-MM-dd'
                  : (isArrayOrListType(param.dataType) ? '[1, 2]' : 'value')
              }
            />
          </label>
        );
      })}
    </div>
  );
}
