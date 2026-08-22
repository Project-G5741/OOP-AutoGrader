import { brand } from '../../theme/brand';

const VARIANTS = {
  header: {
    box: 'flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-primary to-primary-active shadow-sm shadow-primary/20',
    icon: 'h-5 w-5 text-white',
  },
  login: {
    box: 'logo-box',
    icon: 'logo-icon',
  },
  inline: {
    box: '',
    icon: 'h-5 w-5',
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
