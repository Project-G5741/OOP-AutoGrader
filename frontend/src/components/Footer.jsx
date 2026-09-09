const FOOTER_LABEL = 'Object-Oriented Programming';

export default function Footer({ variant = 'default' }) {
  if (variant === 'login') {
    return (
      <div className="mt-5 text-center text-[0.95rem] text-foreground-muted">
        {FOOTER_LABEL}
      </div>
    );
  }

  if (variant === 'compact') {
    return (
      <div className="px-6 pb-4 text-center">
        <p className="text-xs text-foreground-disabled">{FOOTER_LABEL}</p>
      </div>
    );
  }

  return (
    <footer className="mt-8 border-t border-border py-5 text-center">
      <p className="text-sm text-foreground-muted">{FOOTER_LABEL}</p>
    </footer>
  );
}
