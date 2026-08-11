import React from 'react';
import { Plus, Trash2 } from 'lucide-react';

export default function ParameterRows({ parameters = [], onChange, label = 'Parameters' }) {
  const updateParam = (index, patch) => {
    const next = parameters.map((row, i) => (i === index ? { ...row, ...patch } : row));
    onChange(next);
  };

  const addParam = () => {
    onChange([
      ...parameters,
      { id: crypto.randomUUID(), name: '', dataType: '', orderIndex: parameters.length, isFinal: false },
    ]);
  };

  const removeParam = (index) => {
    onChange(parameters.filter((_, i) => i !== index).map((row, i) => ({ ...row, orderIndex: i })));
  };

  return (
    <div className="mt-2 space-y-2">
      <div className="text-xs font-medium uppercase tracking-wide text-gray-500 dark:text-gray-400">{label}</div>
      {parameters.map((param, index) => (
        <div key={param.id || index} className="grid grid-cols-12 gap-2 items-center">
          <input
            className="col-span-5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
            placeholder="name"
            value={param.name}
            onChange={(e) => updateParam(index, { name: e.target.value })}
          />
          <input
            className="col-span-6 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
            placeholder="type"
            value={param.dataType}
            onChange={(e) => updateParam(index, { dataType: e.target.value })}
          />
          <button
            type="button"
            onClick={() => removeParam(index)}
            className="col-span-1 flex justify-center text-gray-400 hover:text-red-400"
            aria-label="Remove parameter"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        </div>
      ))}
      <button
        type="button"
        onClick={addParam}
        className="inline-flex items-center gap-1 text-sm text-purple-500 hover:text-purple-400"
      >
        <Plus className="h-4 w-4" /> Add parameter
      </button>
    </div>
  );
}
