import { useEffect, useRef, useState } from 'react';
import { Download, ChevronDown } from 'lucide-react';

export default function ExportMenu({ onExport, disabled = false, label = 'Export' }) {
  const [open, setOpen] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = async (format) => {
    setOpen(false);
    await onExport?.(format);
  };

  return (
    <div className="relative inline-flex" ref={menuRef}>
      <button
        type="button"
        disabled={disabled}
        onClick={() => setOpen((current) => !current)}
        className="inline-flex items-center gap-2 rounded-md bg-purple-600 px-3 py-2 text-sm text-white disabled:cursor-not-allowed disabled:opacity-50"
      >
        <Download className="h-4 w-4" />
        {label}
        <ChevronDown className="h-4 w-4" />
      </button>
      {open && (
        <div className="absolute right-0 top-full z-20 mt-2 min-w-[9rem] overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg dark:border-gray-700 dark:bg-[#1e2530]">
          {['excel', 'pdf', 'svg'].map((format) => (
            <button
              key={format}
              type="button"
              onClick={() => handleSelect(format)}
              className="block w-full px-4 py-2 text-left text-sm text-gray-700 hover:bg-purple-50 dark:text-gray-200 dark:hover:bg-purple-900/20"
            >
              {format.toUpperCase()}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
