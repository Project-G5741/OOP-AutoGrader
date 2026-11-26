export const BLITZ_KINDS = ['BUG', 'OUTPUT', 'UML', 'MODIFIER'];
export const BLITZ_DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'];

export function createEmptyBlitzDraft() {
  return {
    kind: 'OUTPUT',
    difficulty: 'EASY',
    topic: '',
    prompt: '',
    codeText: '',
    options: ['', '', '', ''],
    correctIndex: 0,
    explanation: '',
    active: true,
  };
}

export function splitCodeText(codeText) {
  return String(codeText || '')
    .replace(/\r/g, '')
    .split('\n');
}

export function validateBlitzQuestionDraft(draft) {
  const errors = {};
  const kind = String(draft?.kind || '').trim().toUpperCase();
  const promptText = String(draft?.prompt || '').trim();
  const explanationText = String(draft?.explanation || '').trim();
  const lines = splitCodeText(draft?.codeText);
  const nonBlankLine = lines.some((l) => l.trim().length > 0);

  if (!promptText) {
    errors.prompt = 'Prompt is required.';
  } else if (promptText.length > 400) {
    errors.prompt = 'Prompt cannot exceed 400 characters.';
  }

  if (!nonBlankLine) {
    errors.codeLines = 'Code snippet is required.';
  } else if (lines.length > 16) {
    errors.codeLines = 'Code snippet can have at most 16 lines.';
  } else if (lines.some((l) => l.length > 240)) {
    errors.codeLines = 'Each code line cannot exceed 240 characters.';
  }

  const idx = Number(draft?.correctIndex);
  if (kind === 'BUG') {
    if (!Number.isInteger(idx) || idx < 0 || idx >= lines.length) {
      errors.correctIndex = `Select a buggy line from 1 to ${Math.max(1, lines.length)}.`;
    }
  } else {
    const opts = Array.isArray(draft?.options) ? draft.options.map((o) => String(o || '').trim()) : [];
    if (opts.length !== 4 || opts.some((o) => !o)) {
      errors.options = 'Fill in all 4 choices.';
    } else if (new Set(opts).size !== 4) {
      errors.options = 'All 4 choices must be distinct.';
    } else if (opts.some((o) => o.length > 180)) {
      errors.options = 'Each choice cannot exceed 180 characters.';
    }
    if (!Number.isInteger(idx) || idx < 0 || idx > 3) {
      errors.correctIndex = 'Select the correct choice (1–4).';
    }
  }

  if (!explanationText) {
    errors.explanation = 'Explanation is required.';
  } else if (explanationText.length > 400) {
    errors.explanation = 'Explanation cannot exceed 400 characters.';
  }

  return errors;
}

export function buildBlitzUpsertPayload(draft) {
  const kind = String(draft?.kind || 'OUTPUT').trim().toUpperCase();
  const difficulty = String(draft?.difficulty || 'EASY').trim().toUpperCase();
  const codeLines = splitCodeText(draft?.codeText);
  return {
    kind,
    difficulty,
    topic: String(draft?.topic || '').trim() || null,
    prompt: String(draft?.prompt || '').trim(),
    codeLines,
    options: kind === 'BUG'
      ? []
      : (draft?.options || []).map((o) => String(o || '').trim()),
    correctIndex: Number(draft?.correctIndex ?? 0),
    explanation: String(draft?.explanation || '').trim(),
    active: draft?.active !== false,
  };
}

export function parseBlitzImportJson(rawText) {
  const text = String(rawText || '').trim();
  if (!text) {
    return { questions: [], error: 'Paste JSON or choose a .json file first.' };
  }
  let parsed;
  try {
    parsed = JSON.parse(text);
  } catch {
    return { questions: [], error: 'Invalid JSON syntax.' };
  }
  const list = Array.isArray(parsed)
    ? parsed
    : (parsed && Array.isArray(parsed.questions) ? parsed.questions : null);
  if (!list) {
    return { questions: [], error: 'JSON must be an array or an object with a "questions" array.' };
  }
  if (list.length === 0) {
    return { questions: [], error: 'Question array cannot be empty.' };
  }
  if (list.length > 500) {
    return { questions: [], error: 'Import batch cannot exceed 500 questions.' };
  }

  const normalized = list.map((item) => {
    const codeLines = Array.isArray(item?.codeLines)
      ? item.codeLines.map((l) => String(l ?? ''))
      : typeof item?.code === 'string'
      ? splitCodeText(item.code)
      : [];
    const kind = String(item?.kind || '').trim().toUpperCase();
    return {
      kind,
      difficulty: String(item?.difficulty || '').trim().toUpperCase(),
      prompt: String(item?.prompt || '').trim(),
      codeLines,
      options: kind === 'BUG'
        ? []
        : (Array.isArray(item?.options) ? item.options.map((o) => String(o ?? '').trim()) : []),
      correctIndex: Number.isInteger(Number(item?.correctIndex)) ? Number(item.correctIndex) : -1,
      explanation: String(item?.explanation || '').trim(),
      topic: item?.topic ? String(item.topic).trim() : null,
      active: item?.active !== false,
    };
  });

  return { questions: normalized, error: null };
}

export function buildBlitzSampleJson() {
  return JSON.stringify(
    {
      questions: [
        {
          kind: 'BUG',
          difficulty: 'EASY',
          topic: 'Static & this',
          prompt: 'Click the line that causes a compile error:',
          codeLines: [
            'public class LabCounter {',
            '    private int count = 0;',
            '    public static void reset() {',
            '        this.count = 0;',
            '    }',
            '}',
          ],
          correctIndex: 3,
          explanation: 'Line 4: Cannot use this inside a static method.',
          active: true,
        },
        {
          kind: 'OUTPUT',
          difficulty: 'MEDIUM',
          topic: 'Polymorphism',
          prompt: 'What does this snippet print?',
          codeLines: [
            'class Base { String tag() { return "B"; } }',
            'class Sub extends Base { String tag() { return "S"; } }',
            'Base ref = new Sub();',
            'System.out.println(ref.tag());',
          ],
          options: ['S', 'B', 'null', 'Compile error'],
          correctIndex: 0,
          explanation: 'Dynamic dispatch invokes Sub.tag() at runtime.',
          active: true,
        },
        {
          kind: 'UML',
          difficulty: 'MEDIUM',
          topic: 'Composition',
          prompt: 'Engine lifetime is owned by Car. Pick the Mermaid relation:',
          codeLines: [
            'public class Car {',
            '    private final Engine engine = new Engine();',
            '}',
          ],
          options: ['Car *-- Engine', 'Car o-- Engine', 'Car <|-- Engine', 'Car ..> Engine'],
          correctIndex: 0,
          explanation: 'Exclusive ownership maps to Composition (*--).',
          active: true,
        },
        {
          kind: 'MODIFIER',
          difficulty: 'HARD',
          topic: 'Override Visibility',
          prompt: 'Does LaserPrinter compile cleanly?',
          codeLines: [
            'class Printer { public void print() { } }',
            'class LaserPrinter extends Printer {',
            '    @Override protected void print() { }',
            '}',
          ],
          options: [
            'Compile error: cannot reduce visibility',
            'Compiles cleanly',
            'Compiles with warning',
            'Runtime IllegalAccessError',
          ],
          correctIndex: 0,
          explanation: 'Overriding methods cannot narrow visibility from public to protected.',
          active: true,
        },
      ],
    },
    null,
    2,
  );
}
