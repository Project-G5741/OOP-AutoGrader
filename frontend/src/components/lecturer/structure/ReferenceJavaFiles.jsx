import { FileCode2, FolderOpen, Plus, Upload, X } from 'lucide-react';
import { useCallback, useRef, useState } from 'react';

function classNameFromJava(fileName, source) {
  const publicMatch = source.match(/public\s+class\s+(\w+)/);
  if (publicMatch) return publicMatch[1];
  const classMatch = source.match(/(?:^|\s)class\s+(\w+)/m);
  if (classMatch) return classMatch[1];
  return fileName.replace(/\.java$/i, '');
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
              await walkEntry(child, pathPrefix + entry.name + '/', collected);
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

async function readJavaEntries(entries) {
  const javaEntries = entries.filter(({ file }) => file.name.toLowerCase().endsWith('.java'));
  if (javaEntries.length === 0) return [];

  return Promise.all(
    javaEntries.map(
      ({ file, relativePath }) => new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
          const source = String(reader.result ?? '');
          const displayName = relativePath || file.name;
          resolve({
            className: classNameFromJava(file.name, source),
            source,
            fileName: displayName,
          });
        };
        reader.onerror = () => reject(reader.error);
        reader.readAsText(file);
      }),
    ),
  );
}

function FileChip({ file, onRemove }) {
  const label = file.fileName || `${file.className}.java`;
  const showPath = label.includes('/');

  return (
    <div
      className="flex items-center gap-2.5 rounded-lg border border-gray-200 bg-white px-3 py-2 dark:border-gray-700/80 dark:bg-[#161b22]"
      onClick={(e) => e.stopPropagation()}
    >
      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-emerald-500/15">
        <FileCode2 className="h-4 w-4 text-emerald-400" />
      </div>
      <div className="min-w-0 flex-1 text-left">
        <div className="truncate text-sm font-medium text-gray-800 dark:text-gray-100">
          {file.className}
        </div>
        <div className="truncate text-xs text-gray-500" title={label}>
          {showPath ? label : `${file.className}.java`}
        </div>
      </div>
      <button
        type="button"
        onClick={(e) => { e.stopPropagation(); onRemove(file); }}
        className="shrink-0 rounded p-1 text-gray-400 transition-colors hover:bg-gray-100 hover:text-red-400 dark:hover:bg-gray-800"
        title="Remove file"
      >
        <X className="h-4 w-4" />
      </button>
    </div>
  );
}

export default function ReferenceJavaFiles({ sources, onChange, onError }) {
  const folderInputRef = useRef(null);
  const fileInputRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);

  const loadedSources = sources.filter((s) => s.source?.trim());
  const hasFiles = loadedSources.length > 0;

  const mergeSources = useCallback((incoming) => {
    const next = [...loadedSources];
    incoming.forEach((file) => {
      const idx = next.findIndex((s) => s.className === file.className);
      if (idx >= 0) next[idx] = file;
      else next.push(file);
    });
    onChange(next);
  }, [loadedSources, onChange]);

  const ingestEntries = useCallback(async (entries) => {
    try {
      const parsed = await readJavaEntries(entries);
      if (parsed.length === 0) {
        onError?.('No .java files found. Drop a folder or select Java source files.');
        return;
      }
      mergeSources(parsed);
    } catch {
      onError?.('Could not read one or more files.');
    }
  }, [mergeSources, onError]);

  const handleFileList = useCallback(async (fileList) => {
    const entries = Array.from(fileList).map((file) => ({
      file,
      relativePath: file.webkitRelativePath || file.name,
    }));
    await ingestEntries(entries);
  }, [ingestEntries]);

  const handleDropItems = useCallback(async (items) => {
    const topLevelEntries = Array.from(items)
      .map((item) => item.webkitGetAsEntry?.())
      .filter(Boolean);

    if (topLevelEntries.length === 0) return;

    const collected = [];
    for (const entry of topLevelEntries) {
      await walkEntry(entry, '', collected);
    }
    await ingestEntries(collected);
  }, [ingestEntries]);

  const onDrop = async (e) => {
    e.preventDefault();
    setIsDragging(false);

    const items = e.dataTransfer?.items;
    if (items?.length) {
      await handleDropItems(items);
      return;
    }

    if (e.dataTransfer?.files?.length) {
      await handleFileList(e.dataTransfer.files);
    }
  };

  const removeFile = (file) => {
    onChange(sources.filter((s) => {
      if (!s.source?.trim()) return true;
      return s.className !== file.className || s.fileName !== file.fileName;
    }));
  };

  const openFolderPicker = (e) => {
    e?.stopPropagation();
    folderInputRef.current?.click();
  };

  const openFilePicker = (e) => {
    e?.stopPropagation();
    fileInputRef.current?.click();
  };

  const dropZoneClass = `rounded-xl border-2 border-dashed transition-all duration-200 ${
    isDragging
      ? 'border-purple-400 bg-purple-500/10 scale-[1.01]'
      : 'border-gray-300 bg-gray-50 hover:border-purple-400/60 hover:bg-purple-500/5 dark:border-gray-700 dark:bg-[#0d1117] dark:hover:border-purple-500/40'
  }`;

  return (
    <div
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') openFolderPicker();
      }}
      onDragEnter={(e) => { e.preventDefault(); setIsDragging(true); }}
      onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
      onDragLeave={(e) => {
        e.preventDefault();
        if (!e.currentTarget.contains(e.relatedTarget)) setIsDragging(false);
      }}
      onDrop={onDrop}
      onClick={openFolderPicker}
      className={`cursor-pointer ${dropZoneClass}`}
    >
      <input
        ref={folderInputRef}
        type="file"
        multiple
        className="hidden"
        onChange={(e) => {
          if (e.target.files?.length) handleFileList(e.target.files);
          e.target.value = '';
        }}
        webkitdirectory=""
        directory=""
      />
      <input
        ref={fileInputRef}
        type="file"
        accept=".java,text/x-java-source,application/java"
        multiple
        className="hidden"
        onChange={(e) => {
          if (e.target.files?.length) handleFileList(e.target.files);
          e.target.value = '';
        }}
      />

      {!hasFiles ? (
        <div className="px-4 py-8 text-center">
          <Upload className={`mx-auto mb-3 h-8 w-8 transition-colors ${isDragging ? 'text-purple-400' : 'text-gray-400'}`} />
          <p className="text-sm font-medium text-gray-700 dark:text-gray-200">
            Drop a challenge folder or Java files here
          </p>
          <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
            All <code className="text-purple-400">.java</code> files inside the folder are loaded automatically
          </p>
          <button
            type="button"
            onClick={openFolderPicker}
            className="mt-4 inline-flex items-center gap-1.5 rounded-lg bg-purple-600/20 px-3 py-1.5 text-xs font-medium text-purple-300 transition-colors hover:bg-purple-600/30"
          >
            <FolderOpen className="h-3.5 w-3.5" />
            Choose folder
          </button>
          <button
            type="button"
            onClick={openFilePicker}
            className="ml-2 mt-4 inline-flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs text-gray-500 transition-colors hover:text-purple-400"
          >
            or pick .java files
          </button>
        </div>
      ) : (
        <div className="p-4">
          <div className="grid gap-2 sm:grid-cols-2">
            {loadedSources.map((file, idx) => (
              <FileChip
                key={`${file.className}-${file.fileName || idx}`}
                file={file}
                onRemove={removeFile}
              />
            ))}
            <button
              type="button"
              onClick={openFolderPicker}
              className="flex min-h-[3.25rem] items-center justify-center gap-2 rounded-lg border border-dashed border-gray-300 px-3 py-2 text-xs text-gray-500 transition-colors hover:border-purple-400/50 hover:bg-purple-500/5 hover:text-purple-400 dark:border-gray-600 dark:hover:border-purple-500/40"
            >
              <Plus className="h-4 w-4" />
              Add folder / files
            </button>
          </div>
          <p className="mt-3 text-center text-[11px] text-gray-500 dark:text-gray-500">
            Drop a folder to load every <code className="text-purple-400/80">.java</code> file, or click to choose
          </p>
        </div>
      )}
    </div>
  );
}
