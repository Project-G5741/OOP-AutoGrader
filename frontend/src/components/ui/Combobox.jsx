import { useMemo, useState } from 'react';
import {
  Listbox,
  ListboxButton,
  ListboxOption,
  ListboxOptions,
} from '@headlessui/react';
import { Check, ChevronDown, Search, SearchX, X } from 'lucide-react';
import { cn } from './cn';

/**
 * Modern, rock-solid theme-adaptive select / combobox dropdown.
 * - Perfectly aligned with trigger button (zero offset, zero overlap).
 * - Instant CSS and keyboard hover feedback using semantic theme tokens.
 * - Clear selected indicator (trailing checkmark, bold label, tinted background).
 * - Full support for icons, descriptions, badges, and grouped options.
 * - Optional inline search bar inside the dropdown for searchable mode.
 */
export default function Combobox({
  value,
  onChange,
  options = [],
  placeholder = 'Select…',
  emptyLabel,
  allowEmpty = true,
  disabled = false,
  searchable = false,
  size = 'default',
  className = '',
  icon: Icon = null,
  title,
  'aria-label': ariaLabel,
}) {
  const [query, setQuery] = useState('');
  const emptyOptionLabel = emptyLabel ?? placeholder;
  const currentValue = value == null ? '' : String(value);

  const listOptions = useMemo(() => {
    const flattened = [];
    for (const item of options) {
      if (item && Array.isArray(item.items)) {
        flattened.push({
          isGroupHeader: true,
          label: item.group || item.label || '',
        });
        for (const child of item.items) {
          flattened.push(child);
        }
      } else {
        flattened.push(item);
      }
    }

    const mapped = flattened.map((opt) => {
      if (opt.isGroupHeader) {
        return opt;
      }
      if (typeof opt === 'string' || typeof opt === 'number') {
        return {
          value: String(opt),
          label: String(opt),
          isPlaceholder: false,
        };
      }
      const val = opt.value !== undefined ? opt.value : (opt.id !== undefined ? opt.id : opt);
      const lbl = opt.label !== undefined ? opt.label : String(val ?? '');
      return {
        value: String(val ?? ''),
        label: lbl,
        icon: opt.icon,
        description: opt.description,
        badge: opt.badge,
        disabled: Boolean(opt.disabled),
        isPlaceholder: false,
      };
    });

    if (allowEmpty) {
      return [{ value: '', label: emptyOptionLabel, isPlaceholder: true }, ...mapped];
    }
    return mapped;
  }, [allowEmpty, emptyOptionLabel, options]);

  const selected = useMemo(
    () => listOptions.find((opt) => !opt.isGroupHeader && opt.value === currentValue) ?? null,
    [listOptions, currentValue],
  );

  const filteredOptions = useMemo(() => {
    if (!searchable || query.trim() === '') {
      return listOptions;
    }
    const q = query.trim().toLowerCase();
    const result = [];
    let pendingHeader = null;
    let groupHasMatches = false;

    for (const opt of listOptions) {
      if (opt.isGroupHeader) {
        pendingHeader = opt;
        groupHasMatches = false;
        continue;
      }
      if (opt.isPlaceholder || opt.label.toLowerCase().includes(q)) {
        if (pendingHeader && !groupHasMatches) {
          result.push(pendingHeader);
          groupHasMatches = true;
        }
        result.push(opt);
      }
    }
    return result;
  }, [listOptions, query, searchable]);

  const isSm = size === 'sm';
  const minWidthOverride = /\bmin-w-/.test(className);
  const ActiveIcon = (selected && !selected.isPlaceholder && selected.icon) || Icon;

  const fieldShellClass = cn(
    'relative flex w-full items-center justify-between border border-border bg-surface text-foreground shadow-sm',
    'transition-all duration-150 ease-in-out cursor-pointer',
    isSm ? 'min-h-[32px] rounded-lg px-2.5 py-1 text-xs' : 'min-h-10 rounded-xl px-3 py-2 text-sm',
    'focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/25',
    'data-open:border-primary data-open:ring-2 data-open:ring-primary/25',
    !disabled && 'hover:border-border-subtle hover:bg-surface-secondary/40',
    disabled && 'cursor-not-allowed opacity-60 bg-surface-secondary',
  );

  const chevronClass = cn(
    'shrink-0 text-foreground-muted transition-transform duration-200 group-data-open:rotate-180 group-data-open:text-primary ml-2',
    isSm ? 'h-3.5 w-3.5' : 'h-4 w-4',
  );

  return (
    <Listbox
      value={currentValue}
      disabled={disabled}
      onChange={(nextVal) => {
        onChange?.(nextVal);
        setQuery('');
      }}
    >
      <div
        className={cn(
          'relative w-full',
          !minWidthOverride && (isSm ? 'min-w-[7rem]' : 'min-w-[12rem]'),
          className,
        )}
      >
        <ListboxButton
          aria-label={ariaLabel}
          title={title}
          className={cn(
            'group cursor-pointer text-left max-w-full',
            fieldShellClass,
            disabled && 'pointer-events-none',
          )}
        >
          <div className="flex min-w-0 flex-1 items-center gap-2">
            {ActiveIcon && (
              <ActiveIcon
                className={cn(
                  'shrink-0 transition-colors duration-150',
                  isSm ? 'h-3.5 w-3.5' : 'h-4 w-4',
                  selected && !selected.isPlaceholder ? 'text-primary' : 'text-foreground-muted',
                )}
                strokeWidth={2.2}
              />
            )}
            <span
              className={cn(
                'truncate text-left select-none',
                isSm ? 'text-xs' : 'text-sm',
                selected && !selected.isPlaceholder
                  ? 'font-semibold text-foreground'
                  : 'font-normal text-foreground-muted',
              )}
            >
              {selected && !selected.isPlaceholder ? selected.label : placeholder}
            </span>
          </div>
          <ChevronDown className={chevronClass} aria-hidden />
        </ListboxButton>

        <ListboxOptions
          anchor={{ to: 'bottom start', gap: 6 }}
          transition
          className={cn(
            'z-[250] w-[var(--button-width)] min-w-[var(--button-width)] max-w-[calc(100vw-2rem)]',
            'rounded-2xl border border-border bg-surface p-1.5 shadow-stage-3 ring-1 ring-border/80',
            'backdrop-blur-xl bg-surface/98',
            'transition duration-150 ease-out',
            'data-closed:scale-95 data-closed:opacity-0',
            'data-[anchor~="bottom"]:data-closed:-translate-y-1',
            'data-[anchor~="top"]:data-closed:translate-y-1',
            'data-[anchor~="bottom"]:origin-top data-[anchor~="top"]:origin-bottom',
            'max-h-72 overflow-y-auto overscroll-contain outline-none',
            'space-y-0.5',
            'empty:invisible',
          )}
        >
          {searchable && (
            <div className="sticky top-0 z-20 -mx-1.5 -mt-1.5 mb-1 bg-surface p-2 pb-1.5 border-b border-border/70 rounded-t-2xl">
              <div className="relative flex items-center">
                <Search className="pointer-events-none absolute left-2.5 h-3.5 w-3.5 text-foreground-muted" />
                <input
                  type="search"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Search…"
                  className="h-8 w-full rounded-lg border border-border bg-surface-secondary pl-8 pr-7 text-xs text-foreground placeholder:text-foreground-muted focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                  onClick={(e) => e.stopPropagation()}
                  onKeyDown={(e) => e.stopPropagation()}
                />
                {query && (
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      setQuery('');
                    }}
                    className="absolute right-2 p-0.5 text-foreground-muted hover:text-foreground"
                    aria-label="Clear search"
                  >
                    <X className="h-3 w-3" />
                  </button>
                )}
              </div>
            </div>
          )}

          {filteredOptions.length === 0 ? (
            <div className="py-6 px-4 text-center select-none">
              <SearchX className="mx-auto h-6 w-6 text-foreground-muted/60 mb-2" strokeWidth={1.75} />
              <p className="text-sm font-semibold text-foreground">No matches found</p>
              <p className="text-xs text-foreground-muted mt-0.5">
                No options match &ldquo;{query}&rdquo;
              </p>
            </div>
          ) : (
            filteredOptions.map((opt, idx) => {
              if (opt.isGroupHeader) {
                return (
                  <div
                    key={`group-${opt.label}-${idx}`}
                    className={cn(
                      'px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-foreground-muted select-none',
                      'sticky top-0 bg-surface/95 backdrop-blur-sm z-10',
                      idx > 0 && 'mt-1.5 pt-1.5 border-t border-border-subtle/50',
                    )}
                  >
                    {opt.label}
                  </div>
                );
              }

              const isOptionSelected = opt.value === currentValue && !opt.isPlaceholder;

              return (
                <ListboxOption
                  key={`${opt.value}-${opt.label}-${idx}`}
                  value={opt.value}
                  disabled={opt.disabled}
                  className={({ active, selected: optSelected, focus }) =>
                    cn(
                      'group relative flex cursor-pointer select-none items-center gap-2.5 transition-colors duration-150 outline-none',
                      isSm ? 'rounded-lg px-2.5 py-1.5 text-xs' : 'rounded-xl px-3 py-2 text-sm',
                      // Base text
                      opt.isPlaceholder ? 'italic text-foreground-muted' : 'text-foreground',
                      // Disabled
                      opt.disabled && 'pointer-events-none opacity-40',
                      // Selected state (distinct tint & primary text)
                      (optSelected || isOptionSelected) && 'bg-primary-light/50 font-semibold text-primary',
                      // Hover / Focus state: immediate strong highlight
                      (active || focus) && 'bg-primary-light text-primary-text',
                      // Native CSS hover
                      'hover:bg-primary-light hover:text-primary-text',
                    )
                  }
                >
                  {({ selected: optSelected, focus, active }) => {
                    const isSelected = (optSelected || isOptionSelected) && !opt.isPlaceholder;
                    const isHighlighted = active || focus;

                    return (
                      <>
                        {/* Left Icon (if provided) */}
                        {opt.icon && (
                          <opt.icon
                            className={cn(
                              'shrink-0 transition-colors duration-150',
                              isSm ? 'h-3.5 w-3.5' : 'h-4 w-4',
                              isSelected
                                ? 'text-primary'
                                : isHighlighted
                                  ? 'text-primary-text'
                                  : 'text-foreground-muted group-hover:text-primary-text',
                            )}
                            strokeWidth={2.2}
                            aria-hidden
                          />
                        )}

                        {/* Option Label + Description + Badge */}
                        <div className="min-w-0 flex-1">
                          <div className="flex items-center gap-2">
                            <span className="truncate">{opt.label}</span>
                            {opt.badge && (
                              <span
                                className={cn(
                                  'shrink-0 rounded-md px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wider',
                                  isSelected
                                    ? 'bg-primary/20 text-primary'
                                    : 'bg-surface-secondary text-foreground-muted',
                                )}
                              >
                                {opt.badge}
                              </span>
                            )}
                          </div>
                          {opt.description && (
                            <span
                              className={cn(
                                'block truncate text-xs font-normal mt-0.5',
                                isSelected ? 'text-primary/80' : 'text-foreground-muted',
                              )}
                            >
                              {opt.description}
                            </span>
                          )}
                        </div>

                        {/* Trailing checkmark */}
                        {!opt.isPlaceholder && (
                          <span
                            className={cn(
                              'ml-auto flex shrink-0 items-center justify-center rounded-full text-primary transition-all duration-150',
                              isSm ? 'h-4 w-4' : 'h-5 w-5',
                              isSelected ? 'opacity-100 scale-100' : 'opacity-0 scale-75',
                            )}
                            aria-hidden
                          >
                            <Check className={cn(isSm ? 'h-3.5 w-3.5' : 'h-4 w-4')} strokeWidth={2.5} />
                          </span>
                        )}
                      </>
                    );
                  }}
                </ListboxOption>
              );
            })
          )}
        </ListboxOptions>
      </div>
    </Listbox>
  );
}
