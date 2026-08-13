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
  onRenameChallenge,
  onAddLab,
  onAddChallenge,
  onAddClass,
  onDeleteLab,
  onDeleteChallenge,
  onDeleteClass,
}) {
  return (
    <aside className="w-full max-w-xs shrink-0 rounded-xl border border-border bg-surface dark:border-border">
      <div className="flex items-center justify-between border-b border-border px-4 py-3 dark:border-border">
        <span className="text-xs font-semibold tracking-widest text-foreground-secondary">STRUCTURE</span>
        <button type="button" onClick={onAddLab} className="text-foreground-muted hover:text-primary" aria-label="Add lab">
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
              <div className={`flex items-center gap-1 rounded-lg px-2 py-2 ${isSelectedLab ? 'bg-primary-light0/10' : 'hover:bg-surface-secondary hover:bg-surface-secondary'}`}>
                <button type="button" onClick={() => onToggleLab(lab.id)} className="text-foreground-muted">
                  {labOpen ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                </button>
                <button type="button" onClick={() => onSelectLab(lab.id)} className="flex flex-1 items-center gap-2 text-left text-sm text-foreground">
                  <BookOpen className="h-4 w-4 shrink-0 text-primary" />
                  <span className="truncate">{lab.name}</span>
                </button>
                <button type="button" onClick={() => onDeleteLab(lab.id)} className="text-foreground-muted hover:text-error">
                  <Trash2 className="h-3.5 w-3.5" />
                </button>
              </div>
              {labOpen && isSelectedLab && labDraft && (
                <div className="ml-4 border-l border-border pl-2 dark:border-border">
                  {(labDraft.challenges || []).map((challenge) => {
                    const challengeOpen = expandedChallenges[challenge.id];
                    return (
                      <div key={challenge.id} className="mb-1">
                        <div className={`flex items-center gap-1 rounded-lg px-2 py-1.5 ${
                          selectedChallengeId === challenge.id && !selectedClassId
                            ? 'bg-chart-blue/15'
                            : 'hover:bg-surface-secondary hover:bg-surface-secondary'
                        }`}>
                          <button type="button" onClick={() => onToggleChallenge(challenge.id)} className="text-foreground-muted">
                            {challengeOpen ? <ChevronDown className="h-3.5 w-3.5" /> : <ChevronRight className="h-3.5 w-3.5" />}
                          </button>
                          <button
                            type="button"
                            onClick={() => onSelectChallenge(challenge.id)}
                            className="flex flex-1 items-center gap-2 text-left text-sm text-foreground-secondary min-w-0"
                          >
                            <Layers className="h-3.5 w-3.5 shrink-0 text-chart-blue" />
                            <input
                              type="text"
                              className="min-w-0 flex-1 truncate rounded border border-transparent bg-transparent px-1 py-0.5 text-sm text-foreground-secondary hover:border-border focus:border-primary focus:outline-none"
                              value={challenge.name}
                              onClick={(e) => e.stopPropagation()}
                              onChange={(e) => onRenameChallenge(challenge.id, e.target.value)}
                            />
                          </button>
                          <button type="button" onClick={() => onDeleteChallenge(challenge.id)} className="text-foreground-muted hover:text-error">
                            <Trash2 className="h-3.5 w-3.5" />
                          </button>
                        </div>
                        {challengeOpen && (
                          <div className="ml-4 space-y-1 border-l border-border pl-2 dark:border-border">
                            {(challenge.classes || []).map((cls) => (
                              <button
                                key={cls.id}
                                type="button"
                                onClick={() => onSelectClass(challenge.id, cls.id)}
                                className={`flex w-full items-center justify-between rounded-lg px-2 py-1.5 text-left text-sm ${
                                  selectedClassId === cls.id
                                    ? 'bg-chart-green/15 text-chart-green'
                                    : 'text-foreground-secondary hover:bg-surface-secondary'
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
                                  className="text-foreground-muted hover:text-error"
                                >
                                  <Trash2 className="h-3.5 w-3.5" />
                                </span>
                              </button>
                            ))}
                            <button
                              type="button"
                              onClick={() => onAddClass(challenge.id)}
                              className="flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-xs text-primary hover:bg-primary-hover/5"
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
                    className="mt-1 flex w-full items-center gap-2 rounded-lg px-2 py-1.5 text-xs text-primary hover:bg-primary-hover/5"
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
