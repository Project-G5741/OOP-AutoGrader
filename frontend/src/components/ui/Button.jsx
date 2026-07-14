export default function Button({ children, className = "", onClick, type = "button" }) {
  return (
    <button
      type={type}
      onClick={onClick}
      className={`px-6 py-2 rounded-lg font-medium transition-all duration-200 ${className}`}
    >
      {children}
    </button>
  );
}