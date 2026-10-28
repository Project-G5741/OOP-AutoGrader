/* Lucian UX/UI Prompt #036 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#036 */
import { useId, useState } from 'react';
import './PasswordStrengthField.css';

const TESTS = {
  len: (value) => value.length >= 6,
  up: (value) => /[A-Z]/.test(value),
  num: (value) => /\d/.test(value),
  sym: (value) => /[^A-Za-z0-9]/.test(value),
};

const RULES = [
  { id: 'len', label: '6+ characters' },
  { id: 'up', label: 'An uppercase letter' },
  { id: 'num', label: 'A number' },
  { id: 'sym', label: 'A symbol' },
];

const LEVELS = [
  ['Type a password', 'var(--ux-line-2)'],
  ['Weak', '#E5484D'],
  ['Fair', '#F07A3E'],
  ['Good', '#E8B417'],
  ['Strong', 'var(--ux-mint)'],
];

export default function PasswordStrengthField({
  value,
  onChange,
  onBlur,
  placeholder = 'Create a password',
  error = '',
}) {
  const uid = useId();
  const inputId = `${uid}-password`;
  const rulesId = `${uid}-rules`;
  const errorId = `${uid}-error`;
  const [shown, setShown] = useState(false);

  const passed = RULES.reduce((count, rule) => count + (TESTS[rule.id](value) ? 1 : 0), 0);
  const level = value ? Math.max(1, passed) : 0;
  const [word, color] = LEVELS[level];
  const describedBy = error ? `${rulesId} ${errorId}` : rulesId;

  function toggleVisibility() {
    setShown((current) => !current);
  }

  return (
    <div className={shown ? 'u036 is-show' : 'u036'} style={{ '--lv': color }}>
      <label className="u036-lab" htmlFor={inputId}>
        Set Password <span className="u036-req">*</span>
      </label>
      <div className="u036-box">
        <input
          id={inputId}
          className="u036-in"
          type={shown ? 'text' : 'password'}
          value={value}
          onChange={onChange}
          onBlur={onBlur}
          placeholder={placeholder}
          autoComplete="new-password"
          aria-describedby={describedBy}
          aria-invalid={error ? 'true' : 'false'}
        />
        <button
          className="u036-eye"
          type="button"
          tabIndex={-1}
          aria-label={shown ? 'Hide password' : 'Show password'}
          aria-pressed={shown ? 'true' : 'false'}
          onClick={toggleVisibility}
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <g className="u036-lid">
              <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" />
            </g>
            <circle className="u036-pupil" cx="12" cy="12" r="3" />
            <path className="u036-slash" d="M4 4l16 16" />
          </svg>
        </button>
      </div>
      {error ? (
        <p className="u036-err" id={errorId}>
          {error}
        </p>
      ) : null}
      <div className="u036-meter" aria-hidden="true">
        <i className={level > 0 ? 'on' : undefined} />
        <i className={level > 1 ? 'on' : undefined} />
        <i className={level > 2 ? 'on' : undefined} />
        <i className={level > 3 ? 'on' : undefined} />
      </div>
      <p className="u036-lv">
        <span>Strength</span>
        <b className="u036-word" aria-live="polite">
          {word}
        </b>
      </p>
      <ul className="u036-rules" id={rulesId}>
        {RULES.map((rule) => (
          <li key={rule.id} className={TESTS[rule.id](value) ? 'u036-r ok' : 'u036-r'} data-r={rule.id}>
            <i aria-hidden="true">
              <svg viewBox="0 0 20 20">
                <path d="M5.5 10.5l3 3 6-6.5" />
              </svg>
            </i>
            {rule.label}
          </li>
        ))}
      </ul>
    </div>
  );
}
