import { firstCompileErrorLine } from './formatters';

export function classListHasCompileError(classes = []) {
  if (!Array.isArray(classes)) {
    return false;
  }
  return classes.some((cls) => Boolean(firstCompileErrorLine(cls?.error)));
}

export function challengeBundleHasCompileError(bundle) {
  if (!bundle) {
    return false;
  }
  if (classListHasCompileError(bundle.class ?? [])) {
    return true;
  }
  const testcases = bundle.testcases ?? [];
  return testcases.some((tc) => {
    const feedback = String(tc.feedback ?? '').trim().toLowerCase();
    return feedback.startsWith('compilation error:');
  });
}

export function labResultHasCompileError(labResult) {
  if (!labResult || typeof labResult !== 'object') {
    return false;
  }
  return Object.values(labResult).some(challengeBundleHasCompileError);
}
