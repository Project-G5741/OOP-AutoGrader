import { Upload } from 'lucide-react';
import { useRef, useState } from 'react';
import Button from './Button';
import { readApiErrorMessage } from '../../utils/apiError';

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
        .filter((segments) => segments.length >= 2)
        .map((segments) => segments[1])
    );

    return Array.from(challengeFolders).every((name) => /^(challenge[_-]?\d+)$/i.test(name));
  };

  const handleFiles = (entries) => {
    const relevant = entries.filter(({ relativePath }) => {
      const lower = relativePath.toLowerCase();
      return lower.endsWith('.mmd') || lower.endsWith('.java');
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
        const message = await readApiErrorMessage(res, `Upload failed with status ${res.status}`);
        throw new Error(message);
      }

      const data = await res.json();
      if (onUploadComplete) {
        onUploadComplete(data);
      }
    } catch (err) {
      console.error('Upload error:', err);
      setUploadError(err.message || 'Upload failed. Please try again.');
    } finally {
      uploadInFlightRef.current = false;
      setIsUploading(false);
    }
  };

  return (
    <div
      className={`
        bg-white dark:bg-[#13131A]
        rounded-xl
        p-6
        border-2
        border-dashed
        shadow-sm
        dark:shadow-none
        transition-all
        duration-200
        ${
          isDragging
            ? "border-purple-500 bg-purple-50 dark:bg-purple-900/10"
            : "border-purple-500/30"
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
        <div className="w-16 h-16 bg-purple-500/10 rounded-full flex items-center justify-center mb-4">
          <Upload className="w-8 h-8 text-purple-500" />
        </div>

        <h3 className="text-gray-900 dark:text-white mb-2">
          {title}
        </h3>

        <p className="text-gray-500 dark:text-gray-400 text-sm mb-4">
          {isUploading ? "Uploading..." : "or click to upload"}
        </p>

        {uploadError && (
          <pre className="text-red-500 text-sm mb-4 max-w-full whitespace-pre-wrap text-left overflow-x-auto">
            {uploadError}
          </pre>
        )}

        <Button
          className="bg-purple-600 hover:bg-purple-700 text-white"
          onClick={() => inputRef.current?.click()}
          disabled={isUploading}
        >
          {buttonText}
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
  );
}