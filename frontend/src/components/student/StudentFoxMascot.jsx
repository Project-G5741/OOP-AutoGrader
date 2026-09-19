import { Mascot } from 'page-mascot';

export default function StudentFoxMascot({ size = 88, className = '' }) {
  return (
    <Mascot
      className={className}
      size={size}
      label="Fox mascot"
      directions="/mascots/fox-directions.webp"
      reactions="/mascots/fox-reactions.webp"
    />
  );
}
