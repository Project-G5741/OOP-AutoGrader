import { Upload } from 'lucide-react';
import { useRef, useState } from 'react';
import Button from './Button';

// TODO: replace with your real backend endpoint
const UPLOAD_URL = '/api/upload';

export default function DropZone({
  title = "Drop or drag your folder here",
  buttonText = "Select Folder",
  onFilesSelected
}) {
  const inputRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);

  // Recursively walks a dropped FileSystemEntry tree (used for drag & drop,
  // since dataTransfer.files alone does not see into subfolders).
  // Collects { file, relativePath } for every file found.
  const walkEntry = (entry, pathPrefix, collected) => {
    return new Promise((resolve, reject) => {
      if (entry.isFile) {
        entry.file((file) => {
          collected.push({ file, relativePath: pathPrefix + file.name });
          resolve();
        }, reject);
      } else if (entry.isDirectory) {
        const reader = entry.createReader();
        // readEntries() only returns a batch at a time, so it must be
        // called repeatedly until it returns an empty array.
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

  // Drag-and-drop path: walk the full folder tree.
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

  // Click-to-select path: the browser already gives each file a
  // webkitRelativePath (e.g. "my-folder/sub/file.txt"), no walking needed.
  const handleInputFiles = (fileList) => {
    const collected = Array.from(fileList).map((file) => ({
      file,
      relativePath: file.webkitRelativePath || file.name,
    }));
    handleFiles(collected);
  };

  // Common handler for both paths: entries = [{ file, relativePath }]
  const handleFiles = (entries) => {
    console.log(`Dropped folder contains ${entries.length} file(s):`);
    entries.forEach(({ relativePath, file }) => {
      console.log(`  ${relativePath}  (${file.size} bytes)`);
    });

    if (onFilesSelected) {
      onFilesSelected(entries.map((e) => e.file));
    }

    uploadFiles(entries);
  };

  // Placeholder upload. Each file is appended with its relative path as the
  // filename so the backend can reconstruct the folder structure
  const uploadFiles = async (entries) => {
    setIsUploading(true);
    try {
      const formData = new FormData();
      entries.forEach(({ file, relativePath }) => {
        formData.append('files', file, relativePath);
      });

      const res = await fetch(UPLOAD_URL, {
        method: 'POST',
        body: formData,
      });

      if (!res.ok) {
        throw new Error(`Upload failed with status ${res.status}`);
      }

      console.log('Upload successful');
    } catch (err) {
      // TODO: surface this to the UI instead of just logging
      console.error('Upload error:', err);
    } finally {
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

        <Button
          className="bg-purple-600 hover:bg-purple-700 text-white"
          onClick={() => inputRef.current?.click()}
          disabled={isUploading}
        >
          {buttonText}
        </Button>

        {/* Hidden Input */}
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