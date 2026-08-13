import React from 'react';
import MmdRelationsPanel from './MmdRelationsPanel';
import TestcasesPanel from './TestcasesPanel';

export default function ChallengeDetailPanel({
  challenge,
  relationTypeOptions,
  onMmdChange,
  activeTab,
  onTabChange,
  labId,
  structureDirty,
  onToast,
}) {
  if (!challenge) {
    return (
      <div className="flex h-full min-h-[24rem] items-center justify-center rounded-xl border border-dashed border-gray-300 text-gray-500 dark:border-gray-700 dark:text-gray-400">
        Select a problem from the structure sidebar.
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex gap-1 border-b border-gray-200 dark:border-gray-800">
        <button
          type="button"
          onClick={() => onTabChange('mmd')}
          className={`px-4 py-2 text-sm font-medium ${
            activeTab === 'mmd'
              ? 'border-b-2 border-blue-400 text-blue-300'
              : 'text-gray-500 hover:text-gray-300'
          }`}
        >
          MMD Relations
        </button>
        <button
          type="button"
          onClick={() => onTabChange('testcases')}
          className={`px-4 py-2 text-sm font-medium ${
            activeTab === 'testcases'
              ? 'border-b-2 border-emerald-400 text-emerald-300'
              : 'text-gray-500 hover:text-gray-300'
          }`}
        >
          Operational Testcases
        </button>
      </div>

      {activeTab === 'mmd' ? (
        <MmdRelationsPanel
          challenge={challenge}
          relationTypeOptions={relationTypeOptions}
          onChange={onMmdChange}
        />
      ) : (
        <TestcasesPanel
          labId={labId}
          challenge={challenge}
          structureDirty={structureDirty}
          onToast={onToast}
        />
      )}
    </div>
  );
}
