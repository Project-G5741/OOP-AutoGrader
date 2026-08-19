import { cn } from './cn';

const VARIANT = {
  default: 'border-transparent bg-transparent hover:bg-surface-secondary',
  outline: 'border-border bg-surface hover:bg-surface-secondary',
  muted: 'border-transparent bg-surface-secondary hover:bg-surface-tertiary',
};

const SIZE = {
  default: 'gap-3 px-3 py-2.5',
  sm: 'gap-2.5 px-2.5 py-2',
  xs: 'gap-2 px-2 py-1.5',
};

export function ItemGroup({ className = '', ...props }) {
  return <div role="list" className={cn('flex flex-col gap-1', className)} {...props} />;
}

export function ItemSeparator({ className = '', ...props }) {
  return <div className={cn('my-1 h-px w-full bg-border', className)} {...props} />;
}

export function Item({
  as: Comp = 'div',
  variant = 'default',
  size = 'default',
  className = '',
  ...props
}) {
  return (
    <Comp
      data-slot="item"
      className={cn(
        'flex w-full items-center rounded-lg border text-left transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary',
        VARIANT[variant] ?? VARIANT.default,
        SIZE[size] ?? SIZE.default,
        className,
      )}
      {...props}
    />
  );
}

export function ItemMedia({ variant = 'default', className = '', ...props }) {
  return (
    <div
      data-slot="item-media"
      className={cn(
        'flex shrink-0 items-center justify-center text-foreground-muted',
        variant === 'icon' && 'h-8 w-8 rounded-md bg-surface-secondary',
        variant === 'image' && 'h-10 w-10 overflow-hidden rounded-md',
        className,
      )}
      {...props}
    />
  );
}

export function ItemContent({ className = '', ...props }) {
  return (
    <div
      data-slot="item-content"
      className={cn('flex min-w-0 flex-1 flex-col gap-0.5', className)}
      {...props}
    />
  );
}

export function ItemTitle({ className = '', ...props }) {
  return (
    <div
      data-slot="item-title"
      className={cn('truncate text-sm font-medium text-foreground', className)}
      {...props}
    />
  );
}

export function ItemDescription({ className = '', ...props }) {
  return (
    <p
      data-slot="item-description"
      className={cn('truncate text-xs text-foreground-muted', className)}
      {...props}
    />
  );
}

export function ItemActions({ className = '', ...props }) {
  return (
    <div
      data-slot="item-actions"
      className={cn('flex shrink-0 items-center gap-1.5', className)}
      {...props}
    />
  );
}
