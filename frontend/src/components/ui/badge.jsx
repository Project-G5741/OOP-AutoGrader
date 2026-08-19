import { cn } from './cn';

const VARIANT = {
  default: 'border-transparent bg-primary-light text-primary-text',
  secondary: 'border-transparent bg-surface-secondary text-foreground-secondary',
  outline: 'border-border bg-transparent text-foreground-secondary',
  warning: 'border-transparent bg-warning-bg text-warning-text',
  destructive: 'border-transparent bg-error-bg text-error-text',
};

export function Badge({ variant = 'default', className = '', children, ...props }) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide',
        VARIANT[variant] ?? VARIANT.default,
        className,
      )}
      {...props}
    >
      {children}
    </span>
  );
}
