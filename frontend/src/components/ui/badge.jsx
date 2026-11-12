import { cn } from './cn';

const VARIANT = {
  default: 'border-transparent bg-primary-light text-primary-text',
  navy: 'border-navy/20 bg-navy-light text-navy-text dark:border-navy-hover/60',
  teal: 'border-transparent bg-secondary-light text-secondary-text',
  gold: 'border-gold/30 bg-gold-bg text-gold-text',
  success: 'border-transparent bg-success-bg text-success-text',
  ai: 'border-accent-purple/30 bg-accent-purple-light text-accent-purple-text',
  secondary: 'border-transparent bg-surface-secondary text-foreground-secondary',
  outline: 'border-border bg-transparent text-foreground-secondary',
  warning: 'border-transparent bg-warning-bg text-warning-text',
  destructive: 'border-transparent bg-error-bg text-error-text',
  'deadline-3': 'border-warning/35 bg-warning-bg text-warning-text',
  'deadline-2': 'border-warning bg-warning text-white font-bold',
  'deadline-1': 'border-error bg-error text-white font-bold',
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
