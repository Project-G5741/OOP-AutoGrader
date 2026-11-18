import { useEffect, useState, useMemo } from 'react';
import { Mascot } from './MascotSprite';
import {
  Sparkles,
  Gem,
  Filter,
  Coins,
  Ticket,
  Search,
  ArrowUpDown,
  ArrowUp01,
  ArrowDown01,
  ArrowDownAZ,
  ArrowUpZA,
} from 'lucide-react';
import ModalOverlay from '../ui/ModalOverlay';
import Combobox from '../ui/Combobox';
import { useToast } from '../ui/Toast';
import { apiFetch } from '../../utils/apiFetch';
import { authHeaders } from '../../utils/authHeaders';
import { friendlyLoadErrorFromResponse, toFriendlyError } from '../../utils/apiError';
import { formatNumber } from '../../utils/formatters';
import { mascotArt, listCatalogMascots, mascotOfferVisibleInShop, STUDENT_MASCOTS, MASCOT_TIERS } from './studentMascots';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
const POINTS_PER_CRYSTAL = 4000;

const SORT_OPTIONS = [
  { value: 'price-asc', label: 'Price: Low → High', icon: ArrowUp01 },
  { value: 'price-desc', label: 'Price: High → Low', icon: ArrowDown01 },
  { value: 'name-asc', label: 'Name: A → Z', icon: ArrowDownAZ },
  { value: 'name-desc', label: 'Name: Z → A', icon: ArrowUpZA },
];

function compareShopOffers(a, b, sortBy) {
  const tailA = a.exclusive ? 1 : 0;
  const tailB = b.exclusive ? 1 : 0;
  if (tailA !== tailB) return tailA - tailB;

  const priceA = Number(a.price) || 0;
  const priceB = Number(b.price) || 0;
  const nameA = String(a.name || '').localeCompare(String(b.name || ''), undefined, { sensitivity: 'base' });

  if (sortBy === 'price-desc') {
    if (priceB !== priceA) return priceB - priceA;
    return nameA;
  }
  if (sortBy === 'name-asc') return nameA;
  if (sortBy === 'name-desc') return -nameA;
  // default: price-asc
  if (priceA !== priceB) return priceA - priceB;
  return nameA;
}

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

export default function MascotShop({ 
  open, 
  onClose, 
  onUpdated, 
  onOpenGacha,
  crystals = 0,
  onCrystalsChange,
  onCrystalsReconcile,
  tickets,
  ownedMascots = ['fox'],
  onMascotsRefresh,
}) {
  const showToast = useToast();
  const [shop, setShop] = useState(null);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [pending, setPending] = useState(null);
  const [paymentChoice, setPaymentChoice] = useState('crystal'); // 'crystal' | 'points'
  const [busy, setBusy] = useState(false);
  const [selectedTier, setSelectedTier] = useState('all'); // 'all' | 'normal' | 'rare' | 'epic' | 'legendary'
  const [sortBy, setSortBy] = useState('price-asc');
  const [searchQuery, setSearchQuery] = useState('');
  const [localEquipped, setLocalEquipped] = useState(null);
  const [exchangeOpen, setExchangeOpen] = useState(false);
  const [exchangeCount, setExchangeCount] = useState(1);

  useEffect(() => {
    if (!open) {
      setPending(null);
      setExchangeOpen(false);
      setSearchQuery('');
      setSortBy('price-asc');
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
        if (cancelled) return;
        setShop(next);
        onUpdated?.(next);
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
  }, [open, onUpdated]);

  // Combine backend shop list with catalog definitions, then filter / sort for display
  const displayedMascots = useMemo(() => {
    const backendMascots = shop?.mascots || [];
    const backendMap = new Map(backendMascots.map((m) => [m.id, m]));
    const query = searchQuery.trim().toLowerCase();

    const all = listCatalogMascots().map((catalogItem) => {
      const backendItem = backendMap.get(catalogItem.id);
      const isOwnedLocally = ownedMascots.includes(catalogItem.id);
      const isEquippedLocally = localEquipped === catalogItem.id || (!localEquipped && (backendItem?.equipped || shop?.equipped === catalogItem.id));

      return {
        ...catalogItem,
        owned: backendItem ? backendItem.owned || isOwnedLocally : catalogItem.starter || isOwnedLocally,
        equipped: isEquippedLocally,
        price: backendItem ? backendItem.price : catalogItem.price,
      };
    });

    const filtered = all.filter((item) => {
      if (!mascotOfferVisibleInShop(item, backendMap)) return false;
      if (selectedTier !== 'all' && item.tier !== selectedTier) return false;
      if (!query) return true;
      const name = String(item.name || '').toLowerCase();
      const id = String(item.id || '').toLowerCase();
      return name.includes(query) || id.includes(query);
    });

    return filtered.slice().sort((a, b) => compareShopOffers(a, b, sortBy));
  }, [shop, selectedTier, sortBy, searchQuery, ownedMascots, localEquipped]);

  if (!open) return null;

  const points = shop?.points ?? 0;

  const apply = (next, message) => {
    setShop(next);
    onUpdated?.(next);
    showToast(message, 'success');
  };

  const handleOpenPurchase = (offer) => {
    setPending(offer);
    const canPayWithCrystal = offer.crystalCost && crystals >= offer.crystalCost;
    setPaymentChoice(canPayWithCrystal ? 'crystal' : 'points');
  };

  const executePurchase = async () => {
    if (!pending || busy) return;
    setBusy(true);

    try {
      if (paymentChoice === 'crystal' && pending.crystalCost) {
        if (crystals < pending.crystalCost) {
          showToast('Not enough Crystals to redeem!', 'error');
          return;
        }

        const spendResponse = await apiFetch(`${API_BASE}/api/students/streak/crystals/spend`, {
          method: 'POST',
          headers: { ...authHeaders(), 'Content-Type': 'application/json' },
          body: JSON.stringify({ count: pending.crystalCost }),
        });
        if (!spendResponse.ok) {
          showToast(await readShopFailure(spendResponse), 'error');
          return;
        }
        const spent = await spendResponse.json();
        onCrystalsReconcile?.(spent.crystals);

        const response = await apiFetch(`${API_BASE}/api/students/mascots/${pending.id}/equip`, {
          method: 'POST',
          headers: authHeaders(),
        });
        if (!response.ok) {
          showToast(await readShopFailure(response), 'error');
          onMascotsRefresh?.();
          return;
        }
        const next = await response.json();
        setPending(null);
        setLocalEquipped(pending.id);
        apply(next, `Successfully redeemed ${pending.name} for ${pending.crystalCost} Crystals!`);
        onMascotsRefresh?.();
        return;
      }

      // Exchange with Points
      const isBackendOffer = Object.keys(STUDENT_MASCOTS).includes(pending.id);
      if (isBackendOffer) {
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
        setLocalEquipped(pending.id);
        apply(next, `Purchased ${pending.name}. ${formatNumber(next.points)} pts remaining.`);
        onMascotsRefresh?.();
      } else {
        // Catalog mascot point purchase simulation
        if (points < pending.price) {
          showToast('Not enough points to buy this mascot.', 'error');
          return;
        }
        showToast('This mascot is not available for purchase yet.', 'error');
      }
    } catch (error) {
      showToast(toFriendlyError(error), 'error');
    } finally {
      setBusy(false);
    }
  };

  const confirmCrystalExchange = async () => {
    if (busy || exchangeCount < 1 || crystals < exchangeCount) return;
    setBusy(true);
    try {
      const response = await apiFetch(`${API_BASE}/api/students/streak/crystals/exchange`, {
        method: 'POST',
        headers: { ...authHeaders(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ count: exchangeCount }),
      });
      if (!response.ok) {
        const message = response.status === 409
          ? 'Not enough Crystals.'
          : await friendlyLoadErrorFromResponse(response);
        showToast(message, 'error');
        return;
      }
      const body = await response.json();
      if (typeof body.crystals === 'number') onCrystalsReconcile?.(body.crystals);
      const nextShop = {
        ...(shop || {}),
        points: body.points,
        equipped: shop?.equipped,
      };
      setExchangeOpen(false);
      apply(nextShop, `Exchanged ${exchangeCount} Crystal(s) for ${formatNumber(exchangeCount * POINTS_PER_CRYSTAL)} pts.`);
    } catch (error) {
      showToast(toFriendlyError(error), 'error');
    } finally {
      setBusy(false);
    }
  };

  const equip = async (offer) => {
    if (busy || offer.equipped) return;
    setBusy(true);
    setLocalEquipped(offer.id);

    try {
      const response = await apiFetch(`${API_BASE}/api/students/mascots/${offer.id}/equip`, {
        method: 'POST',
        headers: authHeaders(),
      });
      if (response.ok) {
        const next = await response.json();
        apply(next, `Now using ${offer.name}.`);
      } else {
        const nextShop = { ...(shop || {}), equipped: offer.id };
        apply(nextShop, `Now using ${offer.name}.`);
      }
    } catch {
      const nextShop = { ...(shop || {}), equipped: offer.id };
      apply(nextShop, `Now using ${offer.name}.`);
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
        className="flex max-h-[min(90vh,54rem)] w-full max-w-4xl flex-col overflow-hidden rounded-3xl border border-border bg-surface shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        {/* Top Header */}
        <div className="flex items-start justify-between gap-4 border-b border-border px-5 py-4 sm:px-6">
          <div>
            <div className="flex items-center gap-2">
              <h2 id="mascot-shop-title" className="text-xl font-semibold text-foreground">
                Mascot shop & Gacha
              </h2>
            </div>
          </div>

          <div className="flex items-center gap-2">
            {onOpenGacha && (
              <button
                type="button"
                onClick={() => {
                  onClose();
                  onOpenGacha();
                }}
                className="flex cursor-pointer items-center gap-1.5 rounded-full border border-primary/30 bg-primary/10 px-3 py-1.5 text-xs font-bold text-primary hover:bg-primary/20 transition-colors"
              >
                <Sparkles className="h-3.5 w-3.5" />
                <span>Open Gacha</span>
              </button>
            )}

            {tickets !== undefined && (
              <div className="flex items-center gap-1.5 rounded-full bg-surface-secondary px-3 py-1.5 text-sm font-semibold tabular-nums text-foreground border border-border">
                <Ticket className="h-4 w-4 text-primary" />
                <span>{tickets}</span>
                <span className="text-xs text-foreground-muted">Tickets</span>
              </div>
            )}

            <div className="flex items-center gap-1 rounded-full bg-surface-secondary px-3 py-1.5 text-sm font-semibold tabular-nums text-foreground border border-border">
              <Coins className="h-4 w-4 text-warning-text" />
              <span>{formatNumber(points)} pts</span>
            </div>

            <button
              type="button"
              onClick={() => {
                setExchangeCount(crystals > 0 ? 1 : 0);
                setExchangeOpen(true);
              }}
              className="flex cursor-pointer items-center gap-1.5 rounded-full border border-warning-border bg-warning-bg px-3 py-1.5 text-sm font-bold text-warning-text shadow-sm hover:opacity-90 focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              title="Exchange Crystals for points"
            >
              <Gem className="h-4 w-4" />
              <span className="tabular-nums">{crystals}</span>
              <span className="text-xs uppercase">Crystal</span>
              <span className="text-[11px] font-semibold">Exchange</span>
            </button>

            <button
              type="button"
              onClick={onClose}
              className="cursor-pointer rounded-lg px-2 py-1 text-sm text-foreground-secondary hover:text-foreground focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
            >
              Close
            </button>
          </div>
        </div>

        {/* Tier / search / sort controls */}
        <div className="flex flex-col gap-2.5 border-b border-border bg-surface-secondary/50 px-5 py-2.5 sm:px-6">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs font-semibold text-foreground-muted flex items-center gap-1">
              <Filter className="h-3.5 w-3.5" />
              Tier:
            </span>
            <button
              type="button"
              onClick={() => setSelectedTier('all')}
              className={`cursor-pointer rounded-full px-3 py-1 text-xs font-semibold transition-colors ${
                selectedTier === 'all'
                  ? 'bg-primary text-white shadow-sm'
                  : 'bg-surface text-foreground-secondary hover:bg-surface-tertiary'
              }`}
            >
              All ({listCatalogMascots().length})
            </button>
            {Object.values(MASCOT_TIERS).map((tier) => (
              <button
                key={tier.id}
                type="button"
                onClick={() => setSelectedTier(tier.id)}
                className={`cursor-pointer rounded-full px-3 py-1 text-xs font-semibold transition-colors ${
                  selectedTier === tier.id
                    ? 'bg-primary text-white shadow-sm'
                    : 'bg-surface text-foreground-secondary hover:bg-surface-tertiary'
                }`}
              >
                {tier.label}
              </button>
            ))}
          </div>

          <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
            <label className="relative min-w-0 flex-1">
              <span className="sr-only">Search mascots</span>
              <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-foreground-muted" />
              <input
                type="search"
                value={searchQuery}
                onChange={(event) => setSearchQuery(event.target.value)}
                placeholder="Search by name…"
                className="min-h-10 w-full rounded-xl border border-border bg-surface pl-9 pr-3 text-sm text-foreground placeholder:text-foreground-muted focus:outline-none focus-visible:ring-2 focus-visible:ring-primary"
              />
            </label>
            <div className="w-full sm:w-auto sm:min-w-[13.5rem]">
              <Combobox
                aria-label="Sort mascots"
                value={sortBy}
                onChange={setSortBy}
                options={SORT_OPTIONS}
                allowEmpty={false}
                icon={ArrowUpDown}
                className="w-full"
              />
            </div>
          </div>
        </div>

        {/* Mascot Cards List */}
        <div className="overflow-y-auto px-5 py-4 sm:px-6">
          {loading && <p className="py-10 text-center text-sm text-foreground-muted">Loading shop…</p>}
          {loadError && <p className="py-10 text-center text-sm text-error-text">{loadError}</p>}
          {shop && displayedMascots.length === 0 && (
            <p className="py-10 text-center text-sm text-foreground-muted">
              No mascots match your search or filter.
            </p>
          )}
          {shop && displayedMascots.length > 0 && (
            <ul className="grid grid-cols-1 gap-4 min-[480px]:grid-cols-2 lg:grid-cols-3">
              {displayedMascots.map((offer) => {
                const art = mascotArt(offer.id);
                const shortfallPoints = Math.max(0, offer.price - points);
                const shortfallCrystal = offer.crystalCost ? Math.max(0, offer.crystalCost - crystals) : 0;
                
                const canBuyWithPoints = !offer.owned && shortfallPoints === 0;
                const canBuyWithCrystals = !offer.owned && Boolean(offer.crystalCost && shortfallCrystal === 0);
                const canBuy = canBuyWithPoints || canBuyWithCrystals;

                const tierMeta = MASCOT_TIERS[offer.tier] || MASCOT_TIERS.normal;

                return (
                  <li
                    key={offer.id}
                    className={`flex flex-col rounded-3xl border bg-surface p-3.5 transition-shadow hover:shadow-md ${
                      offer.equipped ? 'border-primary ring-2 ring-primary' : 'border-border'
                    }`}
                  >
                    {/* Visual Art Area */}
                    <div className="relative flex h-40 items-center justify-center rounded-2xl bg-surface-secondary">
                      {/* Top Badges */}
                      <div className="absolute left-3 top-3">
                        <span className={`rounded-full border px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider ${tierMeta.badgeClass}`}>
                          {tierMeta.name}
                        </span>
                      </div>

                      {offer.equipped && (
                        <span className="absolute right-3 top-3 rounded-full bg-primary px-2 py-0.5 text-xs font-semibold text-white">
                          Using
                        </span>
                      )}

                      {/* Display Mascot */}
                      <Mascot
                        size={110}
                        label={`${offer.name} mascot`}
                        directions={art.directions}
                        reactions={art.reactions}
                        interactive={offer.owned}
                        trackPointer={offer.owned}
                      />
                    </div>

                    {/* Info */}
                    <div className="mt-3 flex items-baseline justify-between gap-2 px-1">
                      <h3 className="text-base font-semibold text-foreground">{offer.name}</h3>
                      <div className="text-right">
                        <span className={`text-sm font-semibold tabular-nums ${offer.starter ? 'text-foreground-muted' : 'text-warning-text'}`}>
                          {offer.starter ? 'Included' : `${formatNumber(offer.price)} pts`}
                        </span>
                        {offer.crystalCost && (
                          <div className="text-[11px] font-bold text-warning-text flex items-center justify-end gap-0.5">
                            <Gem className="h-3 w-3" />
                            <span>or {offer.crystalCost} Crystal</span>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Action Button */}
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
                        onClick={() => handleOpenPurchase(offer)}
                        className={`mt-3 min-h-11 w-full cursor-pointer rounded-xl text-sm font-semibold transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:cursor-not-allowed disabled:opacity-60 ${
                          canBuyWithCrystals && !canBuyWithPoints
                            ? 'bg-warning-bg text-warning-text border border-warning-border hover:bg-warning-bg/80'
                            : canBuy
                            ? 'bg-primary text-white hover:bg-primary-hover'
                            : 'bg-surface-secondary text-foreground-muted border border-border'
                        }`}
                      >
                        {canBuyWithCrystals && !canBuyWithPoints
                          ? `Redeem for ${offer.crystalCost} Crystal`
                          : canBuyWithPoints && !canBuyWithCrystals
                          ? `Buy (${formatNumber(offer.price)} pts)`
                          : canBuy
                          ? `Redeem / Buy now`
                          : offer.crystalCost
                          ? `Need ${shortfallCrystal} Crystal or ${formatNumber(shortfallPoints)} pts`
                          : `Need ${formatNumber(shortfallPoints)} more`}
                      </button>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </div>

      {/* Confirmation & Payment Method Dialog */}
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
            <h3 id="mascot-buy-title" className="text-lg font-bold text-foreground">
              Unlock {pending.name}
            </h3>
            <p id="mascot-buy-body" className="mt-1.5 text-xs text-foreground-secondary">
              Choose a payment method to unlock this mascot:
            </p>

            {/* Payment Choice Selection */}
            <div className="mt-4 space-y-2.5">
              {pending.crystalCost && (
                <label
                  className={`flex cursor-pointer items-center justify-between rounded-2xl border p-3.5 transition-all ${
                    paymentChoice === 'crystal'
                      ? 'border-warning-border bg-warning-bg/40 ring-2 ring-warning-border'
                      : 'border-border bg-surface hover:bg-surface-secondary'
                  } ${crystals < pending.crystalCost ? 'opacity-50 cursor-not-allowed' : ''}`}
                >
                  <div className="flex items-center gap-3">
                    <input
                      type="radio"
                      name="paymentChoice"
                      value="crystal"
                      checked={paymentChoice === 'crystal'}
                      disabled={crystals < pending.crystalCost}
                      onChange={() => setPaymentChoice('crystal')}
                      className="text-warning-text focus:ring-warning-border"
                    />
                    <div>
                      <span className="block text-sm font-bold text-foreground">
                        Redeem with Crystals
                      </span>
                      <span className="text-xs text-foreground-muted">
                        Current balance: {crystals} Crystal
                      </span>
                    </div>
                  </div>
                  <span className="text-sm font-bold text-warning-text flex items-center gap-1">
                    <Gem className="h-4 w-4" />
                    {pending.crystalCost} Crystal
                  </span>
                </label>
              )}

              <label
                className={`flex cursor-pointer items-center justify-between rounded-2xl border p-3.5 transition-all ${
                  paymentChoice === 'points'
                    ? 'border-primary bg-primary/10 ring-2 ring-primary'
                    : 'border-border bg-surface hover:bg-surface-secondary'
                } ${points < pending.price ? 'opacity-50 cursor-not-allowed' : ''}`}
              >
                <div className="flex items-center gap-3">
                  <input
                    type="radio"
                    name="paymentChoice"
                    value="points"
                    checked={paymentChoice === 'points'}
                    disabled={points < pending.price}
                    onChange={() => setPaymentChoice('points')}
                    className="text-primary focus:ring-primary"
                  />
                  <div>
                    <span className="block text-sm font-bold text-foreground">
                      Buy with reward points
                    </span>
                    <span className="text-xs text-foreground-muted">
                      Current balance: {formatNumber(points)} pts
                    </span>
                  </div>
                </div>
                <span className="text-sm font-bold text-warning-text">
                  {formatNumber(pending.price)} pts
                </span>
              </label>
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                disabled={busy}
                onClick={() => setPending(null)}
                className="min-h-11 cursor-pointer rounded-xl border border-border bg-surface-secondary px-4 text-xs font-semibold text-foreground hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:opacity-60"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={
                  busy || 
                  (paymentChoice === 'crystal' && crystals < (pending.crystalCost || 0)) ||
                  (paymentChoice === 'points' && points < pending.price)
                }
                onClick={executePurchase}
                className="min-h-11 cursor-pointer rounded-xl bg-primary px-5 text-xs font-bold text-white hover:bg-primary-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:opacity-50"
              >
                {busy ? 'Processing…' : 'Confirm'}
              </button>
            </div>
          </div>
        </ModalOverlay>
      )}

      {exchangeOpen && (
        <ModalOverlay onBackdropClick={busy ? undefined : () => setExchangeOpen(false)}>
          <div
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="crystal-exchange-title"
            className="w-full max-w-md rounded-3xl border border-border bg-surface p-6 shadow-xl"
            onClick={(event) => event.stopPropagation()}
          >
            <h3 id="crystal-exchange-title" className="text-lg font-bold text-foreground">
              Exchange Crystals for points
            </h3>
            <p className="mt-1.5 text-xs text-foreground-secondary">
              1 Crystal = {formatNumber(POINTS_PER_CRYSTAL)} pts. Current balance: {crystals} Crystal.
            </p>

            <div className="mt-4 flex items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  disabled={busy || exchangeCount <= 1}
                  onClick={() => setExchangeCount((count) => Math.max(1, count - 1))}
                  className="h-11 w-11 cursor-pointer rounded-xl border border-border bg-surface-secondary text-lg font-bold text-foreground hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:cursor-not-allowed disabled:opacity-50"
                >
                  −
                </button>
                <span className="min-w-10 text-center text-lg font-bold tabular-nums text-foreground">{exchangeCount}</span>
                <button
                  type="button"
                  disabled={busy || exchangeCount >= Math.min(crystals, 99)}
                  onClick={() => setExchangeCount((count) => Math.min(99, crystals, count + 1))}
                  className="h-11 w-11 cursor-pointer rounded-xl border border-border bg-surface-secondary text-lg font-bold text-foreground hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:cursor-not-allowed disabled:opacity-50"
                >
                  +
                </button>
              </div>
              <button
                type="button"
                disabled={busy || crystals < 1}
                onClick={() => setExchangeCount(Math.min(crystals, 99))}
                className="min-h-11 cursor-pointer rounded-xl border border-border px-3 text-xs font-semibold text-foreground hover:bg-surface-secondary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:cursor-not-allowed disabled:opacity-50"
              >
                All
              </button>
            </div>

            <p className="mt-4 text-sm font-semibold text-warning-text">
              Receive {formatNumber(Math.max(exchangeCount, 0) * POINTS_PER_CRYSTAL)} pts
            </p>

            <div className="mt-6 flex justify-end gap-2">
              <button
                type="button"
                disabled={busy}
                onClick={() => setExchangeOpen(false)}
                className="min-h-11 cursor-pointer rounded-xl border border-border bg-surface-secondary px-4 text-xs font-semibold text-foreground hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:opacity-60"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={busy || crystals < 1 || exchangeCount < 1 || exchangeCount > Math.min(crystals, 99)}
                onClick={confirmCrystalExchange}
                className="min-h-11 cursor-pointer rounded-xl bg-primary px-5 text-xs font-bold text-white hover:bg-primary-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-primary disabled:opacity-50"
              >
                {busy ? 'Processing…' : 'Exchange'}
              </button>
            </div>
          </div>
        </ModalOverlay>
      )}
    </ModalOverlay>
  );
}
