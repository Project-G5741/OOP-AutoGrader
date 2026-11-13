import React from 'react';
import { ArrowDown, ArrowUp, Plus, Trash2 } from 'lucide-react';

function ensureAliases(values, fallback = '') {
  return Array.isArray(values) && values.length > 0 ? values : [fallback ?? ''];
}

export default function AliasListBox({
  values,
  fallback = '',
  onChange,
  addLabel,
  ariaLabel,
  placeholder,
  className = '',
}) {
  const [expanded, setExpanded] = React.useState(false);
  const containerRef = React.useRef(null);
  const nextInputRef = React.useRef(null);
  const aliases = ensureAliases(values, fallback);

  const updateAlias = (index, value) => {
    onChange(aliases.map((alias, aliasIndex) => (aliasIndex === index ? value : alias)));
  };

  const moveAlias = (index, direction) => {
    const target = index + direction;
    if (target < 0 || target >= aliases.length) return;
    const next = [...aliases];
    [next[index], next[target]] = [next[target], next[index]];
    onChange(next);
  };

  const removeAlias = (index) => {
    if (aliases.length <= 1) return;
    onChange(aliases.filter((_, aliasIndex) => aliasIndex !== index));
  };

  const addAlias = () => {
    onChange([...aliases, '']);
    nextInputRef.current = aliases.length;
    requestAnimationFrame(() => {
      containerRef.current?.querySelector(`[data-alias-index="${nextInputRef.current}"]`)?.focus();
      nextInputRef.current = null;
    });
  };

  return (
    <div
      ref={containerRef}
      className={`min-w-0 rounded-lg border border-border bg-surface-secondary focus-within:ring-2 focus-within:ring-primary/40 ${className}`}
      onFocus={() => setExpanded(true)}
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) setExpanded(false);
      }}
    >
      {aliases.map((alias, index) => {
        if (!expanded && index > 0) return null;
        const primary = index === 0;
        return (
          <div
            key={index}
            className={`flex min-w-0 items-center gap-1 px-2 ${index > 0 ? 'border-t border-border' : ''}`}
          >
            <input
              data-alias-index={index}
              className="min-w-0 flex-1 bg-transparent px-1 py-2 text-sm text-foreground outline-none"
              value={alias}
              placeholder={primary ? placeholder : `${placeholder} alias`}
              aria-label={`${ariaLabel}${primary ? ' (primary)' : ` alias ${index + 1}`}`}
              onChange={(event) => updateAlias(index, event.target.value)}
            />
            {expanded && (
              <div className="flex shrink-0 items-center">
                <button
                  type="button"
                  className="rounded p-1 text-foreground-muted hover:bg-surface hover:text-foreground disabled:cursor-not-allowed disabled:opacity-30"
                  disabled={index === 0}
                  onClick={() => moveAlias(index, -1)}
                  aria-label={`Move ${ariaLabel} alias up`}
                  title="Move up"
                >
                  <ArrowUp className="h-3.5 w-3.5" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  className="rounded p-1 text-foreground-muted hover:bg-surface hover:text-foreground disabled:cursor-not-allowed disabled:opacity-30"
                  disabled={index === aliases.length - 1}
                  onClick={() => moveAlias(index, 1)}
                  aria-label={`Move ${ariaLabel} alias down`}
                  title="Move down"
                >
                  <ArrowDown className="h-3.5 w-3.5" aria-hidden="true" />
                </button>
                <button
                  type="button"
                  className="rounded p-1 text-foreground-muted hover:bg-error-bg hover:text-error disabled:cursor-not-allowed disabled:opacity-30"
                  disabled={aliases.length <= 1}
                  onClick={() => removeAlias(index)}
                  aria-label={`Remove ${ariaLabel} alias`}
                  title={aliases.length <= 1 ? 'At least one entry is required' : 'Remove'}
                >
                  <Trash2 className="h-3.5 w-3.5" aria-hidden="true" />
                </button>
              </div>
            )}
          </div>
        );
      })}
      {expanded && (
        <button
          type="button"
          onClick={addAlias}
          className="flex w-full items-center gap-1 border-t border-border px-3 py-2 text-left text-sm text-primary hover:bg-surface"
        >
          <Plus className="h-3.5 w-3.5" aria-hidden="true" />
          {addLabel}
        </button>
      )}
      {!expanded && aliases.length > 1 && (
        <div className="border-t border-border px-3 py-1 text-xs text-foreground-muted">
          +{aliases.length - 1} {aliases.length === 2 ? 'alias' : 'aliases'}
        </div>
      )}
    </div>
  );
}
