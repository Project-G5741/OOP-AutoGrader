import { describe, expect, it, vi } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';
import { handleRowDoubleClickEdit, isRowInteractiveTarget } from '../rowDoubleClick';

function mockElement(matchedInteractive = null) {
  return {
    closest: vi.fn(() => matchedInteractive),
  };
}

describe('rowDoubleClick', () => {
  it('invokes onEdit when double-clicking a plain table cell or text node', () => {
    const onEdit = vi.fn();
    const row = mockElement(null);
    const cell = mockElement(null);
    const event = { target: cell, currentTarget: row };

    handleRowDoubleClickEdit(event, onEdit);
    expect(onEdit).toHaveBeenCalledTimes(1);
    expect(onEdit).toHaveBeenCalledWith(event);

    const textNode = { parentElement: cell };
    const textEvent = { target: textNode, currentTarget: row };
    handleRowDoubleClickEdit(textEvent, onEdit);
    expect(onEdit).toHaveBeenCalledTimes(2);
  });

  it('ignores double-clicks originating from buttons or interactive controls inside a row', () => {
    const onEdit = vi.fn();
    const row = mockElement(null);
    const actionButton = {};
    const iconInsideButton = mockElement(actionButton);

    expect(isRowInteractiveTarget(iconInsideButton, row)).toBe(true);

    handleRowDoubleClickEdit({ target: iconInsideButton, currentTarget: row }, onEdit);
    expect(onEdit).not.toHaveBeenCalled();
  });

  it('still invokes onEdit when the row element itself matches role="button"', () => {
    const onEdit = vi.fn();
    const row = {};
    row.closest = vi.fn(() => row);
    const cell = mockElement(row);

    expect(isRowInteractiveTarget(cell, row)).toBe(false);
    handleRowDoubleClickEdit({ target: cell, currentTarget: row }, onEdit);
    expect(onEdit).toHaveBeenCalledTimes(1);
  });

  it('safely handles null events or non-function handlers', () => {
    expect(() => handleRowDoubleClickEdit(null, null)).not.toThrow();
    expect(isRowInteractiveTarget(null, null)).toBe(false);
  });

  it('keeps row double-click edit wired across all editable tables and lists', () => {
    const srcDir = path.resolve(__dirname, '../../');
    const targets = [
      { file: 'components/UserTable.jsx', expected: /handleRowDoubleClickEdit\(event,\s*\(\)\s*=>\s*onEdit\?\.\(u\)\)/ },
      { file: 'pages/GachaManagement.jsx', expected: /handleRowDoubleClickEdit\(event,\s*\(\)\s*=>\s*openManageModal\(row\)\)/ },
      { file: 'pages/GachaManagement.jsx', expected: /handleRowDoubleClickEdit\(event,\s*\(\)\s*=>\s*startEditPet\(pet\)\)/ },
      { file: 'pages/BlitzQuestionBank.jsx', expected: /handleRowDoubleClickEdit\(event,\s*\(\)\s*=>\s*openEditModal\(q\)\)/ },
    ];

    for (const { file, expected } of targets) {
      const content = fs.readFileSync(path.join(srcDir, file), 'utf8');
      expect(content, `${file} should wire row double-click edit`).toMatch(expected);
    }
  });
});
