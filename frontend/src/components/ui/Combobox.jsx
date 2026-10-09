import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { Check, ChevronDown } from 'lucide-react';
import { cn } from './cn';

/**
 * Accessible custom select with themed trigger + list (replaces native <select> where styling matters).
 */
export default function Combobox({
  value,
  onChange,
  options = [],
  placeholder = 'Select…',
  emptyLabel,
  allowEmpty = true,
  disabled = false,
  className = '',
  icon: Icon = null,
  'aria-label': ariaLabel,
}) {
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const rootRef = useRef(null);
  const listId = useId();
  const emptyOptionLabel = emptyLabel ?? placeholder;

  const listOptions = useMemo(
    () => (allowEmpty
      ? [{ value: '', label: emptyOptionLabel, isPlaceholder: true }, ...options]
      : options),
    [allowEmpty, emptyOptionLabel, options],
  );

  const selected = options.find((opt) => String(opt.value) === String(value));
  const displayLabel = selected?.label ?? placeholder;
  const hasValue = value !== '' && value != null && selected;

  useEffect(() => {
    const onPointerDown = (event) => {
      if (rootRef.current && !rootRef.current.contains(event.target)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', onPointerDown);
    return () => document.removeEventListener('mousedown', onPointerDown);
  }, []);

  useEffect(() => {
    if (!open) {
      setActiveIndex(-1);
      return;
    }
    const idx = listOptions.findIndex((opt) => String(opt.value) === String(value));
    setActiveIndex(idx >= 0 ? idx : 0);
  }, [open, value, listOptions]);

  const commit = (nextValue) => {
    onChange?.(nextValue);
    setOpen(false);
  };

  const onTriggerKeyDown = (event) => {
    if (disabled) return;
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      if (!open) {
        setOpen(true);
        return;
      }
      const delta = event.key === 'ArrowDown' ? 1 : -1;
      setActiveIndex((i) => {
        const next = i + delta;
        if (next < 0) return listOptions.length - 1;
        if (next >= listOptions.length) return 0;
        return next;
      });
    } else if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      if (open && activeIndex >= 0) {
        commit(listOptions[activeIndex].value);
      } else {
        setOpen(true);
      }
    } else if (event.key === 'Escape') {
      setOpen(false);
    }
  };

  return (
    <div ref={rootRef} className={cn('relative min-w-[14rem]', className)}>
      <button
        type="button"
        disabled={disabled}
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        onClick={() => !disabled && setOpen((o) => !o)}
        onKeyDown={onTriggerKeyDown}
        className={cn(
          'group flex w-full items-center gap-2.5 rounded-xl border px-3 py-2.5 text-left text-sm transition-all duration-200',
          'border-border bg-surface-secondary text-foreground',
          'shadow-sm dark:shadow-none',
          'hover:border-primary/45 hover:ring-2 hover:ring-primary/15',
          'focus-visible:outline-none focus-visible:border-primary focus-visible:ring-2 focus-visible:ring-primary/35',
          open && 'border-primary ring-2 ring-primary/30 bg-primary-light/30 dark:bg-primary/10',
          hasValue && !open && 'border-primary/25',
          disabled && 'cursor-not-allowed opacity-60',
        )}
      >
        {Icon && (
          <span
            className={cn(
              'flex shrink-0 items-center justify-center rounded-lg p-1.5 transition-colors',
              hasValue || open
                ? 'bg-primary/15 text-primary'
                : 'bg-surface-tertiary/80 text-foreground-muted group-hover:text-primary',
            )}
            aria-hidden
          >
            <Icon className="h-4 w-4" strokeWidth={2.25} />
          </span>
        )}
        <span
          className={cn(
            'min-w-0 flex-1 truncate font-medium',
            !hasValue && 'font-normal text-foreground-muted',
          )}
        >
          {displayLabel}
        </span>
        <ChevronDown
          className={cn(
            'h-4 w-4 shrink-0 text-foreground-muted transition-transform duration-200',
            open && 'rotate-180 text-primary',
          )}
          aria-hidden
        />
      </button>

      {open && (
        <ul
          id={listId}
          role="listbox"
          aria-label={ariaLabel}
          className={cn(
            'absolute z-50 mt-2 max-h-64 w-full overflow-auto rounded-xl border border-border py-1.5',
            'bg-surface/95 backdrop-blur-md',
            'shadow-lg dark:shadow-none dark:ring-1 dark:ring-surface-tertiary',
          )}
        >
          {listOptions.map((opt, index) => {
            const isSelected = String(opt.value) === String(value);
            const isActive = index === activeIndex;
            const isPlaceholder = opt.isPlaceholder || opt.value === '';
            return (
              <li
                key={`${opt.value}-${index}`}
                role="option"
                aria-selected={isSelected}
                onMouseEnter={() => setActiveIndex(index)}
                onClick={() => commit(opt.value)}
                className={cn(
                  'mx-1.5 flex cursor-pointer items-center gap-2 rounded-lg px-2.5 py-2 text-sm transition-colors',
                  isPlaceholder && 'text-foreground-muted italic',
                  !isPlaceholder && 'text-foreground',
                  isActive && 'bg-primary/12 text-primary',
                  isSelected && !isPlaceholder && 'bg-primary/15 font-medium text-primary',
                  !isActive && !isSelected && 'hover:bg-primary/8',
                )}
              >
                <span className="flex h-4 w-4 shrink-0 items-center justify-center">
                  {isSelected && !isPlaceholder && (
                    <Check className="h-3.5 w-3.5 text-primary" strokeWidth={2.5} aria-hidden />
                  )}
                </span>
                <span className="min-w-0 flex-1 truncate">{opt.label}</span>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
