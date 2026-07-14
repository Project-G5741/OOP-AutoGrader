import { Upload } from 'lucide-react';
import Button from './Button';

export default function DropZone({ fileType, colorTheme = "purple" }) {
  const isPurple = colorTheme === "purple";
  const borderClass = isPurple ? "border-purple-500/30" : "border-green-500/30";
  const iconBg = isPurple ? "bg-purple-500/10" : "bg-green-500/10";
  const iconColor = isPurple ? "text-purple-500" : "text-green-500";
  const btnClass = isPurple ? "bg-purple-600 hover:bg-purple-700 text-white" : "bg-green-600 hover:bg-green-700 text-white";

  return (
    <div className={`bg-white dark:bg-[#13131A] rounded-xl p-6 border-2 border-dashed ${borderClass} shadow-sm dark:shadow-none`}>
      <div className="flex flex-col items-center justify-center py-8">
        <div className={`w-16 h-16 ${iconBg} rounded-full flex items-center justify-center mb-4`}>
          <Upload className={`w-8 h-8 ${iconColor}`} />
        </div>
        <h3 className="text-gray-900 dark:text-white mb-2">Drop {fileType} files here</h3>
        <p className="text-gray-500 dark:text-gray-400 text-sm mb-4">or click to upload</p>
        <Button className={btnClass}>+ Select project</Button>
      </div>
    </div>
  );
}