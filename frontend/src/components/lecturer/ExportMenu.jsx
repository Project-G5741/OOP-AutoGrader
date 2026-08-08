import { useEffect, useRef, useState } from 'react';
import { Download, ChevronDown } from 'lucide-react';

const MENU_ESTIMATE_HEIGHT = 132;

export default function ExportMenu({ onExport, disabled = false, label = 'Export', dropUp = false }) {
  const [open, setOpen] = useState(false);
  const [openUpward, setOpenUpward] = useState(dropUp);
  const menuRef = useRef(null);
  const buttonRef = useRef(null);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleToggle = () => {
    if (!open && buttonRef.current) {
      const rect = buttonRef.current.getBoundingClientRect();
      const spaceBelow = window.innerHeight - rect.bottom;
      setOpenUpward(dropUp || spaceBelow < MENU_ESTIMATE_HEIGHT);
    }
    setOpen((current) => !current);
  };

  const handleSelect = async (format) => {
    setOpen(false);
    await onExport?.(format);
  };

  const menuPositionClass = openUpward
    ? 'bottom-full mb-2'
    : 'top-full mt-2';

  return (
    <div className="relative inline-flex" ref={menuRef}>
      <button
        ref={buttonRef}
        type="button"
        disabled={disabled}
        onClick={handleToggle}
        className="inline-flex items-center gap-2 rounded-md bg-purple-600 px-3 py-2 text-sm text-white disabled:cursor-not-allowed disabled:opacity-50"
      >
        <Download className="h-4 w-4" />
        {label}
        <ChevronDown className="h-4 w-4" />
      </button>
      {open && (
        <div
          className={`absolute right-0 z-50 min-w-[9rem] overflow-hidden rounded-lg border border-gray-200 bg-white shadow-lg dark:border-gray-700 dark:bg-[#1e2530] ${menuPositionClass}`}
        >
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
