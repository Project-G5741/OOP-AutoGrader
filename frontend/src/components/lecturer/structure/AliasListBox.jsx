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
            anchor="bottom start"
            transition
            className="z-[250] min-w-[var(--button-width)] max-w-[calc(100vw-2rem)] [--anchor-gap:6px] [--anchor-padding:8px] rounded-2xl border border-border bg-surface p-1.5 shadow-stage-3 ring-1 ring-border/80 backdrop-blur-xl bg-surface/98 transition duration-200 ease-out data-closed:scale-95 data-closed:opacity-0 data-[anchor~='bottom']:data-closed:-translate-y-1 data-[anchor~='top']:data-closed:translate-y-1 data-[anchor~='bottom']:origin-top data-[anchor~='top']:origin-bottom max-h-64 overflow-y-auto overscroll-contain outline-none space-y-0.5"
          >
            {aliases.map((alias, index) => {
              const label = alias == null || String(alias).trim() === '' ? '(empty)' : String(alias);
              return (
                <ListboxOption
                  key={`alias-${index}`}
                  value={index}
                  className="group relative flex cursor-pointer select-none items-center gap-3 rounded-xl px-3 py-2 text-sm text-foreground transition-all duration-150 outline-none data-focus:bg-primary-light/60 data-focus:text-primary-text data-selected:bg-primary/10 data-selected:font-semibold data-selected:text-primary data-focus:data-selected:bg-primary-light/80"
                >
                  <span className="min-w-0 flex-1 truncate">{label}</span>
                  {index === 0 && (
                    <span className="shrink-0 rounded-md bg-surface-secondary px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-foreground-muted group-data-selected:bg-primary/20 group-data-selected:text-primary">
                      primary
                    </span>
                  )}
                  <span
                    className="ml-auto flex h-4 w-4 shrink-0 items-center justify-center rounded-full text-primary transition-all duration-150 opacity-0 scale-75 group-data-selected:opacity-100 group-data-selected:scale-100"
                    aria-hidden
                  >
                    <Check className="h-4 w-4" strokeWidth={2.5} />
                  </span>
                </ListboxOption>
              );
            })}
            <ListboxOption
              value={ADD_VALUE}
              className="group flex cursor-pointer select-none items-center gap-2 rounded-xl border-t border-border-subtle/50 px-3 py-2 text-sm font-semibold text-primary transition-all duration-150 hover:bg-primary-light/60 data-focus:bg-primary-light/60 data-focus:text-primary-text"
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
