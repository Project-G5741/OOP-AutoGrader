import { Upload } from 'lucide-react';
import { useRef, useState } from 'react';
import Button from './Button';

export default function DropZone({
  title = "Drop or drag your file",
  buttonText = "Select Project",
  onFilesSelected
}) {
  const inputRef = useRef(null);
  const [isDragging, setIsDragging] = useState(false);

  const handleFiles = (fileList) => {
    const files = Array.from(fileList);

    console.log("Selected Files:", files);

    // Sau này StudentDashboard sẽ nhận được danh sách file
    if (onFilesSelected) {
      onFilesSelected(files);
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

        if (e.dataTransfer.files.length > 0) {
          handleFiles(e.dataTransfer.files);
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
          or click to upload
        </p>

        <Button
          className="bg-purple-600 hover:bg-purple-700 text-white"
          onClick={() => inputRef.current?.click()}
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
            handleFiles(e.target.files);
          }}
        />
      </div>
    </div>
  );
}