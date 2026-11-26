import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import {
  OOP_BLITZ_DAILY_CAP,
  OOP_BLITZ_MAX_MATCH_POINTS,
  OOP_BLITZ_MAX_RUNS,
  OOP_BLITZ_QUESTIONS_PER_MATCH,
  computeBlitzRunsRemaining,
  formatBlitzTimer,
  tokenizeJavaLine,
} from '../../utils/oopBlitzHelpers';
import {
  buildBlitzSampleJson,
  buildBlitzUpsertPayload,
  parseBlitzImportJson,
  validateBlitzQuestionDraft,
} from '../../utils/oopBlitzImport';

describe('oopBlitzHelpers', () => {
  it('locks OOP Blitz economy constants', () => {
    expect(OOP_BLITZ_QUESTIONS_PER_MATCH).toBe(8);
    expect(OOP_BLITZ_DAILY_CAP).toBe(250);
    expect(OOP_BLITZ_MAX_RUNS).toBe(3);
    expect(OOP_BLITZ_MAX_MATCH_POINTS).toBe(100);
  });

  it('tokenizes Java code into safe semantic tokens without mutating text', () => {
    const line = 'public static String tag = "EIU"; // count 2';
    const tokens = tokenizeJavaLine(line);
    expect(tokens.map((t) => t.text).join('')).toBe(line);
    expect(tokens.some((t) => t.type === 'kw' && t.text === 'public')).toBe(true);
    expect(tokens.some((t) => t.type === 'tp' && t.text === 'String')).toBe(true);
    expect(tokens.some((t) => t.type === 'str' && t.text === '"EIU"')).toBe(true);
    expect(tokens.some((t) => t.type === 'cmt' && t.text === '// count 2')).toBe(true);

    const numTokens = tokenizeJavaLine('int count = 42;');
    expect(numTokens.some((t) => t.type === 'num' && t.text === '42')).toBe(true);
  });

  it('computes remaining rewarded runs and formats countdown timers cleanly', () => {
    expect(computeBlitzRunsRemaining(null)).toBe(1);
    expect(computeBlitzRunsRemaining({ rewardedRunsAllowed: 3, rewardedRunsUsed: 1 })).toBe(2);
    expect(computeBlitzRunsRemaining({ rewardedRunsAllowed: 2, rewardedRunsUsed: 4 })).toBe(0);

    expect(formatBlitzTimer(15000)).toBe('15.0s');
    expect(formatBlitzTimer(3450)).toBe('3.5s');
    expect(formatBlitzTimer(-100)).toBe('0.0s');
  });
});

describe('oopBlitzImport helpers', () => {
  it('round-trips the built-in sample JSON into 4 valid questions', () => {
    const sample = buildBlitzSampleJson();
    const { questions, error } = parseBlitzImportJson(sample);
    expect(error).toBeNull();
    expect(questions).toHaveLength(4);
    expect(questions.map((q) => q.kind)).toEqual(['BUG', 'OUTPUT', 'UML', 'MODIFIER']);
  });

  it('rejects malformed JSON and validates question drafts', () => {
    expect(parseBlitzImportJson('{bad').error).toBeTruthy();
    expect(parseBlitzImportJson('[]').error).toBeTruthy();

    const errs = validateBlitzQuestionDraft({
      kind: 'OUTPUT',
      difficulty: 'EASY',
      prompt: '',
      codeText: '   ',
      options: ['1', '1', '', ''],
      correctIndex: 0,
      explanation: '',
    });
    expect(errs.prompt).toBeTruthy();
    expect(errs.codeLines).toBeTruthy();
    expect(errs.options).toBeTruthy();
    expect(errs.explanation).toBeTruthy();

    const validDraft = {
      kind: 'BUG',
      difficulty: 'HARD',
      topic: 'Final',
      prompt: 'Click the buggy line:',
      codeText: 'class A {\n  final int x = 1;\n  void f() { x = 2; }\n}',
      options: ['', '', '', ''],
      correctIndex: 2,
      explanation: 'Line 3: Cannot reassign final field.',
      active: true,
    };
    expect(validateBlitzQuestionDraft(validDraft)).toEqual({});
    const payload = buildBlitzUpsertPayload(validDraft);
    expect(payload.options).toEqual([]);
    expect(payload.codeLines).toHaveLength(4);
  });
});

describe('OOP Blitz UI design and security invariants', () => {
  it('forbids storage state, CSS/SVG gradients, dangerouslySetInnerHTML, and native browser dialogs', () => {
    const files = [
      'src/components/student/OopBlitzModal.jsx',
      'src/pages/BlitzQuestionBank.jsx',
    ];
    for (const rel of files) {
      const src = readFileSync(resolve(process.cwd(), rel), 'utf8');
      expect(src).not.toMatch(/\b(localStorage|sessionStorage)\b/);
      expect(src).not.toMatch(/dangerouslySetInnerHTML/);
      expect(src).not.toMatch(/(linear-gradient|radial-gradient|conic-gradient|bg-gradient-|linearGradient|radialGradient)/);
      expect(src).not.toMatch(/(?:window\s*\.\s*)?(alert|prompt)\s*\(/);
      expect(src).not.toMatch(/\b(?:bg|text|border)-(?:blue|red|green|amber|yellow|emerald|purple|pink|indigo|slate|gray)-\d{2,3}\b/);
      expect(src).not.toMatch(/\b(?:bg|text)-(?:white|black)\b/);
    }
  });
});
