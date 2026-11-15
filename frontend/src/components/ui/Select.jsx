import Combobox from './Combobox';

/**
 * Labeled Combobox wrapper with card layout for form views.
 */
export default function Select({
  label,
  value,
  onChange,
  options = [],
  placeholder = 'Select…',
  disabled = false,
  className = '',
  ...props
}) {
  return (
    <div className={`space-y-1.5 ${className}`}>
      {label && (
        <label className="block text-xs font-semibold text-foreground-muted uppercase tracking-wider">
          {label}
        </label>
      )}
      <Combobox
        value={value}
        onChange={onChange}
        options={options}
        placeholder={placeholder}
        disabled={disabled}
        allowEmpty={false}
        className="w-full min-w-0"
        {...props}
      />
    </div>
  );
}
