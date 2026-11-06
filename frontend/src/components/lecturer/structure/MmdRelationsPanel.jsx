import React from 'react';
import { GitBranch, Plus, Trash2 } from 'lucide-react';
import Combobox from '../../ui/Combobox';

function ClassSelect({ value, options, onChange, placeholder }) {
  return (
    <Combobox
      allowEmpty
      emptyLabel={placeholder}
      placeholder={placeholder}
      className="w-full min-w-0"
      value={value ?? ''}
      options={(options || []).map((cls) => ({
        value: cls.id,
        label: cls.name || 'Untitled class',
      }))}
      onChange={(next) => onChange(next || null)}
    />
  );
}

export default function MmdRelationsPanel({ challenge, relationTypeOptions, onChange }) {
  if (!challenge) {
    return (
      <div className="flex h-full min-h-[24rem] items-center justify-center rounded-xl border border-dashed border-border text-foreground-secondary">
        Select a problem from the structure sidebar to edit MMD relationships.
      </div>
    );
  }

  const classes = challenge.classes || [];
  const relations = challenge.relations || [];

  const patchRelations = (nextRelations) => onChange({ ...challenge, relations: nextRelations });

  const addRelation = () => {
    if (classes.length < 2) return;
    const relation = {
      id: crypto.randomUUID(),
      sourceClassId: classes[0].id,
      targetClassId: classes[1]?.id || classes[0].id,
      relationTypeId: relationTypeOptions[0]?.id,
    };
    patchRelations([...relations, relation]);
  };

  const updateRelation = (relationId, updates) => {
    patchRelations(relations.map((rel) => (rel.id === relationId ? { ...rel, ...updates } : rel)));
  };

  const removeRelation = (relationId) => {
    patchRelations(relations.filter((rel) => rel.id !== relationId));
  };

  return (
    <div className="space-y-4 pb-4">
      <div className="min-w-0 rounded-xl border border-border bg-surface dark:border-border">
        <div className="flex items-center gap-2 border-b border-border px-4 py-3 dark:border-border">
          <GitBranch className="h-4 w-4 text-chart-blue" />
          <h3 className="font-medium text-foreground">MMD Relationships</h3>
        </div>

        <div className="space-y-3 px-4 py-4">
          {classes.length < 2 && (
            <p className="rounded-lg border border-warning bg-warning-bg px-3 py-2 text-sm text-warning-text">
              Add at least two classes in this problem before defining relationships.
            </p>
          )}

          {relations.length === 0 && classes.length >= 2 && (
            <p className="text-sm text-foreground-secondary">No relationships yet.</p>
          )}

          {relations.map((relation, index) => (
            <div
              key={relation.id}
              className="grid min-w-0 gap-3 rounded-lg border border-border p-3 dark:border-border md:grid-cols-[minmax(0,1fr)_minmax(0,1fr)_minmax(0,1fr)_auto]"
            >
              <div className="min-w-0">
                <label className="mb-1 block text-xs text-foreground-muted">Source class</label>
                <ClassSelect
                  value={relation.sourceClassId}
                  options={classes}
                  placeholder="Source"
                  onChange={(sourceClassId) => updateRelation(relation.id, { sourceClassId })}
                />
              </div>
              <div className="min-w-0">
                <label className="mb-1 block text-xs text-foreground-muted">Relation type</label>
                <Combobox
                  allowEmpty
                  emptyLabel="Type"
                  placeholder="Type"
                  className="w-full min-w-0"
                  value={relation.relationTypeId ?? ''}
                  options={(relationTypeOptions || []).map((opt) => ({
                    value: String(opt.id),
                    label: opt.name,
                  }))}
                  onChange={(next) => updateRelation(relation.id, {
                    relationTypeId: next === '' ? null : Number(next),
                  })}
                />
              </div>
              <div className="min-w-0">
                <label className="mb-1 block text-xs text-foreground-muted">Target class</label>
                <ClassSelect
                  value={relation.targetClassId}
                  options={classes.filter((cls) => cls.id !== relation.sourceClassId)}
                  placeholder="Target"
                  onChange={(targetClassId) => updateRelation(relation.id, { targetClassId })}
                />
              </div>
              <div className="flex shrink-0 items-end">
                <button
                  type="button"
                  onClick={() => removeRelation(relation.id)}
                  className="rounded-lg border border-border p-2 text-foreground-muted hover:border-error hover:text-error"
                  aria-label={`Remove relation ${index + 1}`}
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}

          <button
            type="button"
            disabled={classes.length < 2 || relationTypeOptions.length === 0}
            onClick={addRelation}
            className="inline-flex items-center gap-2 rounded-lg border border-dashed border-border dark:border-surface-tertiary px-3 py-2 text-sm text-primary-text transition-colors hover:bg-surface-secondary disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Plus className="h-4 w-4" /> Add relationship
          </button>
        </div>
      </div>
    </div>
  );
}
