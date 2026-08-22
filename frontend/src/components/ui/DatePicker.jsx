import { useEffect, useRef } from 'react';
import flatpickr from 'flatpickr';
import 'flatpickr/dist/flatpickr.min.css';
import { cn } from './cn';
import './datepicker.css';

const INPUT_CLASSES =
  'rounded-lg border border-border bg-surface px-3 py-2 text-sm text-foreground placeholder:text-foreground-muted dark:bg-surface';

export default function DatePicker({
  value = '',
  onChange,
  disabled = false,
  placeholder = 'Select Date...',
  className = '',
}) {
  const inputRef = useRef(null);
  const pickerRef = useRef(null);
  const onChangeRef = useRef(onChange);

  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  useEffect(() => {
    const input = inputRef.current;
    if (!input) return undefined;

    pickerRef.current = flatpickr(input, {
      dateFormat: 'Y-m-d',
      altInput: true,
      altFormat: 'd.m.Y',
      allowInput: false,
      disableMobile: true,
      clickOpens: !disabled,
      onChange(_dates, dateStr) {
        onChangeRef.current?.(dateStr || '');
      },
    });

    return () => {
      pickerRef.current?.destroy();
      pickerRef.current = null;
    };
  }, []);

  useEffect(() => {
    const picker = pickerRef.current;
    if (!picker) return;
    if (value) picker.setDate(value, false, 'Y-m-d');
    else picker.clear(false);
  }, [value]);

  useEffect(() => {
    const picker = pickerRef.current;
    if (!picker) return;
    picker.set('clickOpens', !disabled);
    if (picker.altInput) picker.altInput.disabled = disabled;
    inputRef.current.disabled = disabled;
  }, [disabled]);

  return (
    <span className={cn('inline-block', className.includes('w-full') && 'w-full')}>
      <input
        ref={inputRef}
        type="text"
        className={cn(INPUT_CLASSES, className)}
        placeholder={placeholder}
        disabled={disabled}
        readOnly
      />
    </span>
  );
}
