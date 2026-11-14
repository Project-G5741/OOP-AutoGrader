import React from 'react';
import { Plus, Trash2 } from 'lucide-react';
import AliasListBox from './AliasListBox';

export default function ParameterRows({ parameters = [], onChange, label = 'Parameters' }) {
  const updateParam = (index, patch) => {
    const next = parameters.map((row, i) => (i === index ? { ...row, ...patch } : row));
    onChange(next);
  };

  const addParam = () => {
    onChange([
      ...parameters,
      {
        id: crypto.randomUUID(),
        name: '',
        names: [''],
        dataType: '',
        dataTypes: [''],
        orderIndex: parameters.length,
        isFinal: false,
      },
    ]);
  };

  const removeParam = (index) => {
    onChange(parameters.filter((_, i) => i !== index).map((row, i) => ({ ...row, orderIndex: i })));
  };

  return (
    <div className="mt-2 space-y-2">
      <div className="text-xs font-medium uppercase tracking-wide text-foreground-secondary">{label}</div>
      {parameters.map((param, index) => (
        <div key={param.id || index} className="grid grid-cols-12 gap-2 items-center">
          <AliasListBox
            className="col-span-5"
            placeholder="name"
            ariaLabel={`Parameter ${index + 1} name`}
            addLabel="Add name"
            values={param.names}
            fallback={param.name}
            onChange={(names) => updateParam(index, { names, name: names[0] })}
          />
          <AliasListBox
            className="col-span-6"
            placeholder="type"
            ariaLabel={`Parameter ${index + 1} datatype`}
            addLabel="Add datatype"
            values={param.dataTypes}
            fallback={param.dataType}
            onChange={(dataTypes) => updateParam(index, { dataTypes, dataType: dataTypes[0] })}
          />
          <button
            type="button"
            onClick={() => removeParam(index)}
            className="col-span-1 flex justify-center text-foreground-muted hover:text-error"
            aria-label="Remove parameter"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      ))}
      <button
        type="button"
        onClick={addParam}
        className="inline-flex items-center gap-1 text-sm text-primary hover:text-primary"
      >
        <Plus className="h-4 w-4" /> Add parameter
      </button>
    </div>
  );
}
