import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { PanelLeft } from 'lucide-react';
import { cn } from './cn';

const SIDEBAR_WIDTH = '18rem';
const SIDEBAR_WIDTH_MOBILE = '18rem';
const SIDEBAR_KEYBOARD_SHORTCUT = 'b';

const SidebarContext = createContext(null);

function useIsMobile() {
  const [isMobile, setIsMobile] = useState(false);

  useEffect(() => {
    const media = window.matchMedia('(max-width: 767px)');
    const update = () => setIsMobile(media.matches);
    update();
    media.addEventListener('change', update);
    return () => media.removeEventListener('change', update);
  }, []);

  return isMobile;
}

export function useSidebar() {
  const context = useContext(SidebarContext);
  if (!context) {
    throw new Error('useSidebar must be used within a SidebarProvider.');
  }
  return context;
}

export function SidebarProvider({
  defaultOpen = true,
  open: openProp,
  onOpenChange,
  className = '',
  style,
  children,
  ...props
}) {
  const isMobile = useIsMobile();
  const [openMobile, setOpenMobile] = useState(false);
  const [_open, _setOpen] = useState(defaultOpen);
  const open = openProp ?? _open;

  const setOpen = useCallback(
    (value) => {
      const next = typeof value === 'function' ? value(open) : value;
      if (onOpenChange) onOpenChange(next);
      else _setOpen(next);
    },
    [onOpenChange, open],
  );

  const toggleSidebar = useCallback(() => {
    if (isMobile) setOpenMobile((current) => !current);
    else setOpen((current) => !current);
  }, [isMobile, setOpen]);

  useEffect(() => {
    function handleKeyDown(event) {
      if (event.key === SIDEBAR_KEYBOARD_SHORTCUT && (event.metaKey || event.ctrlKey)) {
        event.preventDefault();
        toggleSidebar();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [toggleSidebar]);

  const state = open ? 'expanded' : 'collapsed';
  const value = useMemo(
    () => ({ state, open, setOpen, isMobile, openMobile, setOpenMobile, toggleSidebar }),
    [state, open, setOpen, isMobile, openMobile, toggleSidebar],
  );

  return (
    <SidebarContext.Provider value={value}>
      <div
        data-slot="sidebar-wrapper"
        className={cn('group/sidebar-wrapper flex min-h-[calc(100svh-10rem)] w-full', className)}
        style={{
          '--sidebar-width': SIDEBAR_WIDTH,
          '--sidebar-width-mobile': SIDEBAR_WIDTH_MOBILE,
          ...style,
        }}
        {...props}
      >
        {children}
      </div>
    </SidebarContext.Provider>
  );
}

export function Sidebar({
  side = 'left',
  variant = 'sidebar',
  collapsible = 'offcanvas',
  className = '',
  children,
  ...props
}) {
  const { isMobile, state, openMobile, setOpenMobile } = useSidebar();

  if (collapsible === 'none') {
    return (
      <aside
        data-slot="sidebar"
        className={cn(
          'flex h-full w-[var(--sidebar-width)] flex-col border-r border-border bg-surface text-foreground',
          className,
        )}
        {...props}
      >
        {children}
      </aside>
    );
  }

  if (isMobile) {
    return (
      <>
        {openMobile && (
          <button
            type="button"
            aria-label="Close lab list"
            className="fixed inset-0 z-40 bg-background/60 md:hidden"
            onClick={() => setOpenMobile(false)}
          />
        )}
        <aside
          data-slot="sidebar"
          data-mobile="true"
          data-side={side}
          className={cn(
            'fixed inset-y-0 z-50 flex w-[var(--sidebar-width-mobile)] flex-col border-border bg-surface text-foreground shadow-lg transition-transform duration-200 ease-linear md:hidden',
            side === 'left' ? 'left-0 border-r' : 'right-0 border-l',
            openMobile ? 'translate-x-0' : side === 'left' ? '-translate-x-full' : 'translate-x-full',
            className,
          )}
          {...props}
        >
          {children}
        </aside>
      </>
    );
  }

  return (
    <div
      data-slot="sidebar-gap"
      data-state={state}
      data-collapsible={state === 'collapsed' ? collapsible : ''}
      className={cn(
        'relative hidden shrink-0 self-stretch overflow-hidden transition-[width] duration-300 ease-in-out md:block',
        state === 'collapsed' && collapsible === 'offcanvas'
          ? 'w-0 min-w-0'
          : 'w-[var(--sidebar-width)]',
      )}
    >
      <aside
        data-slot="sidebar"
        data-state={state}
        data-variant={variant}
        data-side={side}
        className={cn(
          'flex h-full w-[var(--sidebar-width)] min-w-[var(--sidebar-width)] flex-col border-border bg-surface text-foreground transition-transform duration-300 ease-in-out',
          side === 'left' ? 'border-r' : 'border-l',
          state === 'collapsed' && collapsible === 'offcanvas' && (
            side === 'left' ? '-translate-x-full' : 'translate-x-full'
          ),
          className,
        )}
        {...props}
      >
        {children}
      </aside>
    </div>
  );
}

export function SidebarTrigger({ className = '', onClick, ...props }) {
  const { toggleSidebar } = useSidebar();

  return (
    <button
      type="button"
      data-slot="sidebar-trigger"
      aria-label="Toggle lab list"
      className={cn(
        'inline-flex h-8 w-8 items-center justify-center rounded-md border border-border bg-surface text-foreground-secondary transition-colors hover:bg-surface-secondary hover:text-foreground focus:outline-none focus-visible:ring-2 focus-visible:ring-primary',
        className,
      )}
      onClick={(event) => {
        onClick?.(event);
        toggleSidebar();
      }}
      {...props}
    >
      <PanelLeft className="h-4 w-4" />
      <span className="sr-only">Toggle lab list</span>
    </button>
  );
}

export function SidebarInset({ className = '', ...props }) {
  return (
    <div
      data-slot="sidebar-inset"
      className={cn('relative flex min-w-0 flex-1 flex-col bg-background', className)}
      {...props}
    />
  );
}

export function SidebarHeader({ className = '', ...props }) {
  return (
    <div
      data-slot="sidebar-header"
      className={cn('flex flex-col gap-2 border-b border-border p-3', className)}
      {...props}
    />
  );
}

export function SidebarContent({ className = '', ...props }) {
  return (
    <div
      data-slot="sidebar-content"
      className={cn('flex min-h-0 flex-1 flex-col gap-2 overflow-auto p-2', className)}
      {...props}
    />
  );
}

export function SidebarGroup({ className = '', ...props }) {
  return (
    <div
      data-slot="sidebar-group"
      className={cn('relative flex w-full min-w-0 flex-col p-1', className)}
      {...props}
    />
  );
}

export function SidebarGroupLabel({ className = '', ...props }) {
  return (
    <div
      data-slot="sidebar-group-label"
      className={cn(
        'flex h-8 items-center px-2 text-xs font-semibold uppercase tracking-wider text-foreground-muted',
        className,
      )}
      {...props}
    />
  );
}

export function SidebarGroupContent({ className = '', ...props }) {
  return <div data-slot="sidebar-group-content" className={cn('w-full text-sm', className)} {...props} />;
}
