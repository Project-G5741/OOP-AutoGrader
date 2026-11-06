import React, { useEffect, useState } from 'react';
import { clampRepeatCount, FIELD_CLASS, MAX_REPEAT_COUNT } from './testcaseAuthoring';

/**
 * Repeat count editor: keeps a string draft while typing so "1" + "0" becomes "10",
 * not a parsed/clamped jump (controlled number inputs fight multi-digit entry).
 */
export default function TestcaseRepeatCountInput({ repeatCount, onChange, className = `${FIELD_CLASS} w-24` }) {
  const committed = repeatCount ?? 1;
  const [draft, setDraft] = useState(() => String(committed));

  useEffect(() => {
    setDraft(String(repeatCount ?? 1));
  }, [repeatCount]);

  const commit = (raw) => {
    const next = clampRepeatCount(raw === '' ? '1' : raw);
    setDraft(String(next));
    if (next !== committed) {
      onChange(next);
    }
  };

  return (
    <input
      type="text"
      inputMode="numeric"
      autoComplete="off"
      className={className}
      value={draft}
      onChange={(e) => {
        setDraft(e.target.value.replace(/\D/g, ''));
      }}
      onBlur={() => commit(draft)}
      onKeyDown={(e) => {
        if (e.key === 'Enter') {
          e.currentTarget.blur();
        }
      }}
      aria-label={`Repeat count, minimum 1, maximum ${MAX_REPEAT_COUNT}`}
    />
  );
}
