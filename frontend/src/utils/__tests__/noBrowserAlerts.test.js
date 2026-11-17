import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';

describe('No Browser Alert / Confirm / Prompt Policy', () => {
  it('ensures zero native browser dialog calls exist across frontend/src', () => {
    const srcDir = path.resolve(__dirname, '../../');
    const pattern = /(?:window\s*\.\s*)?(alert|confirm|prompt)\s*\(/;
    const violations = [];

    function scanDir(dir) {
      const entries = fs.readdirSync(dir, { withFileTypes: true });
      for (const entry of entries) {
        const fullPath = path.join(dir, entry.name);
        if (entry.isDirectory()) {
          scanDir(fullPath);
        } else if (entry.isFile() && /\.(jsx?|tsx?)$/.test(entry.name)) {
          // Skip the dialogGuard implementation itself which defines the interceptor patch
          if (entry.name === 'dialogGuard.js' || entry.name === 'dialogGuard.test.js') {
            continue;
          }

          const content = fs.readFileSync(fullPath, 'utf8');
          const lines = content.split('\n');
          lines.forEach((line, index) => {
            const clean = line.trim();
            if (clean.startsWith('//') || clean.startsWith('/*') || clean.startsWith('*')) {
              return;
            }

            const match = pattern.exec(line);
            if (match) {
              const startIdx = match.index;
              if (startIdx > 0) {
                const prevChar = line[startIdx - 1];
                // Ignore method calls like dialog.alert( or setState( or showToast(
                if (/[a-zA-Z0-9_$]/.test(prevChar)) {
                  return;
                }
              }

              // Filter out false positive identifier usages (e.g. state setters or validations)
              const matchedCall = match[0];
              if (
                clean.includes('setConfirm') ||
                clean.includes('validatePasswordConfirm') ||
                clean.includes('fieldErrors.confirm') ||
                clean.includes('dialog.alert') ||
                clean.includes('dialog.confirm')
              ) {
                return;
              }

              violations.push(`${path.relative(srcDir, fullPath)}:${index + 1} -> ${clean}`);
            }
          });
        }
      }
    }

    scanDir(srcDir);
    expect(violations).toEqual([]);
  });
});
