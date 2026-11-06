import React, { useCallback, useEffect, useRef, useState } from 'react';
import { FolderOpen, Loader2, Upload, X } from 'lucide-react';
import { useToast } from '../../ui/Toast';
import Combobox from '../../ui/Combobox';
import { normalizeLabPrefix } from '../../ui/LabPrefixBadge';
import { toFriendlyError } from '../../../utils/apiError';
import {
  SOLUTION_IMPORT_KIND,
  defaultImportKindFromLabPrefix,
  layoutForImportKind,
  parseSolutionFolder,
  prefixForImportKind,
} from '../../../utils/solutionFolderParse';

const IMPORT_KIND_OPTIONS = [
  { value: SOLUTION_IMPORT_KIND.OFFICIAL, label: 'Official' },
  { value: SOLUTION_IMPORT_KIND.PRACTICE, label: 'Practice' },
  { value: SOLUTION_IMPORT_KIND.MIDTERM, label: 'Midterm' },
];

function formatBytes(bytes) {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const idx = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / 1024 ** idx;
  return `${value >= 10 || idx === 0 ? Math.round(value) : value.toFixed(1)} ${units[idx]}`;
}

function isSolutionSourcePath(relativePath) {
  const lower = relativePath.toLowerCase();
  return lower.endsWith('.java') || lower.endsWith('.mmd');
}

function summarizeEntries(entries) {
  let javaCount = 0;
  let mmdCount = 0;
  let totalBytes = 0;
  entries.forEach(({ file, relativePath }) => {
    const lower = relativePath.toLowerCase();
    if (lower.endsWith('.java')) javaCount += 1;
    if (lower.endsWith('.mmd')) mmdCount += 1;
    totalBytes += file.size || 0;
  });
  const firstPath = entries[0]?.relativePath || '';
  const folderLabel = firstPath.split('/').filter(Boolean)[0] || 'Solution folder';
  return { folderLabel, entries, javaCount, mmdCount, totalBytes };
}

function walkEntry(entry, pathPrefix, collected) {
  return new Promise((resolve, reject) => {
    if (entry.isFile) {
      entry.file((file) => {
        collected.push({ file, relativePath: pathPrefix + file.name });
        resolve();
      }, reject);
    } else if (entry.isDirectory) {
      const reader = entry.createReader();
      const readAllBatches = (accumulated = []) => {
        reader.readEntries(async (batch) => {
          if (batch.length === 0) {
            for (const child of accumulated) {
              await walkEntry(child, `${pathPrefix}${entry.name}/`, collected);
            }
            resolve();
          } else {
            readAllBatches(accumulated.concat(batch));
          }
        }, reject);
      };
      readAllBatches();
    } else {
      resolve();
    }
  });
}

const PRIMARY_BTN =
  'inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-primary px-4 text-sm font-semibold text-white transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50';
const SECONDARY_BTN =
  'inline-flex h-10 items-center justify-center rounded-lg border border-border px-4 text-sm font-medium text-foreground-secondary transition-colors hover:bg-surface-secondary disabled:cursor-not-allowed disabled:opacity-50';

export default function SolutionImportPanel({
  disabled = false,
  savedLabPrefix = 'Official',
  onImportKindChange,
  onImport,
  actionBar = 'inline',
  onImportActionsChange,
}) {
  const showToast = useToast();
  const folderInputRef = useRef(null);
  const fileInputRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);
  const [selection, setSelection] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importKind, setImportKind] = useState(() =>
    defaultImportKindFromLabPrefix(normalizeLabPrefix(savedLabPrefix)),
  );

  useEffect(() => {
    if (selection) return;
    setImportKind(defaultImportKindFromLabPrefix(normalizeLabPrefix(savedLabPrefix)));
  }, [savedLabPrefix, selection]);

  const isMidtermKind = importKind === SOLUTION_IMPORT_KIND.MIDTERM;

  const folderHint = isMidtermKind
    ? (
      <>
        One folder with <span className="font-mono text-primary/80">.java</span> and optional{' '}
        <span className="font-mono text-primary/80">.mmd</span> directly inside (no{' '}
        <span className="font-mono text-primary/80">challenge_n</span>)
      </>
    )
    : (
      <>
        Lab root with <span className="font-mono text-primary/80">challenge_n</span> folders, or one{' '}
        <span className="font-mono text-primary/80">challenge_n</span> folder with .java and optional .mmd
      </>
    );

  const rejectSelection = useCallback((message) => {
    showToast({ message, type: 'warning' });
  }, [showToast]);

  const ingestRawEntries = useCallback((rawEntries) => {
    const entries = rawEntries.filter(({ relativePath }) => isSolutionSourcePath(relativePath));
    if (entries.length === 0) {
      rejectSelection('No .java or .mmd files found. Drop a folder that contains your solution sources.');
      return;
    }
    const parsed = parseSolutionFolder(entries, { layout: layoutForImportKind(importKind) });
    if (!parsed.ok) {
      rejectSelection(parsed.error || 'Folder does not match the selected layout.');
      return;
    }
    setSelection({
      ...summarizeEntries(entries),
      entries,
      importKind,
    });
  }, [importKind, rejectSelection]);

  const handleFileList = useCallback((fileList) => {
    const entries = Array.from(fileList || []).map((file) => ({
      file,
      relativePath: file.webkitRelativePath || file.name,
    }));
    ingestRawEntries(entries);
  }, [ingestRawEntries]);

  const handleDropItems = useCallback(async (items) => {
    const topLevelEntries = Array.from(items)
      .map((item) => item.webkitGetAsEntry?.())
      .filter(Boolean);

    if (topLevelEntries.length === 0) return;

    const collected = [];
    for (const entry of topLevelEntries) {
      await walkEntry(entry, '', collected);
    }
    ingestRawEntries(collected);
  }, [ingestRawEntries]);

  const onDragEnter = (e) => {
    e.preventDefault();
    if (!disabled) setIsDragging(true);
  };

  const onDragOver = (e) => {
    e.preventDefault();
    if (!disabled) setIsDragging(true);
  };

  const onDragLeave = (e) => {
    e.preventDefault();
    if (!e.currentTarget.contains(e.relatedTarget)) setIsDragging(false);
  };

  const onDrop = async (e) => {
    e.preventDefault();
    setIsDragging(false);
    if (disabled) return;

    const items = e.dataTransfer?.items;
    if (items?.length) {
      await handleDropItems(items);
      return;
    }

    if (e.dataTransfer?.files?.length) {
      handleFileList(e.dataTransfer.files);
    }
  };

  const openFolderPicker = (e) => {
    e?.stopPropagation();
    if (!disabled) folderInputRef.current?.click();
  };

  const openFilePicker = (e) => {
    e?.stopPropagation();
    if (!disabled) fileInputRef.current?.click();
  };

  const clearSelection = () => {
    setSelection(null);
    if (folderInputRef.current) folderInputRef.current.value = '';
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleImport = useCallback(async () => {
    if (!selection || importing) return;
    if (typeof onImport !== 'function') {
      showToast({
        message: 'Solution import is not available on this screen.',
        type: 'warning',
      });
      return;
    }
    setImporting(true);
    try {
      await onImport(selection);
      clearSelection();
    } catch (e) {
      showToast({
        message: toFriendlyError(e, 'solution-import'),
        type: 'error',
      });
    } finally {
      setImporting(false);
    }
  }, [clearSelection, importing, onImport, selection, showToast]);

  useEffect(() => {
    if (actionBar !== 'external' || typeof onImportActionsChange !== 'function') return;
    onImportActionsChange({
      hasSelection: Boolean(selection),
      importing,
      clearSelection,
      handleImport,
    });
  }, [actionBar, clearSelection, handleImport, importing, onImportActionsChange, selection]);

  const dropZoneClass = `flex w-full flex-1 flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed px-3 py-4 text-sm transition-colors ${
    actionBar === 'external' ? 'min-h-[6.5rem]' : 'min-h-[7.5rem]'
  } ${
    disabled
      ? 'cursor-not-allowed opacity-50'
      : 'cursor-pointer'
  } ${
    isDragging && !disabled
      ? 'border-primary bg-primary-light text-primary-text'
      : 'border-border bg-surface-secondary text-foreground-secondary hover:border-primary hover:bg-primary-light'
  }`;

  const actionButtons = (
    <>
      <button
        type="button"
        className={SECONDARY_BTN}
        disabled={disabled || importing || !selection}
        onClick={clearSelection}
      >
        Clear
      </button>
      <button
        type="button"
        className={PRIMARY_BTN}
        disabled={disabled || importing || !selection}
        onClick={handleImport}
      >
        {importing && <Loader2 className="h-4 w-4 animate-spin" />}
        Import solution
      </button>
    </>
  );

  return (
    <div className="flex min-h-0 flex-1 flex-col space-y-3">
      <div className="space-y-1.5">
        <span className="text-xs font-medium text-foreground">Prefix</span>
        <Combobox
          aria-label="Lab prefix"
          title="Official, Practice, or Midterm — sets lab prefix and student upload folder rules"
          allowEmpty={false}
          className="w-full min-w-0"
          value={importKind}
          disabled={disabled || importing}
          options={IMPORT_KIND_OPTIONS}
          onChange={async (next) => {
            if (next === importKind) return;
            const previous = importKind;
            setImportKind(next);
            setSelection(null);
            if (folderInputRef.current) folderInputRef.current.value = '';
            if (fileInputRef.current) fileInputRef.current.value = '';
            if (typeof onImportKindChange !== 'function') return;
            const ok = await onImportKindChange(next);
            if (!ok) setImportKind(previous);
          }}
        />
        <p className="text-[11px] leading-relaxed text-foreground-muted">
          Sets lab prefix to{' '}
          <span className="font-medium text-foreground">{prefixForImportKind(importKind)}</span>{' '}
          when you change layout. Student upload folder rules follow that prefix.
        </p>
      </div>
      <input
        ref={folderInputRef}
        type="file"
        multiple
        className="hidden"
        disabled={disabled}
        webkitdirectory=""
        directory=""
        onChange={(e) => {
          if (e.target.files?.length) handleFileList(e.target.files);
          e.target.value = '';
        }}
      />
      <input
        ref={fileInputRef}
        type="file"
        accept=".java,.mmd,text/x-java-source,application/java"
        multiple
        className="hidden"
        disabled={disabled}
        onChange={(e) => {
          if (e.target.files?.length) handleFileList(e.target.files);
          e.target.value = '';
        }}
      />

      <div className="flex min-h-0 flex-1 flex-col">
      {!selection ? (
        <div
          role="button"
          tabIndex={disabled ? -1 : 0}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') openFolderPicker(e);
          }}
          onDragEnter={onDragEnter}
          onDragOver={onDragOver}
          onDragLeave={onDragLeave}
          onDrop={onDrop}
          onClick={openFolderPicker}
          className={dropZoneClass}
        >
          <Upload className={`h-6 w-6 ${isDragging ? 'text-primary' : 'text-foreground-muted'}`} aria-hidden="true" />
          <span className="text-center font-medium text-foreground">Drop folder or click to choose</span>
          <span className="text-center text-xs text-foreground-muted">{folderHint}</span>
          <button
            type="button"
            disabled={disabled}
            onClick={openFilePicker}
            className="mt-1 text-xs text-foreground-muted transition-colors hover:text-primary disabled:opacity-50"
          >
            Or pick individual .java / .mmd files
          </button>
        </div>
      ) : (
        <div
          className={`rounded-xl border border-border bg-surface-secondary/40 p-4 ${actionBar === 'external' ? 'flex flex-1 flex-col justify-center' : ''}`}
          onClick={(e) => e.stopPropagation()}
        >
          <div className="flex items-start gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/15">
              <FolderOpen className="h-5 w-5 text-primary" aria-hidden="true" />
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium text-foreground" title={selection.folderLabel}>
                {selection.folderLabel}
              </p>
              <p className="text-xs text-foreground-muted">
                {selection.javaCount} Java · {selection.mmdCount} MMD · {formatBytes(selection.totalBytes)}
                {' · '}
                {prefixForImportKind(selection.importKind || importKind)}
              </p>
            </div>
            <button
              type="button"
              disabled={disabled || importing}
              onClick={clearSelection}
              className="shrink-0 rounded p-1 text-foreground-muted transition-colors hover:bg-surface-secondary hover:text-error disabled:opacity-50"
              title="Remove selection"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
          <button
            type="button"
            disabled={disabled}
            onClick={openFolderPicker}
            className="mt-3 text-xs font-medium text-primary transition-colors hover:text-primary-hover disabled:opacity-50"
          >
            Choose a different folder
          </button>
        </div>
      )}
      </div>

      {actionBar === 'inline' && (
        <div className="flex flex-wrap items-center justify-end gap-2 pt-2">
          {actionButtons}
        </div>
      )}
    </div>
  );
}

export { PRIMARY_BTN as schedulingPrimaryBtnClass, SECONDARY_BTN as schedulingSecondaryBtnClass };
