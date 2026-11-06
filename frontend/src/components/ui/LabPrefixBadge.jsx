import { Badge } from './badge';

export function normalizeLabPrefix(prefix) {
  if (prefix === 'Practice') return 'Practice';
  if (prefix === 'Midterm') return 'Midterm';
  return 'Official';
}

export default function LabPrefixBadge({ prefix, className = '' }) {
  const label = normalizeLabPrefix(prefix);
  const variant =
    label === 'Practice' ? 'outline' : label === 'Midterm' ? 'secondary' : 'default';
  return (
    <Badge variant={variant} className={className}>
      {label}
    </Badge>
  );
}
