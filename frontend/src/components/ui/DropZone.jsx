import { Upload, Info } from 'lucide-react';
import { useRef, useState } from 'react';
import Button from './Button';
import { readFriendlyApiError, toFriendlyError } from '../../utils/apiError';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

export default function DropZone({
  title = "Drop or drag your folder here",
  buttonText = "Select Folder",
  onFilesSelected,
  onUploadComplete,
  labId,
  attemptNumber,
  authToken,
}) {
  const inputRef = useRef(null);
  const uploadInFlightRef = useRef(false);
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadError, setUploadError] = useState(null);

  // Recursively walks a dropped FileSystemEntry tree (used for drag & drop,
  // since dataTransfer.files alone does not see into subfolders).
  const walkEntry = (entry, pathPrefix, collected) => {
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
  };

  const handleDropItems = async (items) => {
    const topLevelEntries = Array.from(items)
      .map((item) => item.webkitGetAsEntry())
      .filter(Boolean);

    const collected = [];
    for (const entry of topLevelEntries) {
      await walkEntry(entry, '', collected);
    }
    handleFiles(collected);
  };

  const handleInputFiles = (fileList) => {
    const collected = Array.from(fileList).map((file) => ({
      file,
      relativePath: file.webkitRelativePath || file.name,
    }));
    handleFiles(collected);
    if (inputRef.current) {
      inputRef.current.value = '';
    }
  };

  const isValidFolderStructure = (entries) => {
    const filePaths = entries.map(({ relativePath }) => relativePath);
    if (filePaths.length === 0) {
      return false;
    }

    const rootFolder = filePaths
      .map((path) => path.split('/').filter(Boolean)[0])
      .find(Boolean);

    if (!rootFolder) {
      return false;
    }

    const rootPattern = /^(\d+)_([a-z0-9_\s]+)_lab_(\d+)$/i;
    if (!rootPattern.test(rootFolder)) {
      return false;
    }

    const challengeFolders = new Set(
      filePaths
        .map((path) => path.split('/').filter(Boolean))
        .filter((segments) => segments.length >= 2 && segments[1] !== '.git')
        .map((segments) => segments[1])
    );

    return Array.from(challengeFolders).every((name) => /^(challenge[_-]?\d+)$/i.test(name));
  };

  const handleFiles = (entries) => {
    const relevant = entries.filter(({ relativePath }) => {
      const lower = relativePath.toLowerCase();
      const segments = relativePath.split('/').filter(Boolean);
      return lower.endsWith('.mmd') || lower.endsWith('.java') || segments.includes('.git');
    });

    if (relevant.length === 0) {
      setUploadError('Please drop a valid project folder with .mmd/.java files inside challenge folders.');
      return;
    }

    if (!isValidFolderStructure(relevant)) {
      setUploadError("Invalid folder structure. Expected a root folder named like 'IRN_StudentName_lab_1' with challenge folders named 'challenge_1'.");
      return;
    }

    if (onFilesSelected) {
      onFilesSelected(relevant.map((e) => e.file));
    }

    uploadFiles(relevant);
  };

  const uploadFiles = async (entries) => {
    if (isUploading || uploadInFlightRef.current) return;
    if (!labId || !attemptNumber) {
      console.error('DropZone: labId/attemptNumber not provided, cannot upload.');
      setUploadError('Missing lab or attempt info — cannot upload.');
      return;
    }
    if (!authToken) {
      console.error('DropZone: authToken not provided, cannot upload.');
      setUploadError('You must be signed in to upload.');
      return;
    }

    uploadInFlightRef.current = true;
    setIsUploading(true);
    setUploadError(null);
    try {
      const formData = new FormData();
      entries.forEach(({ file, relativePath }) => {
        formData.append('files', file, relativePath);
      });

      // Backend upserts on (user, lab, attemptNumber) - this must be the
      // next unused attempt number for this student+lab.
      const attemptForUpload = attemptNumber;
      const res = await fetch(`${API_BASE}/api/submissions/${labId}/${attemptForUpload}/upload`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${authToken}`,
        },
        body: formData,
      });

      if (!res.ok) {
        throw new Error(await readFriendlyApiError(res, 'upload'));
      }

      const data = await res.json();
      if (onUploadComplete) {
        onUploadComplete(data);
      }
    } catch (err) {
      console.error('Upload error:', err);
      setUploadError(toFriendlyError(err, 'upload'));
    } finally {
      uploadInFlightRef.current = false;
      setIsUploading(false);
    }
  };

  return (
    <div className="space-y-3">
      <div className="rounded-lg border border-border bg-info-bg px-4 py-3 text-sm text-info-text dark:border-surface-tertiary">
        <div className="flex items-start gap-2">
          <Info className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
          <p className="text-xs leading-relaxed">
            <span className="font-semibold">Folder format:</span>{' '}
            <span className="font-mono">IRN_YourName_lab_n</span>
            {' / '}
            (<span className="font-mono">challenge_1</span>, <span className="font-mono">challenge_2</span>, …)
            {' '}(<span className="font-mono">.java</span> and <span className="font-mono">.mmd</span> inside each;
            include the project <span className="font-mono">.git</span> folder if you have one)
          </p>
        </div>
      </div>

      <div
      className={`
        bg-surface
        rounded-xl
        p-6
        border
        border-dashed
        border-border
        dark:border-surface-tertiary
        shadow-sm
        dark:shadow-none
        transition-all
        duration-200
        ${
          isDragging
            ? "bg-primary-light dark:border-primary-text"
            : ""
        }
      `}
      onDragOver={(e) => {
        e.preventDefault();
        setIsDragging(true);
      }}
      onDragLeave={(e) => {
        e.preventDefault();
        setIsDragging(false);
      }}
      onDrop={(e) => {
        e.preventDefault();
        setIsDragging(false);

        if (e.dataTransfer.items && e.dataTransfer.items.length > 0) {
          handleDropItems(e.dataTransfer.items);
        }
      }}
    >
      <div className="flex flex-col items-center justify-center py-8">
        <div className="w-16 h-16 bg-primary-light rounded-full flex items-center justify-center mb-4">
          <Upload className="w-8 h-8 text-primary" />
        </div>

        <h3 className="text-foreground mb-2">
          {title}
        </h3>

        <p className="text-foreground-muted text-sm mb-4">
          or click to upload
        </p>

        {uploadError && (
          <pre className="text-error-text text-sm mb-4 max-w-full whitespace-pre-wrap text-left overflow-x-auto">
            {uploadError}
          </pre>
        )}

        <Button
          className="inline-flex items-center justify-center gap-2 bg-primary hover:bg-primary-hover text-white"
          onClick={() => inputRef.current?.click()}
          disabled={isUploading}
        >
          {isUploading && (
            <span
              className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent"
              aria-hidden
            />
          )}
          {isUploading ? 'Uploading...' : buttonText}
        </Button>

        <input
          ref={inputRef}
          type="file"
          hidden
          multiple
          webkitdirectory=""
          directory=""
          onChange={(e) => {
            handleInputFiles(e.target.files);
          }}
        />
      </div>
    </div>
    </div>
  );
}