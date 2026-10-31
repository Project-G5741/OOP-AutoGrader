import { useMemo, useState } from 'react';
import {
  Combobox as HeadlessCombobox,
  ComboboxButton,
  ComboboxInput,
  ComboboxOption,
  ComboboxOptions,
} from '@headlessui/react';
import { Check, ChevronDown } from 'lucide-react';
import { cn } from './cn';

/**
 * Headless UI combobox layout: input + chevron trigger, anchored options list with check mark.
 * Non-searchable: entire field is one button so click anywhere opens the list.
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

  const listOptions = useMemo(() => {
    const mapped = options.map((opt) => ({
      value: opt.value,
      label: opt.label,
      isPlaceholder: false,
    }));
    if (allowEmpty) {
      return [{ value: '', label: emptyOptionLabel, isPlaceholder: true }, ...mapped];
    }
    return mapped;
  }, [allowEmpty, emptyOptionLabel, options]);

  const selected = useMemo(
    () => listOptions.find((opt) => String(opt.value) === String(value ?? '')) ?? null,
    [listOptions, value],
  );

  const filteredOptions = useMemo(() => {
    if (!searchable || query.trim() === '') {
      return listOptions;
    }
    const q = query.trim().toLowerCase();
    return listOptions.filter(
      (opt) => opt.isPlaceholder || opt.label.toLowerCase().includes(q),
    );
  }, [listOptions, query, searchable]);

  const isSm = size === 'sm';
  const minWidthOverride = /\bmin-w-/.test(className);

  const fieldShellClass = cn(
    'relative flex w-full items-center rounded-lg border border-border bg-surface-secondary text-foreground shadow-sm',
    'transition-colors duration-150',
    'focus:outline-none focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/30',
    'data-focus:border-primary data-focus:ring-2 data-focus:ring-primary/30',
    !disabled && 'hover:border-primary/40 hover:bg-primary-light',
    disabled && 'cursor-not-allowed opacity-60',
  );

  const inputClass = cn(
    'min-w-0 flex-1 truncate border-0 bg-transparent py-2.5 text-sm font-medium text-foreground',
    'placeholder:font-normal placeholder:text-foreground-muted focus:outline-none focus:ring-0',
    Icon ? 'pl-10' : 'pl-3',
    'pr-2',
    isSm && 'py-1.5 text-xs uppercase tracking-wide',
    !searchable && 'cursor-pointer',
  );

  const chevronClass = cn(
    'shrink-0 text-foreground-muted transition-colors group-data-open:text-primary',
    isSm ? 'mr-2 h-3.5 w-3.5' : 'mr-2.5 h-4 w-4',
  );

  return (
    <HeadlessCombobox
      value={selected}
      disabled={disabled}
      onChange={(option) => {
        if (option == null) return;
        onChange?.(option.value);
        setQuery('');
      }}
      onClose={() => setQuery('')}
    >
      <div
        className={cn(
          'relative w-full',
          !minWidthOverride && (isSm ? 'min-w-[7rem]' : 'min-w-[14rem]'),
          className,
        )}
      >
        {searchable ? (
          <div className={fieldShellClass}>
            {Icon && (
              <span
                className={cn(
                  'pointer-events-none absolute left-2.5 z-[1] flex items-center justify-center rounded-md p-1',
                  'text-foreground-muted',
                  selected && !selected.isPlaceholder && 'text-primary',
                )}
                aria-hidden
              >
                <Icon className={cn(isSm ? 'h-3.5 w-3.5' : 'h-4 w-4')} strokeWidth={2.25} />
              </span>
            )}
            <ComboboxInput
              aria-label={ariaLabel}
              placeholder={placeholder}
              displayValue={(option) => (option?.isPlaceholder ? '' : option?.label) ?? ''}
              onChange={(event) => setQuery(event.target.value)}
              className={inputClass}
            />
            <ComboboxButton
              className={cn(
                'group flex shrink-0 items-center self-stretch px-1',
                disabled && 'pointer-events-none',
              )}
            >
              <ChevronDown className={cn(chevronClass, 'transition-transform group-data-open:rotate-180')} aria-hidden />
            </ComboboxButton>
          </div>
        ) : (
          <ComboboxButton
            aria-label={ariaLabel}
            title={title}
            className={cn(
              'group cursor-pointer text-left max-w-full overflow-hidden',
              fieldShellClass,
              disabled && 'pointer-events-none',
            )}
          >
            {Icon && (
              <span
                className={cn(
                  'pointer-events-none absolute left-2.5 z-[1] flex items-center justify-center rounded-md p-1',
                  'text-foreground-muted',
                  selected && !selected.isPlaceholder && 'text-primary',
                )}
                aria-hidden
              >
                <Icon className={cn(isSm ? 'h-3.5 w-3.5' : 'h-4 w-4')} strokeWidth={2.25} />
              </span>
            )}
            <ComboboxInput
              readOnly
              tabIndex={-1}
              placeholder={placeholder}
              displayValue={(option) => (option?.isPlaceholder ? '' : option?.label) ?? ''}
              className={cn(inputClass, 'pointer-events-none')}
            />
            <ChevronDown
              className={cn(chevronClass, 'transition-transform group-data-open:rotate-180')}
              aria-hidden
            />
          </ComboboxButton>
        )}

        <ComboboxOptions
          anchor="bottom"
          transition
          className={cn(
            'z-[200] w-[var(--input-width)] rounded-xl border border-border bg-surface p-1 shadow-xl',
            '[--anchor-gap:0.25rem] empty:invisible',
            'transition duration-100 ease-out data-closed:opacity-0',
            'max-h-64 overflow-auto outline-none ring-1 ring-border/80',
          )}
        >
          {filteredOptions.map((opt) => (
            <ComboboxOption
              key={`${String(opt.value)}-${opt.label}`}
              value={opt}
              className={cn(
                'group flex cursor-pointer select-none items-center gap-2 rounded-lg px-3 py-2',
                'text-sm text-foreground',
                'hover:bg-primary-light data-focus:bg-primary-light',
                opt.isPlaceholder && 'italic text-foreground-muted',
              )}
            >
              <Check
                className={cn(
                  'h-4 w-4 shrink-0 text-primary opacity-0',
                  'group-data-selected:opacity-100',
                  opt.isPlaceholder && 'invisible',
                )}
                strokeWidth={2.5}
                aria-hidden
              />
              <span className="min-w-0 flex-1 truncate">{opt.label}</span>
            </ComboboxOption>
          ))}
        </ComboboxOptions>
      </div>
    </HeadlessCombobox>
  );
}
