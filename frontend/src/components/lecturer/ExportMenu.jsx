import { useEffect, useRef, useState } from 'react';
import { Download, ChevronDown } from 'lucide-react';

const MENU_ESTIMATE_HEIGHT = 132;

const DEFAULT_FORMATS = ['excel', 'pdf', 'svg'];

const FORMAT_LABELS = {
  excel: 'Excel',
  pdf: 'PDF',
  csv: 'CSV',
  svg: 'SVG',
};

export default function ExportMenu({
  onExport,
  disabled = false,
  label = 'Export',
  dropUp = false,
  formats = DEFAULT_FORMATS,
}) {
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
    <div className="relative w-36 shrink-0" ref={menuRef}>
      <button
        ref={buttonRef}
        type="button"
        disabled={disabled}
        onClick={handleToggle}
        className="flex w-full items-center justify-center gap-2 rounded-md bg-primary px-3 py-2 text-sm text-white disabled:cursor-not-allowed disabled:opacity-50"
      >
        <Download className="h-4 w-4 shrink-0" />
        {label}
        <ChevronDown className="h-4 w-4 shrink-0" />
      </button>
      {open && (
        <div
          className={`absolute left-0 right-0 z-50 w-full overflow-hidden rounded-lg border border-border bg-surface shadow-lg ${menuPositionClass}`}
        >
          {formats.map((format) => (
            <button
              key={format}
              type="button"
              onClick={() => handleSelect(format)}
              className="block w-full px-4 py-2 text-left text-sm text-foreground-secondary hover:bg-primary-light"
            >
              {FORMAT_LABELS[format] ?? format}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
