import { brand } from './brand';

function applyFromConfig({ url, type }) {
  if (!url) {
    document.querySelectorAll('link[rel="icon"], link[rel="apple-touch-icon"]').forEach((link) => link.remove());
    return;
  }

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

  const isRaster = type === 'image/png' || type === 'image/jpeg' || type === 'image/webp';
  const touch = document.querySelector('link[rel="apple-touch-icon"]');

  if (isRaster) {
    const touchLink = touch ?? ensureLink('apple-touch-icon');
    touchLink.href = url;
  } else if (touch) {
    touch.remove();
  }
}

/** Apply generated favicon at startup; re-apply when brand assets hot-reload in dev. */
export function applyBrandFavicon(brandConfig = brand) {
  applyFromConfig(brandConfig.favicon);
}

if (import.meta.hot) {
  import.meta.hot.accept('./brand.assets.generated.js', () => {
    import('./brand.js').then(({ brand: freshBrand }) => applyBrandFavicon(freshBrand));
  });
}
