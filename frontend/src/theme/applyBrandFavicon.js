import { brand } from './brand';

/** Keep the tab icon in sync when brand.assets changes at runtime (dev HMR). */
export function applyBrandFavicon() {
  const { url, type } = brand.favicon;
  if (!url) return;

  const ensureLink = (rel) => {
    let link = document.querySelector(`link[rel="${rel}"]`);
    if (!link) {
      link = document.createElement('link');
      link.rel = rel;
      document.head.appendChild(link);
    }
    return link;
  };

  const icon = ensureLink('icon');
  icon.href = url;
  if (type) icon.type = type;

  if (type === 'image/png' || type === 'image/jpeg' || type === 'image/webp') {
    const touch = ensureLink('apple-touch-icon');
    touch.href = url;
  }
}
