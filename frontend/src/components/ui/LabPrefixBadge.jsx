import { Badge } from './badge';

export function normalizeLabPrefix(prefix) {
  if (prefix === 'Practice') return 'Practice';
  if (prefix === 'Midterm') return 'Midterm';
  if (prefix === 'Final') return 'Final';
  return 'Official';
}

export function usesFlatStudentUploadPrefix(prefix) {
  const label = normalizeLabPrefix(prefix);
  return label === 'Midterm' || label === 'Final';
}

export default function LabPrefixBadge({ prefix, className = '' }) {
  const label = normalizeLabPrefix(prefix);
  const variant =
    label === 'Practice' ? 'teal' : label === 'Midterm' || label === 'Final' ? 'gold' : 'navy';
  return (
    <Badge variant={variant} className={className}>
      {label}
    </Badge>
  );
}
