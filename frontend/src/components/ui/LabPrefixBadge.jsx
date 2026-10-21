import { Badge } from './badge';

export function normalizeLabPrefix(prefix) {
  return prefix === 'Practice' ? 'Practice' : 'Official';
}

export default function LabPrefixBadge({ prefix, className = '' }) {
  const label = normalizeLabPrefix(prefix);
  const practice = label === 'Practice';
  return (
    <Badge variant={practice ? 'outline' : 'default'} className={className}>
      {label}
    </Badge>
  );
}
