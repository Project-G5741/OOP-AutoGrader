import React, { useCallback, useRef, useState } from 'react';
import { FolderOpen, Loader2, Upload, X } from 'lucide-react';
import { useToast } from '../../ui/Toast';

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

export default function SolutionImportPanel({
  disabled = false,
  onImport,
}) {
  const showToast = useToast();
  const folderInputRef = useRef(null);
  const fileInputRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);
  const [selection, setSelection] = useState(null);
  const [importing, setImporting] = useState(false);

  const rejectSelection = useCallback((message) => {
    showToast({ message, type: 'warning' });
  }, [showToast]);

  const ingestRawEntries = useCallback((rawEntries) => {
    const entries = rawEntries.filter(({ relativePath }) => isSolutionSourcePath(relativePath));
    if (entries.length === 0) {
      rejectSelection('No .java or .mmd files found. Drop a folder that contains your solution sources.');
      return;
    }
    setSelection(summarizeEntries(entries));
  }, [rejectSelection]);

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

  const handleImport = async () => {
    if (!selection || importing) return;
    if (typeof onImport === 'function') {
      setImporting(true);
      try {
        await onImport(selection);
      } finally {
        setImporting(false);
      }
      return;
    }
    const { folderLabel, javaCount, mmdCount } = selection;
    showToast({
      message: `${folderLabel}: ${javaCount} Java and ${mmdCount} MMD file(s) ready. Server import is not connected yet.`,
      type: 'warning',
      durationMs: 5000,
    });
  };

  const dropZoneClass = `flex min-h-[7.5rem] w-full flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed px-3 py-4 text-sm transition-colors ${
    disabled
      ? 'cursor-not-allowed opacity-50'
      : 'cursor-pointer'
  } ${
    isDragging && !disabled
      ? 'border-primary bg-primary-light text-primary-text'
      : 'border-border bg-surface-secondary text-foreground-secondary hover:border-primary hover:bg-primary-light'
  }`;

  return (
    <div className="space-y-4">
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
          <button
            type="button"
            disabled={disabled}
            onClick={openFilePicker}
            className="mt-1 text-xs text-foreground-muted transition-colors hover:text-primary disabled:opacity-50"
          >
          </button>
        </div>
      ) : (
        <div
          className="rounded-xl border border-border bg-surface-secondary/40 p-4"
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

      <div className="flex flex-wrap items-center justify-end gap-2 pt-2">
        <button
          type="button"
          className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground-secondary transition-colors hover:bg-surface-secondary disabled:cursor-not-allowed disabled:opacity-50"
          disabled={disabled || importing || !selection}
          onClick={clearSelection}
        >
          Clear
        </button>
        <button
          type="button"
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-primary-hover disabled:cursor-not-allowed disabled:opacity-50"
          disabled={disabled || importing || !selection}
          onClick={handleImport}
        >
          {importing && <Loader2 className="h-4 w-4 animate-spin" />}
          Import solution
        </button>
      </div>
    </div>
  );
}
