export default function Card({ children, className = ""}) {
    return (
        <div className={`bg-surface rounded-xl p-6 shadow-sm dark:shadow-none border border-border transition-colors ${className}`}>
            {children}
        </div>
    );
}