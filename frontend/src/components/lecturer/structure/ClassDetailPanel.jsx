import React from 'react';
import { ChevronDown, ChevronRight, Plus, Trash2 } from 'lucide-react';
import Combobox from '../../ui/Combobox';
import ParameterRows from './ParameterRows';
import WeightInput from './WeightInput';

function ScopeSelect({ value, options, onChange }) {
  return (
    <Combobox
      allowEmpty
      emptyLabel="Scope"
      placeholder="Scope"
      className="w-full min-w-0"
      value={value ?? ''}
      options={(options || []).map((opt) => ({
        value: String(opt.id),
        label: opt.name,
      }))}
      onChange={(next) => onChange(next === '' ? null : Number(next))}
    />
  );
}

function isEnumDeclaringType(declaringTypeId, options) {
  const match = (options || []).find((opt) => opt.id === declaringTypeId);
  return !!match && String(match.name || '').trim().toUpperCase() === 'ENUM';
}

function Section({ title, count, children, defaultOpen = true }) {
  const [open, setOpen] = React.useState(defaultOpen);
  return (
    <div className="rounded-xl border border-border bg-surface">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        className="flex w-full items-center justify-between px-4 py-3 text-left font-medium text-foreground"
      >
        <span>{title}{count != null ? ` (${count})` : ''}</span>
        {open ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
      </button>
      {open && <div className="border-t border-border px-4 py-4 dark:border-border">{children}</div>}
    </div>
  );
}

export default function ClassDetailPanel({
  classData,
  challengeClasses,
  scopeOptions,
  declaringTypeOptions,
  relationTypeOptions = [],
  relations = [],
  onChange,
  onRelationsChange,
}) {
  const [draftKind, setDraftKind] = React.useState(null);

  React.useEffect(() => {
    setDraftKind(null);
  }, [classData?.id]);

  if (!classData) {
    return (
      <div className="flex h-full min-h-[24rem] items-center justify-center rounded-xl border border-dashed border-border text-foreground-secondary">
        Select a class from the structure sidebar to edit its details.
      </div>
    );
  }

  const enumClass = isEnumDeclaringType(classData.declaringTypeId, declaringTypeOptions);

  React.useEffect(() => {
    if (!enumClass) return;
    const fields = classData.fields || [];
    if (!fields.some((field) => (field.dataType && String(field.dataType).trim()) || field.scopeId != null)) {
      return;
    }
    onChange({
      ...classData,
      fields: fields.map((field) => ({ ...field, dataType: '', scopeId: null })),
    });
  }, [enumClass, classData, onChange]);

  const patch = (updates) => onChange({ ...classData, ...updates });

  const outerClassOptions = (challengeClasses || []).filter(
    (cls) => cls.id !== classData.id && !cls.outerClassId,
  );
  const heritageTargets = (challengeClasses || []).filter((cls) => cls.id !== classData.id);
  const canSetHeritage = heritageTargets.length >= 1;
  const derivedPair = deriveHeritagePair(classData.id, relations, relationTypeOptions);
  const pair = derivedPair.kind !== 'none'
    ? derivedPair
    : { kind: draftKind || 'none', targetClassId: null, relationId: derivedPair.relationId };

  const updateFields = (fields) => patch({ fields });
  const updateMethods = (methods) => patch({ methods });
  const updateConstructors = (constructors) => patch({ constructors });

  const applyHeritage = (kind, targetClassId) => {
    if (!onRelationsChange) return;
    if (!kind || kind === 'none') {
      setDraftKind(null);
      onRelationsChange(upsertHeritageRelation({
        relations,
        sourceClassId: classData.id,
        kind: 'none',
        targetClassId: null,
        relationTypeOptions,
        existingId: derivedPair.relationId,
      }));
      return;
    }
    if (!targetClassId) {
      setDraftKind(kind);
      if (derivedPair.kind !== 'none') {
        onRelationsChange(upsertHeritageRelation({
          relations,
          sourceClassId: classData.id,
          kind: 'none',
          targetClassId: null,
          relationTypeOptions,
          existingId: derivedPair.relationId,
        }));
      }
      return;
    }
    setDraftKind(null);
    onRelationsChange(upsertHeritageRelation({
      relations,
      sourceClassId: classData.id,
      kind,
      targetClassId,
      relationTypeOptions,
      existingId: derivedPair.relationId,
    }));
  };

  return (
    <div className="space-y-4 pb-4">
      <Section title="Class Definition">
        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <label className="mb-1 block text-xs text-foreground-muted">Class Name</label>
            <input
              className="w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm dark:text-white"
              value={classData.name}
              onChange={(e) => patch({ name: e.target.value })}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-foreground-muted">Scope</label>
            <ScopeSelect value={classData.scopeId} options={scopeOptions} onChange={(scopeId) => patch({ scopeId })} />
          </div>
          <div>
            <label className="mb-1 block text-xs text-foreground-muted">Declaring Type</label>
            <ScopeSelect
              value={classData.declaringTypeId}
              options={declaringTypeOptions}
              onChange={(declaringTypeId) => {
                const nextEnum = isEnumDeclaringType(declaringTypeId, declaringTypeOptions);
                patch({
                  declaringTypeId,
                  fields: nextEnum
                    ? (classData.fields || []).map((field) => ({ ...field, dataType: '', scopeId: null }))
                    : classData.fields,
                });
              }}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-foreground-muted">Outer class (optional)</label>
            <Combobox
              allowEmpty
              emptyLabel="None (top-level)"
              placeholder="None (top-level)"
              className="w-full min-w-0"
              value={classData.outerClassId ?? ''}
              options={outerClassOptions.map((cls) => ({
                value: cls.id,
                label: cls.name,
              }))}
              onChange={(outerClassId) => {
                if (!outerClassId) {
                  patch({ outerClassId: null, isStatic: false });
                } else if (!classData.outerClassId) {
                  patch({ outerClassId, isStatic: true });
                } else {
                  patch({ outerClassId });
                }
              }}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-foreground-muted">Extends / Implements</label>
            <Combobox
              allowEmpty={false}
              className="w-full min-w-0"
              value={pair.kind}
              disabled={!canSetHeritage}
              options={[
                { value: 'none', label: 'None' },
                { value: 'extends', label: 'Extends' },
                { value: 'implements', label: 'Implements' },
              ]}
              onChange={(kind) => {
                if (kind === 'none') {
                  applyHeritage('none', null);
                  return;
                }
                applyHeritage(kind, pair.targetClassId);
              }}
            />
          </div>
          <div>
            <label className="mb-1 block text-xs text-foreground-muted">Target class</label>
            <Combobox
              allowEmpty
              emptyLabel="Select class"
              placeholder="Select class"
              className="w-full min-w-0"
              value={pair.targetClassId ?? ''}
              disabled={!canSetHeritage || pair.kind === 'none'}
              options={heritageTargets.map((cls) => ({
                value: cls.id,
                label: cls.name || 'Untitled class',
              }))}
              onChange={(targetId) => applyHeritage(pair.kind, targetId || null)}
            />
          </div>
          <WeightInput
            id={`class-weight-${classData.id}`}
            label="Class weight"
            value={classData.weight}
            onChange={(weight) => patch({ weight })}
          />
          <label className="flex items-center gap-2 text-sm text-foreground-secondary">
            <input type="checkbox" checked={classData.isAbstract} onChange={(e) => patch({ isAbstract: e.target.checked })} />
            Abstract
          </label>
          {classData.outerClassId && (
            <label className="flex items-center gap-2 text-sm text-foreground-secondary">
              <input
                type="checkbox"
                checked={!!classData.isStatic}
                onChange={(e) => patch({ isStatic: e.target.checked })}
              />
              Static nested
            </label>
          )}
        </div>
      </Section>

      <Section title="Fields" count={classData.fields?.length || 0}>
        <div className="space-y-3">
          {(classData.fields || []).map((field, index) => (
            <div key={field.id} className="grid grid-cols-12 gap-2 items-center">
              <input
                className={`${enumClass ? 'col-span-11' : 'col-span-4'} rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm dark:text-white`}
                placeholder={enumClass ? 'Constant name' : 'Field name'}
                value={field.name}
                onChange={(e) => {
                  const fields = [...classData.fields];
                  fields[index] = { ...field, name: e.target.value };
                  updateFields(fields);
                }}
              />
              {!enumClass && (
                <>
                  <input
                    className="col-span-2 rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm dark:text-white"
                    placeholder="type"
                    value={field.dataType}
                    onChange={(e) => {
                      const fields = [...classData.fields];
                      fields[index] = { ...field, dataType: e.target.value };
                      updateFields(fields);
                    }}
                  />
                  <div className="col-span-2 min-w-0">
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
                  <label className="col-span-2 flex shrink-0 items-center gap-2 whitespace-nowrap text-sm">
                    <input
                      type="checkbox"
                      checked={!!field.isStatic}
                      onChange={(e) => {
                        const fields = [...classData.fields];
                        fields[index] = { ...field, isStatic: e.target.checked };
                        updateFields(fields);
                      }}
                    />
                    static
                  </label>
                  <label className="col-span-1 flex shrink-0 items-center gap-2 whitespace-nowrap text-sm">
                    <input
                      type="checkbox"
                      checked={!!field.isFinal}
                      onChange={(e) => {
                        const fields = [...classData.fields];
                        fields[index] = { ...field, isFinal: e.target.checked };
                        updateFields(fields);
                      }}
                    />
                    final
                  </label>
                </>
              )}
              <button
                type="button"
                className="col-span-1 text-foreground-muted hover:text-error"
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
              {
                id: crypto.randomUUID(),
                name: '',
                dataType: '',
                scopeId: enumClass ? null : scopeOptions[0]?.id,
                isStatic: false,
                isFinal: false,
              },
            ])}
            className="flex w-full items-center justify-center gap-2 rounded-xl border border-dashed border-border dark:border-surface-tertiary py-3 text-sm text-primary-text transition-colors hover:bg-surface-secondary"
          >
            <Plus className="h-4 w-4" /> Add field
          </button>
        </div>
      </Section>

      <Section title="Methods" count={classData.methods?.length || 0}>
        <div className="space-y-4">
          {(classData.methods || []).map((method, index) => (
            <div key={method.id} className="rounded-lg border border-border p-3 dark:border-border">
              <div className="grid gap-2 md:grid-cols-2">
                <input
                  className="rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm dark:text-white"
                  placeholder="Method name"
                  value={method.name}
                  onChange={(e) => {
                    const methods = [...classData.methods];
                    methods[index] = { ...method, name: e.target.value };
                    updateMethods(methods);
                  }}
                />
                <input
                  className="rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm dark:text-white"
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
                  className="col-span-1 text-foreground-muted hover:text-error"
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
            className="flex w-full items-center justify-center gap-2 rounded-xl border border-dashed border-border dark:border-surface-tertiary py-3 text-sm text-primary-text transition-colors hover:bg-surface-secondary"
          >
            <Plus className="h-4 w-4" /> Add method
          </button>
        </div>
      </Section>

      <Section title="Constructors" count={classData.constructors?.length || 0}>
        <div className="space-y-4">
          {(classData.constructors || []).map((ctor, index) => (
            <div key={ctor.id} className="rounded-lg border border-border p-3 dark:border-border">
              <div className="grid grid-cols-12 gap-2 items-center">
                <input
                  className="col-span-5 rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm dark:text-white"
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
                  className="col-span-1 text-foreground-muted hover:text-error"
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
            className="flex w-full items-center justify-center gap-2 rounded-xl border border-dashed border-border dark:border-surface-tertiary py-3 text-sm text-primary-text transition-colors hover:bg-surface-secondary"
          >
            <Plus className="h-4 w-4" /> Add constructor
          </button>
        </div>
      </Section>
    </div>
  );
}

function relationTypeKind(option) {
  const name = (option?.name || '').toLowerCase();
  if (name.includes('bidirectional') && name.includes('inherit')) return null;
  if (name.includes('realiz') || name.includes('implement')) return 'implements';
  if (name.includes('inherit') || name.includes('extend') || name.includes('general')) return 'extends';
  return null;
}

function isHeritageRelation(option) {
  return relationTypeKind(option) != null;
}

function typeIdForKind(relationTypeOptions, kind) {
  const match = (relationTypeOptions || []).find((option) => relationTypeKind(option) === kind);
  return match?.id ?? null;
}

function deriveHeritagePair(sourceClassId, relations, relationTypeOptions) {
  const heritage = (relations || []).filter((relation) => {
    if (relation.sourceClassId !== sourceClassId) return false;
    const option = (relationTypeOptions || []).find((item) => item.id === relation.relationTypeId);
    return isHeritageRelation(option);
  });
  if (heritage.length !== 1) {
    return { kind: 'none', targetClassId: null, relationId: null };
  }
  const row = heritage[0];
  const option = (relationTypeOptions || []).find((item) => item.id === row.relationTypeId);
  return {
    kind: relationTypeKind(option) || 'none',
    targetClassId: row.targetClassId || null,
    relationId: row.id,
  };
}

function upsertHeritageRelation({
  relations,
  sourceClassId,
  kind,
  targetClassId,
  relationTypeOptions,
  existingId,
}) {
  const withoutSourceHeritage = (relations || []).filter((relation) => {
    if (relation.sourceClassId !== sourceClassId) return true;
    const option = (relationTypeOptions || []).find((item) => item.id === relation.relationTypeId);
    return !isHeritageRelation(option);
  });
  if (!kind || kind === 'none' || !targetClassId) {
    return withoutSourceHeritage;
  }
  const relationTypeId = typeIdForKind(relationTypeOptions, kind);
  if (relationTypeId == null) {
    return withoutSourceHeritage;
  }
  return [
    ...withoutSourceHeritage,
    {
      id: existingId || crypto.randomUUID(),
      sourceClassId,
      targetClassId,
      relationTypeId,
    },
  ];
}
