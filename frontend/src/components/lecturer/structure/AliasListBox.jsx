import React from 'react';
import { Listbox, ListboxButton, ListboxOption, ListboxOptions } from '@headlessui/react';
import { Check, ChevronDown, Plus, Trash2 } from 'lucide-react';

function ensureAliases(values, fallback = '') {
  return Array.isArray(values) && values.length > 0 ? values : [fallback];
}

const ADD_VALUE = '__add__';

export default function AliasListBox({
  values,
  fallback = '',
  onChange,
  addLabel,
  ariaLabel,
  placeholder,
  className = '',
}) {
  const inputRef = React.useRef(null);
  const aliases = ensureAliases(values, fallback);
  const [selectedIndex, setSelectedIndex] = React.useState(0);
  const safeIndex = Math.min(selectedIndex, Math.max(0, aliases.length - 1));
  const selectedValue = aliases[safeIndex] ?? '';

  React.useEffect(() => {
    if (selectedIndex > aliases.length - 1) {
      setSelectedIndex(Math.max(0, aliases.length - 1));
    }
  }, [aliases.length, selectedIndex]);

  const updateSelected = (value) => {
    onChange(aliases.map((alias, index) => (index === safeIndex ? value : alias)));
  };

  const selectAlias = (index) => {
    setSelectedIndex(index);
    requestAnimationFrame(() => inputRef.current?.focus());
  };

  const addAlias = () => {
    const nextIndex = aliases.length;
    onChange([...aliases, '']);
    setSelectedIndex(nextIndex);
    requestAnimationFrame(() => inputRef.current?.focus());
  };

  const makePrimary = () => {
    if (safeIndex <= 0) return;
    const next = [...aliases];
    const [picked] = next.splice(safeIndex, 1);
    next.unshift(picked);
    onChange(next);
    setSelectedIndex(0);
  };

  const removeSelected = () => {
    if (aliases.length <= 1) return;
    onChange(aliases.filter((_, index) => index !== safeIndex));
    setSelectedIndex(Math.max(0, safeIndex - 1));
  };

  return (
    <div className={`flex min-w-0 items-center gap-1 ${className}`}>
      <Listbox
        value={safeIndex}
        onChange={(next) => {
          if (next === ADD_VALUE) {
            addAlias();
            return;
          }
          selectAlias(next);
        }}
      >
        <div className="relative min-w-0 flex-1">
          <div className="relative flex w-full items-center rounded-lg border border-border bg-surface-secondary text-foreground shadow-sm transition-colors focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/30 hover:border-primary/40">
            <input
              ref={inputRef}
              aria-label={ariaLabel}
              placeholder={placeholder}
              value={selectedValue == null ? '' : String(selectedValue)}
              onChange={(event) => updateSelected(event.target.value)}
              className="min-w-0 flex-1 truncate border-0 bg-transparent py-2.5 pl-3 pr-2 text-sm text-foreground placeholder:text-foreground-muted focus:outline-none focus:ring-0"
            />
            <ListboxButton
              className="group flex shrink-0 items-center self-stretch px-1.5 text-foreground-muted"
              aria-label={`Open ${ariaLabel} aliases`}
            >
              <ChevronDown
                className="h-4 w-4 transition-transform group-data-open:rotate-180 group-data-open:text-primary"
                aria-hidden
              />
            </ListboxButton>
          </div>

          <ListboxOptions
            transition
            className="absolute left-0 right-0 z-[200] mt-1 max-h-64 overflow-auto rounded-xl border border-border bg-surface p-1 shadow-xl outline-none ring-1 ring-border/80 transition duration-100 ease-out data-closed:opacity-0"
          >
            {aliases.map((alias, index) => {
              const label = alias == null || String(alias).trim() === '' ? '(empty)' : String(alias);
              return (
                <ListboxOption
                  key={`alias-${index}`}
                  value={index}
                  className="group flex cursor-pointer select-none items-center gap-2 rounded-lg px-3 py-2 text-sm text-foreground hover:bg-primary-light data-focus:bg-primary-light data-selected:bg-primary-light"
                >
                  <Check
                    className="h-4 w-4 shrink-0 text-primary opacity-0 group-data-selected:opacity-100"
                    strokeWidth={2.5}
                    aria-hidden
                  />
                  <span className="min-w-0 flex-1 truncate">{label}</span>
                  {index === 0 && (
                    <span className="shrink-0 text-[10px] font-medium uppercase tracking-wide text-foreground-muted">
                      primary
                    </span>
                  )}
                </ListboxOption>
              );
            })}
            <ListboxOption
              value={ADD_VALUE}
              className="group flex cursor-pointer select-none items-center gap-2 rounded-lg border-t border-border px-3 py-2 text-sm font-medium text-primary-text hover:bg-primary-light data-focus:bg-primary-light"
            >
              <Plus className="h-4 w-4 shrink-0" aria-hidden />
              <span>{addLabel || 'Add alias'}</span>
            </ListboxOption>
          </ListboxOptions>
        </div>
      </Listbox>

      {safeIndex > 0 && (
        <button
          type="button"
          onClick={makePrimary}
          className="shrink-0 rounded-lg border border-border px-2 py-1.5 text-[10px] font-medium uppercase tracking-wide text-foreground-muted hover:bg-surface-secondary hover:text-foreground"
          title="Make primary"
        >
          Primary
        </button>
      )}
      {aliases.length > 1 && (
        <button
          type="button"
          onClick={removeSelected}
          className="shrink-0 rounded p-1.5 text-foreground-muted hover:bg-error-bg hover:text-error"
          aria-label={`Remove ${ariaLabel}`}
          title="Remove"
        >
          <Trash2 className="h-4 w-4" aria-hidden />
        </button>
      )}
    </div>
  );
}
