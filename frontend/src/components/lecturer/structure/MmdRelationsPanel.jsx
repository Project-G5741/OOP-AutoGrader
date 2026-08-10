import React from 'react';
import { GitBranch, Plus, Trash2 } from 'lucide-react';

function ClassSelect({ value, options, onChange, placeholder }) {
  return (
    <select
      value={value ?? ''}
      onChange={(e) => onChange(e.target.value || null)}
      className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
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
      <div className="flex h-full min-h-[24rem] items-center justify-center rounded-xl border border-dashed border-gray-300 text-gray-500 dark:border-gray-700 dark:text-gray-400">
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
      <div className="rounded-xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-[#0f1419]">
        <div className="flex items-center gap-2 border-b border-gray-200 px-4 py-3 dark:border-gray-800">
          <GitBranch className="h-4 w-4 text-blue-400" />
          <div>
            <h3 className="font-medium text-gray-900 dark:text-gray-100">MMD Relationships</h3>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              {challenge.name} — define class-to-class relations graded from the student MMD diagram.
            </p>
          </div>
        </div>

        <div className="space-y-3 px-4 py-4">
          {classes.length < 2 && (
            <p className="rounded-lg border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-800 dark:border-amber-800 dark:bg-amber-950 dark:text-amber-200">
              Add at least two classes in this problem before defining relationships.
            </p>
          )}

          {relations.length === 0 && classes.length >= 2 && (
            <p className="text-sm text-gray-500 dark:text-gray-400">
              No relationships yet. Class declarations are graded separately; add relations here for inheritance, composition, association, and other MMD arrows.
            </p>
          )}

          {relations.map((relation, index) => (
            <div
              key={relation.id}
              className="grid gap-3 rounded-lg border border-gray-200 p-3 dark:border-gray-800 md:grid-cols-[1fr_1fr_1fr_auto]"
            >
              <div>
                <label className="mb-1 block text-xs text-gray-500">Source class</label>
                <ClassSelect
                  value={relation.sourceClassId}
                  options={classes}
                  placeholder="Source"
                  onChange={(sourceClassId) => updateRelation(relation.id, { sourceClassId })}
                />
              </div>
              <div>
                <label className="mb-1 block text-xs text-gray-500">Relation type</label>
                <select
                  value={relation.relationTypeId ?? ''}
                  onChange={(e) => updateRelation(relation.id, { relationTypeId: Number(e.target.value) })}
                  className="w-full rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm dark:border-gray-700 dark:bg-[#0d1117] dark:text-white"
                >
                  <option value="">Type</option>
                  {relationTypeOptions.map((opt) => (
                    <option key={opt.id} value={opt.id}>{opt.name}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="mb-1 block text-xs text-gray-500">Target class</label>
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
                  className="rounded-lg border border-gray-300 p-2 text-gray-400 hover:border-red-400 hover:text-red-400 dark:border-gray-700"
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
            className="inline-flex items-center gap-2 rounded-lg border border-dashed border-purple-400 px-3 py-2 text-sm text-purple-500 hover:bg-purple-500/5 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <Plus className="h-4 w-4" /> Add relationship
          </button>
        </div>
      </div>
    </div>
  );
}
