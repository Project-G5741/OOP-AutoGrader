import { useEffect, useState } from 'react';
import { Mascot } from 'page-mascot';
import ModalOverlay from '../ui/ModalOverlay';
import { useToast } from '../ui/Toast';
import { apiFetch } from '../../utils/apiFetch';
import { authHeaders } from '../../utils/authHeaders';
import { friendlyLoadErrorFromResponse, toFriendlyError } from '../../utils/apiError';
import { formatNumber } from '../../utils/formatters';
import { mascotArt } from './studentMascots';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';

function shopFailure(code) {
  if (code === 'INSUFFICIENT_POINTS') return 'Not enough points for this mascot.';
  if (code === 'ALREADY_OWNED') return 'You already own this mascot.';
  if (code === 'NOT_OWNED') return 'Choose a mascot you own.';
  return 'Unable to update your mascot. Please try again.';
}

async function readShopFailure(response) {
  let code = '';
  try {
    const body = await response.clone().json();
    code = body?.message || '';
  } catch {
    /* keep the friendly mapper */
  }
  if (response.status === 409 || response.status === 404) {
    return shopFailure(code);
  }
  return friendlyLoadErrorFromResponse(response);
}

export default function MascotShop({ open, onClose, onUpdated }) {
  const showToast = useToast();
  const [shop, setShop] = useState(null);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [pending, setPending] = useState(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!open) {
      setPending(null);
      return undefined;
    }
    let cancelled = false;
    setLoading(true);
    setLoadError('');
    apiFetch(`${API_BASE}/api/students/mascots/list`, { headers: authHeaders() })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(await friendlyLoadErrorFromResponse(response));
        }
        return response.json();
      })
      .then((next) => {
        if (!cancelled) setShop(next);
      })
      .catch((error) => {
        if (cancelled) return;
        setLoadError(error?.message || 'Unable to load the mascot shop. Please try again.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [open]);

  if (!open) return null;

  const points = shop?.points ?? 0;

  const apply = (next, message) => {
    setShop(next);
    onUpdated?.(next);
    showToast(message, 'success');
  };

  const purchase = async () => {
    if (!pending || busy) return;
    setBusy(true);
    try {
      const response = await apiFetch(`${API_BASE}/api/students/mascots/${pending.id}/purchase`, {
        method: 'POST',
        headers: authHeaders(),
      });
      if (!response.ok) {
        showToast(await readShopFailure(response), 'error');
        return;
      }
      const next = await response.json();
      setPending(null);
      apply(next, `Purchased ${pending.name}. ${formatNumber(next.points)} points left.`);
    } catch (error) {
      showToast(toFriendlyError(error), 'error');
    } finally {
      setBusy(false);
    }
  };

  const equip = async (offer) => {
    if (busy || offer.equipped) return;
    setBusy(true);
    try {
      const response = await apiFetch(`${API_BASE}/api/students/mascots/${offer.id}/equip`, {
        method: 'POST',
        headers: authHeaders(),
      });
      if (!response.ok) {
        showToast(await readShopFailure(response), 'error');
        return;
      }
      const next = await response.json();
      apply(next, `Now using ${offer.name}.`);
    } catch (error) {
      showToast(toFriendlyError(error), 'error');
    } finally {
      setBusy(false);
    }
  };

  return (
    <ModalOverlay onBackdropClick={pending ? undefined : onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="mascot-shop-title"
        className="flex max-h-[min(90vh,52rem)] w-full max-w-3xl flex-col overflow-hidden rounded-3xl border border-border bg-surface shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-4 border-b border-border px-5 py-4 sm:px-6">
          <div>
            <h2 id="mascot-shop-title" className="text-xl font-semibold text-foreground">Mascot shop</h2>
            <p className="mt-1 text-sm text-foreground-muted">Spend reward points to wear a different mascot.</p>
          </div>
          <div className="flex items-center gap-2">
            <div className="rounded-full bg-warning-bg px-3 py-1.5 text-sm font-semibold tabular-nums text-warning-text">
              {formatNumber(points)} pts
            </div>
            <button
              type="button"
              onClick={onClose}
              className="cursor-pointer rounded-lg px-2 py-1 text-sm text-foreground-secondary hover:text-foreground focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              Close
            </button>
          </div>
        </div>

        <div className="overflow-y-auto px-5 py-4 sm:px-6">
          {loading && <p className="py-10 text-center text-sm text-foreground-muted">Loading shop…</p>}
          {loadError && <p className="py-10 text-center text-sm text-error-text">{loadError}</p>}
          {shop && (
            <ul className="grid grid-cols-1 gap-4 min-[480px]:grid-cols-2 lg:grid-cols-3">
              {shop.mascots.map((offer) => {
                const art = mascotArt(offer.id);
                const shortfall = Math.max(0, offer.price - points);
                const canBuy = !offer.owned && shortfall === 0;
                return (
                  <li
                    key={offer.id}
                    className={`flex flex-col rounded-3xl border bg-surface p-3 ${
                      offer.equipped ? 'border-primary ring-2 ring-primary' : 'border-border'
                    }`}
                  >
                    <div className="relative flex h-40 items-center justify-center rounded-2xl bg-surface-secondary">
                      {offer.equipped && (
                        <span className="absolute right-3 top-3 rounded-full bg-primary px-2 py-0.5 text-xs font-semibold text-white">
                          Using
                        </span>
                      )}
                      <Mascot
                        size={120}
                        label={`${offer.name} mascot`}
                        directions={art.directions}
                        reactions={art.reactions}
                      />
                    </div>
                    <div className="mt-3 flex items-baseline justify-between gap-2 px-1">
                      <h3 className="text-base font-semibold text-foreground">{offer.name}</h3>
                      <span className={`text-sm font-semibold tabular-nums ${offer.starter ? 'text-foreground-muted' : 'text-warning-text'}`}>
                        {offer.starter ? 'Included' : `${formatNumber(offer.price)} pts`}
                      </span>
                    </div>
                    {offer.owned ? (
                      <button
                        type="button"
                        disabled={offer.equipped || busy}
                        onClick={() => equip(offer)}
                        className="mt-3 min-h-11 w-full cursor-pointer rounded-xl border border-border bg-surface-secondary text-sm font-semibold text-foreground transition-colors hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:cursor-default disabled:opacity-70"
                      >
                        {offer.equipped ? 'Using' : 'Use'}
                      </button>
                    ) : (
                      <button
                        type="button"
                        disabled={!canBuy || busy}
                        onClick={() => setPending(offer)}
                        className="mt-3 min-h-11 w-full cursor-pointer rounded-xl bg-primary text-sm font-semibold text-white transition-colors hover:bg-primary-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:cursor-not-allowed disabled:opacity-60"
                      >
                        {canBuy ? 'Purchase' : `Need ${formatNumber(shortfall)} more`}
                      </button>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>

      {pending && (
        <ModalOverlay onBackdropClick={busy ? undefined : () => setPending(null)}>
          <div
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="mascot-buy-title"
            aria-describedby="mascot-buy-body"
            className="w-full max-w-md rounded-3xl border border-border bg-surface p-6 shadow-xl"
            onClick={(event) => event.stopPropagation()}
          >
            <h3 id="mascot-buy-title" className="text-lg font-semibold text-foreground">
              Purchase {pending.name}?
            </h3>
            <p id="mascot-buy-body" className="mt-2 text-sm text-foreground-secondary">
              This spends {formatNumber(pending.price)} points. Your balance will be {formatNumber(points - pending.price)} {points - pending.price === 1 ? 'point' : 'points'}.
            </p>
            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                disabled={busy}
                onClick={() => setPending(null)}
                className="min-h-11 cursor-pointer rounded-xl border border-border bg-surface-secondary px-4 text-sm font-semibold text-foreground hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:opacity-60"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={busy}
                onClick={purchase}
                className="min-h-11 cursor-pointer rounded-xl bg-primary px-4 text-sm font-semibold text-white hover:bg-primary-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:opacity-60"
              >
                {busy ? 'Purchasing…' : 'Purchase'}
              </button>
            </div>
          </div>
        </ModalOverlay>
      )}
    </ModalOverlay>
  );
}
