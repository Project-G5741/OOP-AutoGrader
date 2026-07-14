export default function Card({ children, className = ""}) {
    return (
        <div className={`bg-white dark:bg-[#13131A] rounded-xl p-6 shadow-sm dark:shadow-none border border-gray-100 dark:border-transparent transition-colors ${className}`}>
            {children}
        </div>
    );
}