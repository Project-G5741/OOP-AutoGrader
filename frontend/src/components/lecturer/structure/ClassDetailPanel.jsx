import React from 'react';
import { ChevronDown, ChevronRight, Plus, Trash2 } from 'lucide-react';
import ParameterRows from './ParameterRows';

function ScopeSelect({ value, options, onChange }) {
  return (
    <select
      value={value ?? ''}
      onChange={(e) => onChange(Number(e.target.value))}
      className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
    >
      <option value="">Scope</option>
      {options.map((opt) => (
        <option key={opt.id} value={opt.id}>{opt.name}</option>
      ))}
    </select>
  );
}

function Section({ title, count, children, defaultOpen = true }) {
  const [open, setOpen] = React.useState(defaultOpen);
  return (
    <div className="rounded-xl border border-gray-200 dark:border-gray-800 bg-white dark:bg-[#0f1419]">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="flex w-full items-center justify-between px-4 py-3 text-left font-medium text-gray-900 dark:text-gray-100"
      >
        <span>{title}{count != null ? ` (${count})` : ''}</span>
        {open ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
      </button>
      {open && <div className="border-t border-gray-200 px-4 py-4 dark:border-gray-800">{children}</div>}
    </div>
  );
}

export default function ClassDetailPanel({ classData, scopeOptions, declaringTypeOptions, onChange }) {
  if (!classData) {
    return (
      <div className="flex h-full min-h-[24rem] items-center justify-center rounded-xl border border-dashed border-gray-300 text-gray-500 dark:border-gray-700 dark:text-gray-400">
        Select a class from the structure sidebar to edit its details.
      </div>
    );
  }

  const patch = (updates) => onChange({ ...classData, ...updates });

  const updateFields = (fields) => patch({ fields });
  const updateMethods = (methods) => patch({ methods });
  const updateConstructors = (constructors) => patch({ constructors });

  return (
    <div className="space-y-4 pb-4">
      <Section title="Class Definition">
        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <label className="mb-1 block text-xs text-gray-500">Class Name</label>
            <input
              className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
              value={classData.name}
              onChange={(e) => patch({ name: e.target.value })}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-gray-500">Scope</label>
            <ScopeSelect value={classData.scopeId} options={scopeOptions} onChange={(scopeId) => patch({ scopeId })} />
          </div>
          <div>
            <label className="mb-1 block text-xs text-gray-500">Declaring Type</label>
            <ScopeSelect
              value={classData.declaringTypeId}
              options={declaringTypeOptions}
              onChange={(declaringTypeId) => patch({ declaringTypeId })}
            />
          </div>
          <label className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-200">
            <input type="checkbox" checked={classData.isAbstract} onChange={(e) => patch({ isAbstract: e.target.checked })} />
            Abstract
          </label>
        </div>
      </Section>

      <Section title="Fields" count={classData.fields?.length || 0}>
        <div className="space-y-3">
          {(classData.fields || []).map((field, index) => (
            <div key={field.id} className="grid grid-cols-12 gap-2 items-center">
              <input
                className="col-span-6 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
                placeholder="Field name"
                value={field.name}
                onChange={(e) => {
                  const fields = [...classData.fields];
                  fields[index] = { ...field, name: e.target.value };
                  updateFields(fields);
                }}
              />
              <input
                className="col-span-3 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
                placeholder="type"
                value={field.dataType}
                onChange={(e) => {
                  const fields = [...classData.fields];
                  fields[index] = { ...field, dataType: e.target.value };
                  updateFields(fields);
                }}
              />
              <div className="col-span-2">
                <ScopeSelect
                  value={field.scopeId}
                  options={scopeOptions}
                  onChange={(scopeId) => {
                    const fields = [...classData.fields];
                    fields[index] = { ...field, scopeId };
                    updateFields(fields);
                  }}
                />
              </div>
              <button
                type="button"
                className="col-span-1 text-gray-400 hover:text-red-400"
                onClick={() => updateFields(classData.fields.filter((_, i) => i !== index))}
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </div>
          ))}
          <button
            type="button"
            onClick={() => updateFields([
              ...(classData.fields || []),
              { id: crypto.randomUUID(), name: '', dataType: '', scopeId: scopeOptions[0]?.id },
            ])}
            className="flex w-full items-center justify-center gap-2 rounded-xl border border-dashed border-purple-500/50 py-3 text-sm text-purple-500 hover:bg-purple-500/5"
          >
            <Plus className="h-4 w-4" /> Add field
          </button>
        </div>
      </Section>

      <Section title="Methods" count={classData.methods?.length || 0}>
        <div className="space-y-4">
          {(classData.methods || []).map((method, index) => (
            <div key={method.id} className="rounded-lg border border-gray-200 p-3 dark:border-gray-800">
              <div className="grid gap-2 md:grid-cols-2">
                <input
                  className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
                  placeholder="Method name"
                  value={method.name}
                  onChange={(e) => {
                    const methods = [...classData.methods];
                    methods[index] = { ...method, name: e.target.value };
                    updateMethods(methods);
                  }}
                />
                <input
                  className="rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
                  placeholder="Return type"
                  value={method.returnType}
                  onChange={(e) => {
                    const methods = [...classData.methods];
                    methods[index] = { ...method, returnType: e.target.value };
                    updateMethods(methods);
                  }}
                />
              </div>
              <div className="mt-2 grid grid-cols-12 gap-2 items-center">
                <div className="col-span-4">
                  <ScopeSelect
                    value={method.scopeId}
                    options={scopeOptions}
                    onChange={(scopeId) => {
                      const methods = [...classData.methods];
                      methods[index] = { ...method, scopeId };
                      updateMethods(methods);
                    }}
                  />
                </div>
                <label className="col-span-3 flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={method.isStatic}
                    onChange={(e) => {
                      const methods = [...classData.methods];
                      methods[index] = { ...method, isStatic: e.target.checked };
                      updateMethods(methods);
                    }}
                  />
                  static
                </label>
                <label className="col-span-4 flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={method.isAbstract}
                    onChange={(e) => {
                      const methods = [...classData.methods];
                      methods[index] = { ...method, isAbstract: e.target.checked };
                      updateMethods(methods);
                    }}
                  />
                  abstract
                </label>
                <button
                  type="button"
                  className="col-span-1 text-gray-400 hover:text-red-400"
                  onClick={() => updateMethods(classData.methods.filter((_, i) => i !== index))}
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
              <ParameterRows
                parameters={method.parameters || []}
                onChange={(parameters) => {
                  const methods = [...classData.methods];
                  methods[index] = { ...method, parameters };
                  updateMethods(methods);
                }}
              />
            </div>
          ))}
          <button
            type="button"
            onClick={() => updateMethods([
              ...(classData.methods || []),
              {
                id: crypto.randomUUID(),
                name: '',
                returnType: 'void',
                scopeId: scopeOptions[0]?.id,
                isStatic: false,
                isAbstract: false,
                parameters: [],
              },
            ])}
            className="flex w-full items-center justify-center gap-2 rounded-xl border border-dashed border-purple-500/50 py-3 text-sm text-purple-500 hover:bg-purple-500/5"
          >
            <Plus className="h-4 w-4" /> Add method
          </button>
        </div>
      </Section>

      <Section title="Constructors" count={classData.constructors?.length || 0}>
        <div className="space-y-4">
          {(classData.constructors || []).map((ctor, index) => (
            <div key={ctor.id} className="rounded-lg border border-gray-200 p-3 dark:border-gray-800">
              <div className="grid grid-cols-12 gap-2 items-center">
                <input
                  className="col-span-5 rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
                  placeholder="Constructor name"
                  value={ctor.name}
                  onChange={(e) => {
                    const constructors = [...classData.constructors];
                    constructors[index] = { ...ctor, name: e.target.value };
                    updateConstructors(constructors);
                  }}
                />
                <div className="col-span-3">
                  <ScopeSelect
                    value={ctor.scopeId}
                    options={scopeOptions}
                    onChange={(scopeId) => {
                      const constructors = [...classData.constructors];
                      constructors[index] = { ...ctor, scopeId };
                      updateConstructors(constructors);
                    }}
                  />
                </div>
                <label className="col-span-3 flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={ctor.isDefault}
                    onChange={(e) => {
                      const constructors = [...classData.constructors];
                      constructors[index] = { ...ctor, isDefault: e.target.checked };
                      updateConstructors(constructors);
                    }}
                  />
                  default
                </label>
                <button
                  type="button"
                  className="col-span-1 text-gray-400 hover:text-red-400"
                  onClick={() => updateConstructors(classData.constructors.filter((_, i) => i !== index))}
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
              <ParameterRows
                parameters={ctor.parameters || []}
                onChange={(parameters) => {
                  const constructors = [...classData.constructors];
                  constructors[index] = { ...ctor, parameters };
                  updateConstructors(constructors);
                }}
              />
            </div>
          ))}
          <button
            type="button"
            onClick={() => updateConstructors([
              ...(classData.constructors || []),
              {
                id: crypto.randomUUID(),
                name: classData.name || '',
                scopeId: scopeOptions[0]?.id,
                isDefault: false,
                parameters: [],
              },
            ])}
            className="flex w-full items-center justify-center gap-2 rounded-xl border border-dashed border-purple-500/50 py-3 text-sm text-purple-500 hover:bg-purple-500/5"
          >
            <Plus className="h-4 w-4" /> Add constructor
          </button>
        </div>
      </Section>
    </div>
  );
}
