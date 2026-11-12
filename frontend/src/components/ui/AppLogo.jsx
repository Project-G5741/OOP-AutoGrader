import { brand } from '../../theme/brand';

const VARIANTS = {
  header: {
    box: 'flex h-10 w-10 items-center justify-center rounded-xl bg-navy text-white shadow-stage-1 ring-1 ring-primary/25 transition-transform duration-200 hover:-translate-y-0.5',
    icon: 'h-5 w-5 text-white',
  },
  login: {
    box: 'logo-box',
    icon: 'logo-icon',
  },
  inline: {
    box: '',
    icon: 'h-5 w-5 text-navy dark:text-primary-text',
  },
};

export default function AppLogo({ variant = 'header', className = '' }) {
  const styles = VARIANTS[variant] ?? VARIANTS.header;
  const { logo } = brand;
  const Icon = logo.icon;

  const content = <Icon className={styles.icon} aria-hidden={variant !== 'inline'} />;

  if (variant === 'inline') {
    return <span className={className}>{content}</span>;
  }

  return (
    <div className={`${styles.box} ${className}`.trim()}>
      {content}
    </div>
  );
}
