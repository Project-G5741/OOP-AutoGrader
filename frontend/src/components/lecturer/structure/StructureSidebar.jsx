import React from 'react';
import { BookOpen, ChevronDown, ChevronRight, Code2, Layers, Plus, Trash2 } from 'lucide-react';

export default function StructureSidebar({
  labs,
  draft,
  selectedLabId,
  expandedLabs,
  expandedChallenges,
  selectedChallengeId,
  selectedClassId,
  onSelectLab,
  onToggleLab,
  onToggleChallenge,
  onSelectChallenge,
  onSelectClass,
  onAddLab,
  onAddChallenge,
  onAddClass,
  onDeleteLab,
  onDeleteChallenge,
  onDeleteClass,
}) {
  return (
    <aside className="w-full max-w-xs shrink-0 rounded-xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-[#0f1419]">
      <div className="flex items-center justify-between border-b border-gray-200 px-4 py-3 dark:border-gray-800">
        <span className="text-xs font-semibold tracking-widest text-gray-500">STRUCTURE</span>
        <button type="button" onClick={onAddLab} className="text-gray-400 hover:text-purple-500" aria-label="Add lab">
          <Plus className="h-4 w-4" />
        </button>
      </div>
      <div className="max-h-[calc(100vh-12rem)] overflow-y-auto p-2">
        {labs.map((lab) => {
          const isSelectedLab = selectedLabId === lab.id;
          const labOpen = expandedLabs[lab.id];
          const labDraft = isSelectedLab ? draft : null;
          return (
            <div key={lab.id} className="mb-1">
              <div className={`flex items-center gap-1 rounded-lg px-2 py-2 ${isSelectedLab ? 'bg-purple-500/10' : 'hover:bg-gray-50 dark:hover:bg-[#161b22]'}`}>
                <button type="button" onClick={() => onToggleLab(lab.id)} className="text-gray-400">
                  {labOpen ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                </button>
                <button type="button" onClick={() => onSelectLab(lab.id)} className="flex flex-1 items-center gap-2 text-left text-sm text-gray-800 dark:text-gray-100">
                  <BookOpen className="h-4 w-4 shrink-0 text-purple-400" />
                  <span className="truncate">{lab.name}</span>
                </button>
                <button type="button" onClick={() => onDeleteLab(lab.id)} className="text-gray-400 hover:text-red-400">
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </div>
              {labOpen && isSelectedLab && labDraft && (
                <div className="ml-4 border-l border-gray-200 pl-2 dark:border-gray-800">
                  {(labDraft.challenges || []).map((challenge) => {
                    const challengeOpen = expandedChallenges[challenge.id];
                    return (
                      <div key={challenge.id} className="mb-1">
                        <div className={`flex items-center gap-1 rounded-lg px-2 py-1.5 ${
                          selectedChallengeId === challenge.id && !selectedClassId
                            ? 'bg-blue-500/15'
                            : 'hover:bg-gray-50 dark:hover:bg-[#161b22]'
                        }`}>
                          <button type="button" onClick={() => onToggleChallenge(challenge.id)} className="text-gray-400">
                            {challengeOpen ? <ChevronDown className="h-3.5 w-3.5" /> : <ChevronRight className="h-3.5 w-3.5" />}
                          </button>
                          <button
                            type="button"
                            onClick={() => onSelectChallenge(challenge.id)}
                            className="flex flex-1 items-center gap-2 text-left text-sm text-gray-700 dark:text-gray-300"
                          >
                            <Layers className="h-3.5 w-3.5 text-blue-400" />
                            <span className="truncate">{challenge.name}</span>
                          </button>
                          <button type="button" onClick={() => onDeleteChallenge(challenge.id)} className="text-gray-400 hover:text-red-400">
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        </div>
                        {challengeOpen && (
                          <div className="ml-4 space-y-1 border-l border-gray-200 pl-2 dark:border-gray-800">
                            {(challenge.classes || []).map((cls) => (
                              <button
                                key={cls.id}
                                type="button"
                                onClick={() => onSelectClass(challenge.id, cls.id)}
                                className={`flex w-full items-center justify-between rounded-lg px-2 py-1.5 text-left text-sm ${
                                  selectedClassId === cls.id
                                    ? 'bg-emerald-500/15 text-emerald-300'
                                    : 'text-gray-600 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-[#161b22]'
                                }`}
                              >
                                <span className="flex items-center gap-2 truncate">
                                  <Code2 className="h-3.5 w-3.5" />
                                  {cls.name || 'Untitled class'}
                                </span>
                                <span
                                  role="button"
                                  tabIndex={0}
                                  onClick={(e) => { e.stopPropagation(); onDeleteClass(challenge.id, cls.id); }}
                                  onKeyDown={(e) => { if (e.key === 'Enter') { e.stopPropagation(); onDeleteClass(challenge.id, cls.id); } }}
                                  className="text-gray-400 hover:text-red-400"
                                >
                                  <Trash2 className="h-3.5 w-3.5" />
                                </span>
                              </button>
                            ))}
                            <button
                              type="button"
                              onClick={() => onAddClass(challenge.id)}
                              className="flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-xs text-purple-500 hover:bg-purple-500/5"
                            >
                              <Plus className="h-3.5 w-3.5" /> Add class
                            </button>
                          </div>
                        )}
                      </div>
                    );
                  })}
                  <button
                    type="button"
                    onClick={onAddChallenge}
                    className="mt-1 flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-xs text-purple-500 hover:bg-purple-500/5"
                  >
                    <Plus className="h-3.5 w-3.5" /> Add problem
                  </button>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </aside>
  );
}
