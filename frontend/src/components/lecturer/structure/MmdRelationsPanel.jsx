import React from 'react';
import { GitBranch, Plus, Trash2 } from 'lucide-react';

function ClassSelect({ value, options, onChange, placeholder }) {
  return (
    <select
      value={value ?? ''}
      onChange={(e) => onChange(e.target.value || null)}
      className="w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm dark:text-white"
    >
      <option value="">{placeholder}</option>
      {options.map((cls) => (
        <option key={cls.id} value={cls.id}>{cls.name || 'Untitled class'}</option>
      ))}
    </select>
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
  const hasMmd = challenge.hasMmd !== false;

  const patchRelations = (nextRelations) => onChange({ ...challenge, relations: nextRelations });
  const setHasMmd = (next) => onChange({ ...challenge, hasMmd: next });

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
      <div className="rounded-xl border border-border bg-surface dark:border-border">
        <div className="flex items-center justify-between gap-2 border-b border-border px-4 py-3 dark:border-border">
          <div className="flex items-center gap-2">
            <GitBranch className="h-4 w-4 text-chart-blue" />
            <div>
              <h3 className="font-medium text-foreground">MMD Relationships</h3>
              <p className="text-xs text-foreground-secondary">
                {challenge.name} — define class-to-class relations graded from the student MMD diagram.
              </p>
            </div>
          </div>
          <label className="flex shrink-0 items-center gap-2 text-sm text-foreground-secondary">
            <input
              type="checkbox"
              checked={hasMmd}
              onChange={(e) => setHasMmd(e.target.checked)}
              className="h-4 w-4 rounded border-border text-primary focus:ring-primary"
            />
            Requires MMD diagram
          </label>
        </div>

        {!hasMmd && (
          <div className="mx-4 mt-4 rounded-lg border border-warning bg-warning-bg px-3 py-2 text-sm text-warning-text">
            This problem doesn't require an MMD diagram. The MMD pillar won't be graded — the total score
            is redistributed across the remaining pillars. Any relations defined below are saved but not scored.
          </div>
        )}

        <div className="space-y-3 px-4 py-4">
          {classes.length < 2 && (
            <p className="rounded-lg border border-warning bg-warning-bg px-3 py-2 text-sm text-warning-text">
              Add at least two classes in this problem before defining relationships.
            </p>
          )}

          {relations.length === 0 && classes.length >= 2 && (
            <p className="text-sm text-foreground-secondary">
              No relationships yet. Class declarations are graded separately; add relations here for inheritance, composition, association, and other MMD arrows.
            </p>
          )}

          {relations.map((relation, index) => (
            <div
              key={relation.id}
              className="grid gap-3 rounded-lg border border-border p-3 dark:border-border md:grid-cols-[1fr_1fr_1fr_auto]"
            >
              <div>
                <label className="mb-1 block text-xs text-foreground-muted">Source class</label>
                <ClassSelect
                  value={relation.sourceClassId}
                  options={classes}
                  placeholder="Source"
                  onChange={(sourceClassId) => updateRelation(relation.id, { sourceClassId })}
                />
              </div>
              <div>
                <label className="mb-1 block text-xs text-foreground-muted">Relation type</label>
                <select
                  value={relation.relationTypeId ?? ''}
                  onChange={(e) => updateRelation(relation.id, { relationTypeId: Number(e.target.value) })}
                  className="w-full rounded-lg border border-border bg-surface-secondary px-3 py-2 text-sm dark:text-white"
                >
                  <option value="">Type</option>
                  {relationTypeOptions.map((opt) => (
                    <option key={opt.id} value={opt.id}>{opt.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-xs text-foreground-muted">Target class</label>
                <ClassSelect
                  value={relation.targetClassId}
                  options={classes.filter((cls) => cls.id !== relation.sourceClassId)}
                  placeholder="Target"
                  onChange={(targetClassId) => updateRelation(relation.id, { targetClassId })}
                />
              </div>
              <div className="flex items-end">
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
