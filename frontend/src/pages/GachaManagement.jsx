import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  ArrowLeft,
  ArrowRight,
  Check,
  Coins,
  Gem,
  PawPrint,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  SlidersHorizontal,
  Ticket,
  Trash2,
  X,
} from 'lucide-react';
import {
  Crown,
  Diamond,
  Ticket as PhosphorTicket,
  UsersThree,
} from '@phosphor-icons/react';
import DashboardSection from '../components/lecturer/DashboardSection';
import { Mascot } from '../components/student/MascotSprite';
import { listCatalogMascots, mascotArt } from '../components/student/studentMascots';
import { Badge } from '../components/ui/badge';
import Combobox from '../components/ui/Combobox';
import Modal from '../components/ui/Modal';
import {
  AnimatedCounter,
  StageCard3D,
  StaggerGroup,
  StaggerItem,
} from '../components/ui/MotionPrimitives';
import SortableTableHeader from '../components/ui/SortableTableHeader';
import { useToast } from '../components/ui/Toast';
import { useDialog } from '../context/DialogContext';
import {
  friendlyLoadErrorFromResponse,
  readFriendlyApiError,
  toFriendlyError,
} from '../utils/apiError';
import { apiFetch } from '../utils/apiFetch';
import { authHeaders } from '../utils/authHeaders';
import { formatNumber } from '../utils/formatters';
import { handleRowDoubleClickEdit } from '../utils/rowDoubleClick';
import { sortRows, toggleSortState } from '../utils/sort';
import DropRateManagement from './DropRateManagement';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8002';
const PAGE_SIZE = 10;

const TIER_ORDER = ['normal', 'rare', 'epic', 'legendary', 'mythic'];

const TIER_LABELS = {
  normal: 'Normal',
  rare: 'Rare',
  epic: 'Epic',
  legendary: 'Legendary',
  mythic: 'Mythic',
};

const TIER_BADGE_VARIANT = {
  normal: 'secondary',
  rare: 'teal',
  epic: 'navy',
  legendary: 'gold',
  mythic: 'destructive',
};

const HEADER_CLASS =
  'px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-foreground-muted';

function getNonStarterCatalog(iceQueenEligible = true) {
  return listCatalogMascots()
    .filter((item) => !item.starter && (iceQueenEligible || !item.exclusive))
    .sort((a, b) => {
      const tierDiff = TIER_ORDER.indexOf(a.tier) - TIER_ORDER.indexOf(b.tier);
      if (tierDiff !== 0) return tierDiff;
      if ((a.price ?? 0) !== (b.price ?? 0)) return (a.price ?? 0) - (b.price ?? 0);
      return a.name.localeCompare(b.name);
    });
}

function buildGroupedMascotOptions(items) {
  const groups = [];
  for (const tier of TIER_ORDER) {
    const tierItems = items
      .filter((item) => item.tier === tier)
      .map((item) => ({
        value: item.id,
        label: item.name,
        badge: TIER_LABELS[tier],
        description: item.exclusive
          ? 'Exclusive'
          : `${formatNumber(item.price)} pts${item.crystalCost ? ` · ${item.crystalCost} Crystal` : ''}`,
      }));
    if (tierItems.length > 0) {
      groups.push({
        group: TIER_LABELS[tier],
        items: tierItems,
      });
    }
  }
  return groups;
}

function resolveIrn(row) {
  return row?.studentCode || row?.teacherCode || '—';
}

function toWalletDraft(user) {
  return {
    crystals: String(user?.crystals ?? 0),
    gachaTickets: String(user?.gachaTickets ?? 0),
    points: String(user?.points ?? 0),
    gachaPityCount: String(user?.gachaPityCount ?? 0),
    eggSmashPity: String(user?.eggSmashPity ?? 0),
  };
}

export default function GachaManagement() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeView = searchParams.get('tab') === 'rates' ? 'rates' : 'players';

  const showToast = useToast();
  const dialog = useDialog();

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [search, setSearch] = useState('');
  const [scopeFilter, setScopeFilter] = useState('all');
  const [petFilter, setPetFilter] = useState('');
  const [sortState, setSortState] = useState({ field: 'crystals', direction: 'desc' });
  const [page, setPage] = useState(1);

  const [managingUserId, setManagingUserId] = useState(null);
  const [walletDraft, setWalletDraft] = useState(() => toWalletDraft(null));
  const [addMascotId, setAddMascotId] = useState('');
  const [addPricePaid, setAddPricePaid] = useState('0');
  const [addEquip, setAddEquip] = useState(false);
  const [mutatingUser, setMutatingUser] = useState(false);

  const [editingMascotId, setEditingMascotId] = useState(null);
  const [editTargetMascotId, setEditTargetMascotId] = useState('');
  const [editPricePaid, setEditPricePaid] = useState('0');
  const [editEquip, setEditEquip] = useState(false);

  const [distributeOpen, setDistributeOpen] = useState(false);
  const [distUserSearch, setDistUserSearch] = useState('');
  const [distOnlyCurrentTerm, setDistOnlyCurrentTerm] = useState(false);
  const [distSelectedUserIds, setDistSelectedUserIds] = useState([]);
  const [distCrystalsDelta, setDistCrystalsDelta] = useState('0');
  const [distTicketsDelta, setDistTicketsDelta] = useState('0');
  const [distPointsDelta, setDistPointsDelta] = useState('0');
  const [distPetSearch, setDistPetSearch] = useState('');
  const [distTierFilter, setDistTierFilter] = useState('all');
  const [distSelectedMascotIds, setDistSelectedMascotIds] = useState([]);
  const [distPricePaid, setDistPricePaid] = useState('0');
  const [distEquip, setDistEquip] = useState(false);
  const [submittingDistribute, setSubmittingDistribute] = useState(false);

  const switchView = (nextView) => {
    const nextParams = new URLSearchParams(searchParams);
    if (nextView === 'rates') {
      nextParams.set('tab', 'rates');
    } else {
      nextParams.delete('tab');
    }
    setSearchParams(nextParams, { replace: true });
  };

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const res = await apiFetch(`${API_BASE}/api/lecturer/admin-mascots/users`, {
        headers: authHeaders(),
      });
      if (!res.ok) {
        setLoadError(await friendlyLoadErrorFromResponse(res));
        return;
      }
      const data = await res.json();
      setRows(Array.isArray(data) ? data : []);
    } catch (err) {
      setLoadError(toFriendlyError(err, 'read'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadUsers();
  }, [loadUsers]);

  const replaceUserRow = useCallback((updatedRow) => {
    if (!updatedRow?.userId) return;
    setRows((prev) =>
      prev.map((item) => (item.userId === updatedRow.userId ? updatedRow : item)),
    );
  }, []);

  const replaceManyUserRows = useCallback((updatedList) => {
    if (!Array.isArray(updatedList) || updatedList.length === 0) return;
    const byId = new Map(updatedList.map((r) => [r.userId, r]));
    setRows((prev) => prev.map((item) => byId.get(item.userId) ?? item));
  }, []);

  const managingUser = useMemo(
    () => rows.find((r) => r.userId === managingUserId) ?? null,
    [rows, managingUserId],
  );

  const allFilterPetOptions = useMemo(() => {
    const starterOption = {
      value: 'fox',
      label: 'Fox',
      badge: 'Starter',
    };
    return [starterOption, ...buildGroupedMascotOptions(getNonStarterCatalog(true))];
  }, []);

  const summaryStats = useMemo(() => {
    const totalUsers = rows.length;
    let totalCrystals = 0;
    let totalTickets = 0;
    let totalOwnedPets = 0;
    for (const r of rows) {
      totalCrystals += r.crystals ?? 0;
      totalTickets += r.gachaTickets ?? 0;
      totalOwnedPets += r.owned?.length ?? 0;
    }
    return [
      {
        label: 'Players',
        value: totalUsers,
        icon: UsersThree,
        pillClass: 'bg-navy-light text-navy dark:text-foreground',
      },
      {
        label: 'Total Crystals',
        value: totalCrystals,
        icon: Diamond,
        pillClass: 'bg-warning-bg text-warning-text',
      },
      {
        label: 'Total Roll Tickets',
        value: totalTickets,
        icon: PhosphorTicket,
        pillClass: 'bg-secondary-light text-secondary-text',
      },
      {
        label: 'Unlocked Pets',
        value: totalOwnedPets,
        icon: Crown,
        pillClass: 'bg-gold-bg text-gold-text',
      },
    ];
  }, [rows]);

  const filteredRows = useMemo(() => {
    const q = search.trim().toLowerCase();
    const matched = rows.filter((row) => {
      if (scopeFilter === 'current' && !row.inCurrentTerm) return false;
      if (scopeFilter === 'withPets' && (row.owned?.length ?? 0) === 0) return false;
      if (scopeFilter === 'withCrystals' && (row.crystals ?? 0) <= 0) return false;
      if (petFilter) {
        if (petFilter === 'fox') {
          if ((row.equippedMascot || 'fox') !== 'fox') return false;
        } else {
          const hasPet =
            row.equippedMascot === petFilter
            || (row.owned || []).some((p) => p.mascotId === petFilter);
          if (!hasPet) return false;
        }
      }
      if (!q) return true;
      const ownedNames = (row.owned || [])
        .map((p) => `${p.mascotId} ${p.displayName}`)
        .join(' ');
      const equippedArt = mascotArt(row.equippedMascot);
      const haystack = `${row.studentCode || ''} ${row.teacherCode || ''} ${row.fullName || ''} ${row.email || ''} ${equippedArt.name} ${ownedNames}`.toLowerCase();
      return haystack.includes(q);
    });

    return sortRows(matched, sortState.field, sortState.direction, (row) => {
      if (sortState.field === 'irn') return resolveIrn(row);
      if (sortState.field === 'fullName') return row.fullName || '';
      if (sortState.field === 'crystals') return row.crystals ?? 0;
      if (sortState.field === 'gachaTickets') return row.gachaTickets ?? 0;
      if (sortState.field === 'points') return row.points ?? 0;
      if (sortState.field === 'ownedCount') return row.owned?.length ?? 0;
      return row[sortState.field] ?? '';
    });
  }, [rows, search, scopeFilter, petFilter, sortState]);

  const pageCount = Math.max(1, Math.ceil(filteredRows.length / PAGE_SIZE));
  const safePage = Math.min(Math.max(page, 1), pageCount);

  const pagedRows = useMemo(() => {
    const start = (safePage - 1) * PAGE_SIZE;
    return filteredRows.slice(start, start + PAGE_SIZE);
  }, [filteredRows, safePage]);

  const handleSort = (field) => {
    setSortState((prev) => toggleSortState(prev, field));
    setPage(1);
  };

  const openManageModal = (row) => {
    setManagingUserId(row.userId);
    setWalletDraft(toWalletDraft(row));
    setAddMascotId('');
    setAddPricePaid('0');
    setAddEquip(false);
    setEditingMascotId(null);
  };

  const closeManageModal = () => {
    if (mutatingUser) return;
    setManagingUserId(null);
    setEditingMascotId(null);
  };

  const walletDirty = useMemo(() => {
    if (!managingUser) return false;
    const clean = toWalletDraft(managingUser);
    return Object.keys(clean).some((k) => String(walletDraft[k] ?? '').trim() !== clean[k]);
  }, [managingUser, walletDraft]);

  const availableAddOptionsForManagingUser = useMemo(() => {
    if (!managingUser) return [];
    const ownedSet = new Set((managingUser.owned || []).map((o) => o.mascotId));
    const unowned = getNonStarterCatalog(managingUser.iceQueenEligible).filter(
      (item) => !ownedSet.has(item.id),
    );
    return buildGroupedMascotOptions(unowned);
  }, [managingUser]);

  const buildEditOptionsForPet = useCallback(
    (currentPetId) => {
      if (!managingUser) return [];
      const otherOwned = new Set(
        (managingUser.owned || [])
          .map((o) => o.mascotId)
          .filter((id) => id !== currentPetId),
      );
      const allowed = getNonStarterCatalog(managingUser.iceQueenEligible).filter(
        (item) => !otherOwned.has(item.id),
      );
      return buildGroupedMascotOptions(allowed);
    },
    [managingUser],
  );

  const handleSaveWallet = async () => {
    if (!managingUser || mutatingUser || !walletDirty) return;

    const crystals = Number(walletDraft.crystals);
    const gachaTickets = Number(walletDraft.gachaTickets);
    const points = Number(walletDraft.points);
    const gachaPityCount = Number(walletDraft.gachaPityCount);
    const eggSmashPity = Number(walletDraft.eggSmashPity);

    if (
      !Number.isInteger(crystals) || crystals < 0 || crystals > 1000000
      || !Number.isInteger(gachaTickets) || gachaTickets < 0 || gachaTickets > 1000000
      || !Number.isInteger(points) || points < 0 || points > 100000000
      || !Number.isInteger(gachaPityCount) || gachaPityCount < 0 || gachaPityCount > 1000
      || !Number.isInteger(eggSmashPity) || eggSmashPity < 0 || eggSmashPity > 8
    ) {
      showToast('Check wallet and pity ranges (Crystals/Tickets ≥ 0, Wheel Pity 0–1000, Egg Pity 0–8).', 'error');
      return;
    }

    setMutatingUser(true);
    try {
      const res = await apiFetch(
        `${API_BASE}/api/lecturer/admin-mascots/users/${managingUser.userId}/resources`,
        {
          method: 'PUT',
          headers: authHeaders({ 'Content-Type': 'application/json' }),
          body: JSON.stringify({
            crystals,
            gachaTickets,
            points,
            gachaPityCount,
            eggSmashPity,
          }),
        },
      );
      if (!res.ok) {
        showToast(await readFriendlyApiError(res, 'save'), 'error');
        return;
      }
      const updated = await res.json();
      replaceUserRow(updated);
      setWalletDraft(toWalletDraft(updated));
      showToast('Saved wallet & pity.', 'success');
    } catch (err) {
      showToast(toFriendlyError(err, 'save'), 'error');
    } finally {
      setMutatingUser(false);
    }
  };

  const handleAddSingleUserPet = async () => {
    if (!managingUser || !addMascotId || mutatingUser) return;
    const parsedPrice = Number(addPricePaid);
    if (!Number.isInteger(parsedPrice) || parsedPrice < 0 || parsedPrice > 1000000) {
      showToast('Price paid must be an integer from 0 to 1,000,000.', 'error');
      return;
    }

    setMutatingUser(true);
    try {
      const res = await apiFetch(
        `${API_BASE}/api/lecturer/admin-mascots/users/${managingUser.userId}/pets`,
        {
          method: 'POST',
          headers: authHeaders({ 'Content-Type': 'application/json' }),
          body: JSON.stringify({
            mascotIds: [addMascotId],
            pricePaid: parsedPrice,
            equip: addEquip,
          }),
        },
      );
      if (!res.ok) {
        showToast(await readFriendlyApiError(res, 'save'), 'error');
        return;
      }
      const updated = await res.json();
      replaceUserRow(updated);
      setAddMascotId('');
      setAddPricePaid('0');
      setAddEquip(false);
      showToast(`Granted ${mascotArt(addMascotId).name}.`, 'success');
    } catch (err) {
      showToast(toFriendlyError(err, 'save'), 'error');
    } finally {
      setMutatingUser(false);
    }
  };

  const startEditPet = (pet) => {
    setEditingMascotId(pet.mascotId);
    setEditTargetMascotId(pet.mascotId);
    setEditPricePaid(String(pet.pricePaid ?? 0));
    setEditEquip(Boolean(pet.equipped));
  };

  const handleSaveEditedPet = async (originalMascotId) => {
    if (!managingUser || !editTargetMascotId || mutatingUser) return;
    const parsedPrice = Number(editPricePaid);
    if (!Number.isInteger(parsedPrice) || parsedPrice < 0 || parsedPrice > 1000000) {
      showToast('Price paid must be an integer from 0 to 1,000,000.', 'error');
      return;
    }

    setMutatingUser(true);
    try {
      const res = await apiFetch(
        `${API_BASE}/api/lecturer/admin-mascots/users/${managingUser.userId}/pets/${encodeURIComponent(originalMascotId)}`,
        {
          method: 'PUT',
          headers: authHeaders({ 'Content-Type': 'application/json' }),
          body: JSON.stringify({
            targetMascotId: editTargetMascotId,
            pricePaid: parsedPrice,
            equip: editEquip,
          }),
        },
      );
      if (!res.ok) {
        showToast(await readFriendlyApiError(res, 'save'), 'error');
        return;
      }
      const updated = await res.json();
      replaceUserRow(updated);
      setEditingMascotId(null);
      showToast('Updated pet.', 'success');
    } catch (err) {
      showToast(toFriendlyError(err, 'save'), 'error');
    } finally {
      setMutatingUser(false);
    }
  };

  const handleSetEquipped = async (userRow, mascotId) => {
    if (!userRow || mutatingUser) return;
    setMutatingUser(true);
    try {
      const res = await apiFetch(
        `${API_BASE}/api/lecturer/admin-mascots/users/${userRow.userId}/equipped`,
        {
          method: 'PUT',
          headers: authHeaders({ 'Content-Type': 'application/json' }),
          body: JSON.stringify({ mascotId }),
        },
      );
      if (!res.ok) {
        showToast(await readFriendlyApiError(res, 'save'), 'error');
        return;
      }
      const updated = await res.json();
      replaceUserRow(updated);
      showToast(`Equipped ${mascotArt(mascotId).name}.`, 'success');
    } catch (err) {
      showToast(toFriendlyError(err, 'save'), 'error');
    } finally {
      setMutatingUser(false);
    }
  };

  const handleDeletePet = async (userRow, pet) => {
    if (!userRow || !pet || mutatingUser) return;
    const confirmed = await dialog.confirm({
      level: 'dangerous',
      title: 'Remove Pet',
      message: `Remove ${pet.displayName} from ${userRow.fullName || resolveIrn(userRow)}?`,
      confirmLabel: 'Remove',
    });
    if (!confirmed) return;

    setMutatingUser(true);
    try {
      const res = await apiFetch(
        `${API_BASE}/api/lecturer/admin-mascots/users/${userRow.userId}/pets/${encodeURIComponent(pet.mascotId)}`,
        {
          method: 'DELETE',
          headers: authHeaders(),
        },
      );
      if (!res.ok) {
        showToast(await readFriendlyApiError(res, 'delete'), 'error');
        return;
      }
      const updated = await res.json();
      replaceUserRow(updated);
      if (editingMascotId === pet.mascotId) {
        setEditingMascotId(null);
      }
      showToast(`Removed ${pet.displayName}.`, 'success');
    } catch (err) {
      showToast(toFriendlyError(err, 'delete'), 'error');
    } finally {
      setMutatingUser(false);
    }
  };

  const openDistributeModal = (preselectedUser = null) => {
    setDistUserSearch('');
    setDistOnlyCurrentTerm(false);
    setDistSelectedUserIds(preselectedUser ? [preselectedUser.userId] : []);
    setDistCrystalsDelta('0');
    setDistTicketsDelta('0');
    setDistPointsDelta('0');
    setDistPetSearch('');
    setDistTierFilter('all');
    setDistSelectedMascotIds([]);
    setDistPricePaid('0');
    setDistEquip(false);
    setDistributeOpen(true);
  };

  const closeDistributeModal = () => {
    if (submittingDistribute) return;
    setDistributeOpen(false);
  };

  const distCandidateUsers = useMemo(() => {
    const q = distUserSearch.trim().toLowerCase();
    return rows.filter((r) => {
      if (distOnlyCurrentTerm && !r.inCurrentTerm) return false;
      if (!q) return true;
      return `${r.studentCode || ''} ${r.teacherCode || ''} ${r.fullName || ''} ${r.email || ''}`
        .toLowerCase()
        .includes(q);
    });
  }, [rows, distUserSearch, distOnlyCurrentTerm]);

  const distCatalogPets = useMemo(() => {
    const allowIceQueen =
      distSelectedUserIds.length > 0
      && distSelectedUserIds.every((uid) => rows.find((r) => r.userId === uid)?.iceQueenEligible);
    const q = distPetSearch.trim().toLowerCase();
    return getNonStarterCatalog(allowIceQueen).filter((item) => {
      if (distTierFilter !== 'all' && item.tier !== distTierFilter) return false;
      if (!q) return true;
      return `${item.id} ${item.name}`.toLowerCase().includes(q);
    });
  }, [distSelectedUserIds, rows, distPetSearch, distTierFilter]);

  const toggleDistUser = (userId) => {
    setDistSelectedUserIds((prev) =>
      prev.includes(userId) ? prev.filter((id) => id !== userId) : [...prev, userId],
    );
  };

  const toggleDistMascot = (mascotId) => {
    setDistSelectedMascotIds((prev) =>
      prev.includes(mascotId) ? prev.filter((id) => id !== mascotId) : [...prev, mascotId],
    );
  };

  const parsedDistCrystals = Number(distCrystalsDelta || 0);
  const parsedDistTickets = Number(distTicketsDelta || 0);
  const parsedDistPoints = Number(distPointsDelta || 0);
  const hasAnyDistPayload =
    distSelectedMascotIds.length > 0
    || (Number.isInteger(parsedDistCrystals) && parsedDistCrystals !== 0)
    || (Number.isInteger(parsedDistTickets) && parsedDistTickets !== 0)
    || (Number.isInteger(parsedDistPoints) && parsedDistPoints !== 0);

  const handleSubmitDistribute = async () => {
    if (
      submittingDistribute
      || distSelectedUserIds.length === 0
      || !hasAnyDistPayload
    ) {
      return;
    }

    const parsedPrice = Number(distPricePaid);
    if (
      !Number.isInteger(parsedDistCrystals) || Math.abs(parsedDistCrystals) > 1000000
      || !Number.isInteger(parsedDistTickets) || Math.abs(parsedDistTickets) > 1000000
      || !Number.isInteger(parsedDistPoints) || Math.abs(parsedDistPoints) > 100000000
      || !Number.isInteger(parsedPrice) || parsedPrice < 0 || parsedPrice > 1000000
    ) {
      showToast('Check Crystal, Ticket, Point, and Price Paid integer ranges.', 'error');
      return;
    }

    setSubmittingDistribute(true);
    try {
      const res = await apiFetch(`${API_BASE}/api/lecturer/admin-mascots/bulk-grant`, {
        method: 'POST',
        headers: authHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({
          userIds: distSelectedUserIds,
          mascotIds: distSelectedMascotIds,
          pricePaid: parsedPrice,
          equip: distEquip,
          crystalsDelta: parsedDistCrystals,
          ticketsDelta: parsedDistTickets,
          pointsDelta: parsedDistPoints,
        }),
      });
      if (!res.ok) {
        showToast(await readFriendlyApiError(res, 'save'), 'error');
        return;
      }
      const result = await res.json();
      replaceManyUserRows(result?.updatedRows);
      if (managingUserId) {
        const refreshedTarget = result?.updatedRows?.find((r) => r.userId === managingUserId);
        if (refreshedTarget) {
          setWalletDraft(toWalletDraft(refreshedTarget));
        }
      }
      setDistributeOpen(false);
      showToast(
        `Updated ${result.usersUpdated ?? 0} user(s)${
          (result.petsGranted ?? 0) > 0 ? ` · +${result.petsGranted} pet(s)` : ''
        }.`,
        'success',
      );
    } catch (err) {
      showToast(toFriendlyError(err, 'save'), 'error');
    } finally {
      setSubmittingDistribute(false);
    }
  };

  return (
    <div className="max-w-full space-y-6">
      {/* Unified Gacha Section Switcher */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="inline-flex rounded-2xl border border-border bg-surface p-1.5 shadow-stage-1">
          <button
            type="button"
            onClick={() => switchView('players')}
            className={`inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold transition-colors cursor-pointer ${
              activeView === 'players'
                ? 'bg-navy text-on-navy dark:bg-primary dark:text-on-primary'
                : 'text-foreground-secondary hover:bg-surface-secondary hover:text-foreground'
            }`}
          >
            <PawPrint className="h-4 w-4" />
            Players & Inventory
          </button>
          <button
            type="button"
            onClick={() => switchView('rates')}
            className={`inline-flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold transition-colors cursor-pointer ${
              activeView === 'rates'
                ? 'bg-navy text-on-navy dark:bg-primary dark:text-on-primary'
                : 'text-foreground-secondary hover:bg-surface-secondary hover:text-foreground'
            }`}
          >
            <SlidersHorizontal className="h-4 w-4" />
            Drop Rates & Pity
          </button>
        </div>
      </div>

      {activeView === 'rates' ? (
        <DropRateManagement />
      ) : loading && rows.length === 0 ? (
        <div className="flex h-64 items-center justify-center">
          <div className="flex flex-col items-center gap-3">
            <div className="h-10 w-10 animate-spin rounded-full border-4 border-primary border-t-transparent" />
            <p className="text-foreground-secondary">Loading data...</p>
          </div>
        </div>
      ) : (
        <>
          {loadError && (
            <div className="flex items-center justify-between gap-3 rounded-xl bg-warning-bg px-4 py-3 text-sm text-warning-text">
              <span>{loadError}</span>
              <button
                type="button"
                onClick={loadUsers}
                className="inline-flex min-h-9 items-center gap-1.5 rounded-lg bg-surface px-3 py-1.5 text-xs font-semibold text-foreground transition-colors hover:bg-surface-secondary cursor-pointer"
              >
                <RefreshCw className="h-3.5 w-3.5" />
                Retry
              </button>
            </div>
          )}

          <StaggerGroup className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {summaryStats.map(({ label, value, icon: Icon, pillClass }) => (
              <StaggerItem key={label} className="h-full">
                <StageCard3D maxTilt={3} className="h-full p-4 sm:p-5">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <span className="text-xs font-semibold uppercase tracking-wider text-foreground-muted">
                        {label}
                      </span>
                      <p className="mt-2 text-2xl font-bold tabular-nums text-foreground">
                        <AnimatedCounter value={value} />
                      </p>
                    </div>
                    <div className={`rounded-xl p-2.5 ${pillClass}`}>
                      <Icon weight="duotone" className="h-5 w-5" aria-hidden="true" />
                    </div>
                  </div>
                </StageCard3D>
              </StaggerItem>
            ))}
          </StaggerGroup>

          <DashboardSection
            title="Players & Inventory"
            actions={
              <div className="flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  onClick={() => openDistributeModal(null)}
                  className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-primary px-4 py-2 text-xs font-semibold text-on-primary transition-colors hover:bg-primary-hover cursor-pointer"
                >
                  <Plus className="h-4 w-4" />
                  Distribute / Compensate
                </button>
                <button
                  type="button"
                  onClick={loadUsers}
                  title="Refresh"
                  className="inline-flex min-h-10 min-w-10 items-center justify-center rounded-xl border border-border bg-surface text-foreground-secondary transition-colors hover:bg-surface-secondary hover:text-foreground cursor-pointer"
                >
                  <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                </button>
              </div>
            }
          >
            <div className="overflow-hidden rounded-2xl border border-border bg-surface shadow-stage-1">
              <div className="flex flex-col gap-3 border-b border-border px-5 py-4 xl:flex-row xl:items-center xl:justify-between">
                <div className="flex flex-wrap items-center gap-3">
                  <div className="relative w-full sm:w-72">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-foreground-muted" />
                    <input
                      type="search"
                      value={search}
                      onChange={(e) => {
                        setSearch(e.target.value);
                        setPage(1);
                      }}
                      placeholder="Search IRN, name, email, or pet…"
                      className="w-full rounded-xl border border-border bg-surface-secondary py-2 pl-9 pr-4 text-sm text-foreground placeholder-foreground-disabled focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                  </div>

                  <div className="inline-flex flex-wrap rounded-xl bg-surface-secondary p-1 text-xs font-semibold">
                    {[
                      { id: 'all', label: 'All' },
                      { id: 'current', label: 'Current Quarter' },
                      { id: 'withPets', label: 'With Pets' },
                      { id: 'withCrystals', label: 'With Crystals' },
                    ].map((tab) => (
                      <button
                        key={tab.id}
                        type="button"
                        onClick={() => {
                          setScopeFilter(tab.id);
                          setPage(1);
                        }}
                        className={`rounded-lg px-3 py-1.5 transition-colors cursor-pointer ${
                          scopeFilter === tab.id
                            ? 'bg-primary text-on-primary'
                            : 'text-foreground-secondary hover:text-foreground'
                        }`}
                      >
                        {tab.label}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="w-full sm:w-64">
                  <Combobox
                    value={petFilter}
                    onChange={(val) => {
                      setPetFilter(val);
                      setPage(1);
                    }}
                    options={allFilterPetOptions}
                    placeholder="All Pets"
                    emptyLabel="All Pets"
                    searchable
                    aria-label="Filter by pet"
                  />
                </div>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full min-w-[920px] table-auto text-sm">
                  <thead>
                    <tr className="border-b border-border bg-surface-secondary/50">
                      <SortableTableHeader
                        label="IRN"
                        field="irn"
                        activeField={sortState.field}
                        direction={sortState.direction}
                        onSort={handleSort}
                        className={HEADER_CLASS}
                      />
                      <SortableTableHeader
                        label="User"
                        field="fullName"
                        activeField={sortState.field}
                        direction={sortState.direction}
                        onSort={handleSort}
                        className={HEADER_CLASS}
                      />
                      <SortableTableHeader
                        label="Crystals"
                        field="crystals"
                        activeField={sortState.field}
                        direction={sortState.direction}
                        onSort={handleSort}
                        className={HEADER_CLASS}
                      />
                      <SortableTableHeader
                        label="Tickets"
                        field="gachaTickets"
                        activeField={sortState.field}
                        direction={sortState.direction}
                        onSort={handleSort}
                        className={HEADER_CLASS}
                      />
                      <SortableTableHeader
                        label="Points & Pity"
                        field="points"
                        activeField={sortState.field}
                        direction={sortState.direction}
                        onSort={handleSort}
                        className={HEADER_CLASS}
                      />
                      <SortableTableHeader
                        label="Equipped & Pets"
                        field="ownedCount"
                        activeField={sortState.field}
                        direction={sortState.direction}
                        onSort={handleSort}
                        className={HEADER_CLASS}
                      />
                      <SortableTableHeader
                        label="Actions"
                        sortable={false}
                        className={HEADER_CLASS}
                      />
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {pagedRows.length === 0 ? (
                      <tr>
                        <td colSpan={7} className="px-5 py-12 text-center text-foreground-muted">
                          No matching users found.
                        </td>
                      </tr>
                    ) : (
                      pagedRows.map((row) => {
                        const equippedArt = mascotArt(row.equippedMascot);
                        const ownedList = row.owned || [];
                        const visibleOwned = ownedList.slice(0, 3);
                        const hiddenOwnedCount = Math.max(0, ownedList.length - visibleOwned.length);

                        return (
                          <tr
                            key={row.userId}
                            onDoubleClick={(event) =>
                              handleRowDoubleClickEdit(event, () => openManageModal(row))
                            }
                            className="cursor-pointer transition-colors hover:bg-surface-secondary/60"
                          >
                            <td className="px-4 py-3.5 font-mono text-xs font-semibold tabular-nums text-foreground-secondary whitespace-nowrap">
                              {resolveIrn(row)}
                            </td>

                            <td className="px-4 py-3.5">
                              <div className="flex flex-wrap items-center gap-1.5">
                                <span className="font-semibold text-foreground">
                                  {row.fullName || '—'}
                                </span>
                                {row.inCurrentTerm && <Badge variant="teal">Current</Badge>}
                                {!row.active && <Badge variant="warning">Suspended</Badge>}
                              </div>
                              <div className="mt-0.5 text-xs text-foreground-muted">
                                {row.email}
                              </div>
                            </td>

                            <td className="px-4 py-3.5 whitespace-nowrap">
                              <span className="inline-flex items-center gap-1.5 rounded-lg bg-warning-bg px-2.5 py-1 font-mono text-xs font-bold tabular-nums text-warning-text">
                                <Gem className="h-3.5 w-3.5" />
                                {formatNumber(row.crystals)}
                              </span>
                            </td>

                            <td className="px-4 py-3.5 whitespace-nowrap">
                              <span className="inline-flex items-center gap-1.5 rounded-lg bg-secondary-light px-2.5 py-1 font-mono text-xs font-bold tabular-nums text-secondary-text">
                                <Ticket className="h-3.5 w-3.5" />
                                {formatNumber(row.gachaTickets)}
                              </span>
                            </td>

                            <td className="px-4 py-3.5 font-mono text-xs tabular-nums whitespace-nowrap">
                              <div className="inline-flex items-center gap-1 font-semibold text-foreground">
                                <Coins className="h-3.5 w-3.5 text-primary" />
                                {formatNumber(row.points)} pts
                              </div>
                              <div className="mt-0.5 text-[11px] text-foreground-muted">
                                Wheel {row.gachaPityCount ?? 0} · Egg {row.eggSmashPity ?? 0}/8
                              </div>
                            </td>

                            <td className="px-4 py-3.5">
                              <div className="flex flex-wrap items-center gap-1.5">
                                <span
                                  title={`Equipped: ${equippedArt.name}`}
                                  className="inline-flex items-center gap-1.5 rounded-lg bg-primary-light/60 px-2 py-1 text-xs font-semibold text-primary-text"
                                >
                                  <Mascot
                                    directions={equippedArt.directions}
                                    reactions={equippedArt.reactions}
                                    tier={equippedArt.tier}
                                    size={22}
                                    interactive={false}
                                    label={equippedArt.name}
                                  />
                                  <span>{equippedArt.name}</span>
                                </span>

                                {visibleOwned
                                  .filter((pet) => pet.mascotId !== row.equippedMascot)
                                  .map((pet) => {
                                    const art = mascotArt(pet.mascotId);
                                    return (
                                      <button
                                        key={pet.mascotId}
                                        type="button"
                                        onClick={() => openManageModal(row)}
                                        className="inline-flex items-center gap-1 rounded-lg bg-surface-secondary px-2 py-1 text-xs font-medium text-foreground transition-colors hover:bg-primary-light hover:text-primary-text cursor-pointer"
                                      >
                                        <Mascot
                                          directions={art.directions}
                                          reactions={art.reactions}
                                          tier={art.tier}
                                          size={18}
                                          interactive={false}
                                          label={art.name}
                                        />
                                        <span>{pet.displayName}</span>
                                      </button>
                                    );
                                  })}

                                {hiddenOwnedCount > 0 && (
                                  <button
                                    type="button"
                                    onClick={() => openManageModal(row)}
                                    className="inline-flex items-center rounded-lg bg-surface-secondary px-2 py-1 font-mono text-xs font-semibold text-foreground-secondary hover:text-foreground cursor-pointer"
                                  >
                                    +{hiddenOwnedCount}
                                  </button>
                                )}
                              </div>
                            </td>

                            <td className="px-4 py-3.5">
                              <div className="flex items-center gap-1.5">
                                <button
                                  type="button"
                                  onClick={() => openDistributeModal(row)}
                                  title="Distribute to user"
                                  className="inline-flex min-h-9 min-w-9 items-center justify-center rounded-lg text-foreground-secondary transition-colors hover:bg-primary-light hover:text-primary cursor-pointer"
                                >
                                  <Plus className="h-4 w-4" />
                                </button>
                                <button
                                  type="button"
                                  onClick={() => openManageModal(row)}
                                  className="inline-flex min-h-9 items-center gap-1.5 rounded-lg bg-surface-secondary px-3 py-1.5 text-xs font-semibold text-foreground transition-colors hover:bg-primary-light hover:text-primary-text cursor-pointer"
                                >
                                  <Pencil className="h-3.5 w-3.5" />
                                  Manage
                                </button>
                              </div>
                            </td>
                          </tr>
                        );
                      })
                    )}
                  </tbody>
                </table>
              </div>

              <div className="flex flex-col gap-3 border-t border-border bg-surface-secondary px-5 py-3.5 sm:flex-row sm:items-center sm:justify-between">
                <div className="text-xs text-foreground-muted">
                  Showing {pagedRows.length} of {filteredRows.length} users
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => setPage((p) => Math.max(1, p - 1))}
                    disabled={safePage === 1}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-1.5 text-xs font-medium text-foreground-secondary transition-colors hover:bg-surface-tertiary disabled:cursor-not-allowed disabled:opacity-50 cursor-pointer"
                  >
                    <ArrowLeft className="h-3.5 w-3.5" />
                    Previous
                  </button>
                  <span className="font-mono text-xs text-foreground-muted">
                    Page {safePage} of {pageCount}
                  </span>
                  <button
                    type="button"
                    onClick={() => setPage((p) => Math.min(pageCount, p + 1))}
                    disabled={safePage === pageCount}
                    className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-surface px-3 py-1.5 text-xs font-medium text-foreground-secondary transition-colors hover:bg-surface-tertiary disabled:cursor-not-allowed disabled:opacity-50 cursor-pointer"
                  >
                    Next
                    <ArrowRight className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            </div>
          </DashboardSection>
        </>
      )}

      {/* Single-User Wallet, Pity & Pet CRUD Modal */}
      {managingUser && (
        <Modal onClose={closeManageModal} className="max-w-3xl">
          <div className="space-y-5">
            <div className="flex flex-wrap items-center justify-between gap-4 border-b border-border pb-4">
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="text-lg font-bold text-foreground">
                    {managingUser.fullName || resolveIrn(managingUser)}
                  </h2>
                  <span className="rounded-md bg-surface-secondary px-2 py-0.5 font-mono text-xs font-semibold text-foreground-secondary">
                    {resolveIrn(managingUser)}
                  </span>
                </div>
                <div className="mt-1 font-mono text-xs text-foreground-muted">
                  {managingUser.email}
                </div>
              </div>

              <div className="flex items-center gap-3 rounded-xl bg-surface-secondary px-3.5 py-2">
                <Mascot
                  directions={mascotArt(managingUser.equippedMascot).directions}
                  reactions={mascotArt(managingUser.equippedMascot).reactions}
                  tier={mascotArt(managingUser.equippedMascot).tier}
                  size={38}
                  interactive={false}
                  label={mascotArt(managingUser.equippedMascot).name}
                />
                <div>
                  <div className="text-[11px] font-semibold uppercase tracking-wider text-foreground-muted">
                    Equipped
                  </div>
                  <div className="text-sm font-bold text-foreground">
                    {mascotArt(managingUser.equippedMascot).name}
                  </div>
                </div>
                {managingUser.equippedMascot !== 'fox' && (
                  <button
                    type="button"
                    disabled={mutatingUser}
                    onClick={() => handleSetEquipped(managingUser, 'fox')}
                    className="ml-2 rounded-lg bg-surface px-2.5 py-1 text-xs font-semibold text-foreground-secondary transition-colors hover:text-foreground disabled:opacity-50 cursor-pointer"
                  >
                    Equip Fox
                  </button>
                )}
              </div>
            </div>

            {/* 1. Wallet & Pity Editor */}
            <div className="rounded-xl bg-surface-secondary p-4 space-y-3">
              <div className="flex items-center justify-between gap-2">
                <span className="text-xs font-semibold uppercase tracking-wider text-foreground-muted">
                  Wallet & Pity
                </span>
                <button
                  type="button"
                  disabled={mutatingUser || !walletDirty}
                  onClick={handleSaveWallet}
                  className="inline-flex min-h-9 items-center gap-1.5 rounded-xl bg-primary px-3.5 py-1.5 text-xs font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:opacity-50 cursor-pointer"
                >
                  <Check className="h-3.5 w-3.5" />
                  Save Wallet
                </button>
              </div>

              <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
                <div>
                  <label className="mb-1 flex items-center gap-1 text-xs font-semibold text-foreground-secondary">
                    <Gem className="h-3.5 w-3.5 text-warning-text" />
                    Crystals
                  </label>
                  <input
                    type="number"
                    min="0"
                    max="1000000"
                    disabled={mutatingUser}
                    value={walletDraft.crystals}
                    onChange={(e) =>
                      setWalletDraft((prev) => ({ ...prev, crystals: e.target.value }))
                    }
                    className="min-h-10 w-full rounded-xl border border-border bg-surface px-3 py-1.5 font-mono text-sm font-semibold tabular-nums text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>

                <div>
                  <label className="mb-1 flex items-center gap-1 text-xs font-semibold text-foreground-secondary">
                    <Ticket className="h-3.5 w-3.5 text-secondary-text" />
                    Roll Tickets
                  </label>
                  <input
                    type="number"
                    min="0"
                    max="1000000"
                    disabled={mutatingUser}
                    value={walletDraft.gachaTickets}
                    onChange={(e) =>
                      setWalletDraft((prev) => ({ ...prev, gachaTickets: e.target.value }))
                    }
                    className="min-h-10 w-full rounded-xl border border-border bg-surface px-3 py-1.5 font-mono text-sm font-semibold tabular-nums text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>

                <div>
                  <label className="mb-1 flex items-center gap-1 text-xs font-semibold text-foreground-secondary">
                    <Coins className="h-3.5 w-3.5 text-primary" />
                    Points
                  </label>
                  <input
                    type="number"
                    min="0"
                    max="100000000"
                    disabled={mutatingUser}
                    value={walletDraft.points}
                    onChange={(e) =>
                      setWalletDraft((prev) => ({ ...prev, points: e.target.value }))
                    }
                    className="min-h-10 w-full rounded-xl border border-border bg-surface px-3 py-1.5 font-mono text-sm font-semibold tabular-nums text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-xs font-semibold text-foreground-secondary">
                    Wheel Pity
                  </label>
                  <input
                    type="number"
                    min="0"
                    max="1000"
                    disabled={mutatingUser}
                    value={walletDraft.gachaPityCount}
                    onChange={(e) =>
                      setWalletDraft((prev) => ({ ...prev, gachaPityCount: e.target.value }))
                    }
                    className="min-h-10 w-full rounded-xl border border-border bg-surface px-3 py-1.5 font-mono text-sm font-semibold tabular-nums text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>

                <div>
                  <label className="mb-1 block text-xs font-semibold text-foreground-secondary">
                    Egg Pity (0–8)
                  </label>
                  <input
                    type="number"
                    min="0"
                    max="8"
                    disabled={mutatingUser}
                    value={walletDraft.eggSmashPity}
                    onChange={(e) =>
                      setWalletDraft((prev) => ({ ...prev, eggSmashPity: e.target.value }))
                    }
                    className="min-h-10 w-full rounded-xl border border-border bg-surface px-3 py-1.5 font-mono text-sm font-semibold tabular-nums text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
              </div>
            </div>

            {/* 2. Add Pet Row */}
            <div className="rounded-xl bg-surface-secondary p-3.5">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
                <div className="min-w-0 flex-1">
                  <label className="mb-1 block text-xs font-semibold text-foreground-secondary">
                    Add Pet
                  </label>
                  <Combobox
                    value={addMascotId}
                    onChange={setAddMascotId}
                    options={availableAddOptionsForManagingUser}
                    placeholder="Select pet to grant…"
                    allowEmpty={false}
                    searchable
                    disabled={mutatingUser || availableAddOptionsForManagingUser.length === 0}
                    aria-label="Select pet to grant"
                  />
                </div>

                <div className="w-full sm:w-32">
                  <label
                    htmlFor="admin-add-pet-price"
                    className="mb-1 block text-xs font-semibold text-foreground-secondary"
                  >
                    Price Paid
                  </label>
                  <input
                    id="admin-add-pet-price"
                    type="number"
                    min="0"
                    max="1000000"
                    disabled={mutatingUser}
                    value={addPricePaid}
                    onChange={(e) => setAddPricePaid(e.target.value)}
                    className="min-h-10 w-full rounded-xl border border-border bg-surface px-3 py-2 font-mono text-sm tabular-nums text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>

                <label className="inline-flex min-h-10 items-center gap-2 px-1 text-xs font-semibold text-foreground-secondary select-none cursor-pointer">
                  <input
                    type="checkbox"
                    checked={addEquip}
                    disabled={mutatingUser}
                    onChange={(e) => setAddEquip(e.target.checked)}
                    className="h-4 w-4 rounded border-border text-primary focus:ring-primary"
                  />
                  Equip
                </label>

                <button
                  type="button"
                  disabled={mutatingUser || !addMascotId}
                  onClick={handleAddSingleUserPet}
                  className="inline-flex min-h-10 items-center justify-center gap-1.5 rounded-xl bg-primary px-4 py-2 text-xs font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:opacity-50 cursor-pointer"
                >
                  <Plus className="h-4 w-4" />
                  Add
                </button>
              </div>
            </div>

            {/* 3. Owned Pets CRUD List */}
            <div className="max-h-[300px] overflow-y-auto divide-y divide-border">
              <div className="flex items-center justify-between gap-3 py-3">
                <div className="flex items-center gap-3">
                  <Mascot
                    directions={mascotArt('fox').directions}
                    reactions={mascotArt('fox').reactions}
                    tier="normal"
                    size={40}
                    interactive={false}
                    label="Fox"
                  />
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-sm font-semibold text-foreground">Fox</span>
                      <Badge variant="secondary">Starter</Badge>
                      {managingUser.equippedMascot === 'fox' && (
                        <Badge variant="success">Equipped</Badge>
                      )}
                    </div>
                    <div className="font-mono text-xs text-foreground-muted">Default free pet</div>
                  </div>
                </div>

                {managingUser.equippedMascot !== 'fox' && (
                  <button
                    type="button"
                    disabled={mutatingUser}
                    onClick={() => handleSetEquipped(managingUser, 'fox')}
                    className="rounded-lg bg-surface-secondary px-3 py-1.5 text-xs font-semibold text-foreground-secondary transition-colors hover:bg-primary-light hover:text-primary-text disabled:opacity-50 cursor-pointer"
                  >
                    Equip
                  </button>
                )}
              </div>

              {(managingUser.owned || []).map((pet) => {
                const art = mascotArt(pet.mascotId);
                const isEditing = editingMascotId === pet.mascotId;

                if (isEditing) {
                  return (
                    <div
                      key={pet.mascotId}
                      className="flex flex-col gap-3 bg-surface-secondary/60 px-3 py-3.5 sm:flex-row sm:items-end"
                    >
                      <div className="min-w-0 flex-1">
                        <label className="mb-1 block text-xs font-semibold text-foreground-secondary">
                          Pet
                        </label>
                        <Combobox
                          value={editTargetMascotId}
                          onChange={setEditTargetMascotId}
                          options={buildEditOptionsForPet(pet.mascotId)}
                          allowEmpty={false}
                          searchable
                          disabled={mutatingUser}
                          aria-label="Change pet"
                        />
                      </div>

                      <div className="w-full sm:w-28">
                        <label className="mb-1 block text-xs font-semibold text-foreground-secondary">
                          Price Paid
                        </label>
                        <input
                          type="number"
                          min="0"
                          max="1000000"
                          disabled={mutatingUser}
                          value={editPricePaid}
                          onChange={(e) => setEditPricePaid(e.target.value)}
                          className="min-h-10 w-full rounded-xl border border-border bg-surface px-3 py-2 font-mono text-sm tabular-nums text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                        />
                      </div>

                      <label className="inline-flex min-h-10 items-center gap-2 px-1 text-xs font-semibold text-foreground-secondary select-none cursor-pointer">
                        <input
                          type="checkbox"
                          checked={editEquip}
                          disabled={mutatingUser}
                          onChange={(e) => setEditEquip(e.target.checked)}
                          className="h-4 w-4 rounded border-border text-primary focus:ring-primary"
                        />
                        Equipped
                      </label>

                      <div className="flex items-center gap-1.5">
                        <button
                          type="button"
                          disabled={mutatingUser || !editTargetMascotId}
                          onClick={() => handleSaveEditedPet(pet.mascotId)}
                          className="inline-flex min-h-10 items-center gap-1 rounded-xl bg-primary px-3.5 py-2 text-xs font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:opacity-50 cursor-pointer"
                        >
                          <Check className="h-3.5 w-3.5" />
                          Save
                        </button>
                        <button
                          type="button"
                          disabled={mutatingUser}
                          onClick={() => setEditingMascotId(null)}
                          className="inline-flex min-h-10 items-center gap-1 rounded-xl bg-surface px-3 py-2 text-xs font-semibold text-foreground-secondary transition-colors hover:text-foreground disabled:opacity-50 cursor-pointer"
                        >
                          <X className="h-3.5 w-3.5" />
                          Cancel
                        </button>
                      </div>
                    </div>
                  );
                }

                return (
                  <div
                    key={pet.mascotId}
                    onDoubleClick={(event) => {
                      if (mutatingUser) return;
                      handleRowDoubleClickEdit(event, () => startEditPet(pet));
                    }}
                    className={`flex flex-col gap-3 py-3 transition-colors sm:flex-row sm:items-center sm:justify-between ${
                      mutatingUser ? '' : 'cursor-pointer hover:bg-surface-secondary/40'
                    }`}
                  >
                    <div className="flex min-w-0 items-center gap-3">
                      <Mascot
                        directions={art.directions}
                        reactions={art.reactions}
                        tier={art.tier}
                        size={40}
                        interactive={false}
                        label={pet.displayName}
                      />
                      <div className="min-w-0">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="text-sm font-semibold text-foreground">
                            {pet.displayName}
                          </span>
                          <Badge variant={TIER_BADGE_VARIANT[art.tier] || 'secondary'}>
                            {TIER_LABELS[art.tier] || 'Normal'}
                          </Badge>
                          {pet.equipped && <Badge variant="success">Equipped</Badge>}
                        </div>
                        <div className="mt-0.5 flex flex-wrap items-center gap-2 font-mono text-xs text-foreground-muted">
                          <span>
                            {pet.pricePaid === 0
                              ? '0 pts (Grant / Gacha)'
                              : `${formatNumber(pet.pricePaid)} pts`}
                          </span>
                          {pet.purchasedAt && (
                            <>
                              <span>·</span>
                              <span>{pet.purchasedAt}</span>
                            </>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5">
                      {pet.equipped ? (
                        <button
                          type="button"
                          disabled={mutatingUser}
                          onClick={() => handleSetEquipped(managingUser, 'fox')}
                          className="rounded-lg bg-surface-secondary px-3 py-1.5 text-xs font-semibold text-foreground-secondary transition-colors hover:text-foreground disabled:opacity-50 cursor-pointer"
                        >
                          Unequip
                        </button>
                      ) : (
                        <button
                          type="button"
                          disabled={mutatingUser}
                          onClick={() => handleSetEquipped(managingUser, pet.mascotId)}
                          className="rounded-lg bg-surface-secondary px-3 py-1.5 text-xs font-semibold text-foreground-secondary transition-colors hover:bg-primary-light hover:text-primary-text disabled:opacity-50 cursor-pointer"
                        >
                          Equip
                        </button>
                      )}

                      <button
                        type="button"
                        disabled={mutatingUser}
                        onClick={() => startEditPet(pet)}
                        title="Edit pet"
                        className="inline-flex min-h-9 min-w-9 items-center justify-center rounded-lg text-foreground-muted transition-colors hover:bg-primary-light hover:text-primary disabled:opacity-50 cursor-pointer"
                      >
                        <Pencil className="h-4 w-4" />
                      </button>

                      <button
                        type="button"
                        disabled={mutatingUser}
                        onClick={() => handleDeletePet(managingUser, pet)}
                        title="Remove pet"
                        className="inline-flex min-h-9 min-w-9 items-center justify-center rounded-lg text-foreground-muted transition-colors hover:bg-error-bg hover:text-error disabled:opacity-50 cursor-pointer"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </Modal>
      )}

      {/* Unified Bulk / Single Distribute & Compensate Modal */}
      {distributeOpen && (
        <Modal onClose={closeDistributeModal} className="max-w-4xl">
          <div className="space-y-5">
            <div className="border-b border-border pb-3">
              <h2 className="text-lg font-bold text-foreground">Distribute / Compensate</h2>
            </div>

            <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
              {/* Left: Target Users */}
              <div className="space-y-2.5">
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs font-semibold uppercase tracking-wider text-foreground-muted">
                    Users ({distSelectedUserIds.length} selected)
                  </span>
                  <div className="flex items-center gap-2 text-xs">
                    <button
                      type="button"
                      onClick={() =>
                        setDistSelectedUserIds(distCandidateUsers.map((u) => u.userId))
                      }
                      className="font-semibold text-primary hover:underline cursor-pointer"
                    >
                      Select filtered
                    </button>
                    {distSelectedUserIds.length > 0 && (
                      <button
                        type="button"
                        onClick={() => setDistSelectedUserIds([])}
                        className="font-semibold text-foreground-muted hover:text-foreground cursor-pointer"
                      >
                        Clear
                      </button>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <div className="relative flex-1">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-foreground-muted" />
                    <input
                      type="search"
                      value={distUserSearch}
                      onChange={(e) => setDistUserSearch(e.target.value)}
                      placeholder="Search IRN or name…"
                      className="w-full rounded-xl border border-border bg-surface-secondary py-2 pl-8 pr-3 text-xs text-foreground placeholder-foreground-disabled focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                  </div>
                  <label className="inline-flex items-center gap-1.5 text-xs font-semibold text-foreground-secondary whitespace-nowrap select-none cursor-pointer">
                    <input
                      type="checkbox"
                      checked={distOnlyCurrentTerm}
                      onChange={(e) => setDistOnlyCurrentTerm(e.target.checked)}
                      className="h-3.5 w-3.5 rounded border-border text-primary focus:ring-primary"
                    />
                    Current
                  </label>
                </div>

                <div className="max-h-72 overflow-y-auto rounded-xl bg-surface-secondary p-1.5 space-y-1">
                  {distCandidateUsers.length === 0 ? (
                    <p className="py-6 text-center text-xs text-foreground-muted">No users match.</p>
                  ) : (
                    distCandidateUsers.map((u) => {
                      const checked = distSelectedUserIds.includes(u.userId);
                      return (
                        <button
                          key={u.userId}
                          type="button"
                          onClick={() => toggleDistUser(u.userId)}
                          className={`flex w-full items-center justify-between gap-2 rounded-lg px-2.5 py-2 text-left text-xs transition-colors cursor-pointer ${
                            checked
                              ? 'bg-primary-light font-semibold text-primary-text'
                              : 'text-foreground hover:bg-surface'
                          }`}
                        >
                          <div className="min-w-0">
                            <div className="truncate">{u.fullName || resolveIrn(u)}</div>
                            <div className="truncate font-mono text-[11px] text-foreground-muted">
                              {resolveIrn(u)} · {formatNumber(u.crystals)} Crystal ·{' '}
                              {formatNumber(u.gachaTickets)} Ticket
                            </div>
                          </div>
                          {checked && <Check className="h-4 w-4 shrink-0 text-primary" />}
                        </button>
                      );
                    })
                  )}
                </div>
              </div>

              {/* Right: Currencies + Pets */}
              <div className="space-y-4">
                <div className="rounded-xl bg-surface-secondary p-3 space-y-2.5">
                  <span className="block text-xs font-semibold uppercase tracking-wider text-foreground-muted">
                    Currency Adjustments (+/-)
                  </span>
                  <div className="grid grid-cols-3 gap-2.5">
                    <div>
                      <label className="mb-1 flex items-center gap-1 text-xs font-semibold text-foreground-secondary">
                        <Gem className="h-3.5 w-3.5 text-warning-text" />
                        Crystals
                      </label>
                      <input
                        type="number"
                        step="1"
                        disabled={submittingDistribute}
                        value={distCrystalsDelta}
                        onChange={(e) => setDistCrystalsDelta(e.target.value)}
                        className="min-h-9 w-full rounded-lg border border-border bg-surface px-2.5 py-1 font-mono text-xs font-semibold tabular-nums text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                      />
                    </div>

                    <div>
                      <label className="mb-1 flex items-center gap-1 text-xs font-semibold text-foreground-secondary">
                        <Ticket className="h-3.5 w-3.5 text-secondary-text" />
                        Tickets
                      </label>
                      <input
                        type="number"
                        step="1"
                        disabled={submittingDistribute}
                        value={distTicketsDelta}
                        onChange={(e) => setDistTicketsDelta(e.target.value)}
                        className="min-h-9 w-full rounded-lg border border-border bg-surface px-2.5 py-1 font-mono text-xs font-semibold tabular-nums text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                      />
                    </div>

                    <div>
                      <label className="mb-1 flex items-center gap-1 text-xs font-semibold text-foreground-secondary">
                        <Coins className="h-3.5 w-3.5 text-primary" />
                        Points
                      </label>
                      <input
                        type="number"
                        step="100"
                        disabled={submittingDistribute}
                        value={distPointsDelta}
                        onChange={(e) => setDistPointsDelta(e.target.value)}
                        className="min-h-9 w-full rounded-lg border border-border bg-surface px-2.5 py-1 font-mono text-xs font-semibold tabular-nums text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                      />
                    </div>
                  </div>
                </div>

                <div className="space-y-2">
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-xs font-semibold uppercase tracking-wider text-foreground-muted">
                      Pets ({distSelectedMascotIds.length} selected)
                    </span>
                    {distSelectedMascotIds.length > 0 && (
                      <button
                        type="button"
                        onClick={() => setDistSelectedMascotIds([])}
                        className="text-xs font-semibold text-foreground-muted hover:text-foreground cursor-pointer"
                      >
                        Clear
                      </button>
                    )}
                  </div>

                  <div className="relative">
                    <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-foreground-muted" />
                    <input
                      type="search"
                      value={distPetSearch}
                      onChange={(e) => setDistPetSearch(e.target.value)}
                      placeholder="Search pet (e.g. Red Panda)…"
                      className="w-full rounded-xl border border-border bg-surface-secondary py-1.5 pl-8 pr-3 text-xs text-foreground placeholder-foreground-disabled focus:outline-none focus:ring-2 focus:ring-primary"
                    />
                  </div>

                  <div className="flex flex-wrap gap-1">
                    {['all', ...TIER_ORDER].map((tier) => (
                      <button
                        key={tier}
                        type="button"
                        onClick={() => setDistTierFilter(tier)}
                        className={`rounded-lg px-2.5 py-1 text-[11px] font-semibold transition-colors cursor-pointer ${
                          distTierFilter === tier
                            ? 'bg-primary text-on-primary'
                            : 'bg-surface-secondary text-foreground-secondary hover:text-foreground'
                        }`}
                      >
                        {tier === 'all' ? 'All' : TIER_LABELS[tier]}
                      </button>
                    ))}
                  </div>

                  <div className="max-h-44 overflow-y-auto rounded-xl bg-surface-secondary p-1.5 grid grid-cols-2 gap-1.5">
                    {distCatalogPets.map((pet) => {
                      const selected = distSelectedMascotIds.includes(pet.id);
                      return (
                        <button
                          key={pet.id}
                          type="button"
                          onClick={() => toggleDistMascot(pet.id)}
                          className={`flex items-center gap-2 rounded-lg px-2.5 py-1.5 text-left text-xs transition-colors cursor-pointer ${
                            selected
                              ? 'bg-primary-light font-semibold text-primary-text ring-1 ring-inset ring-primary/40'
                              : 'bg-surface/70 text-foreground hover:bg-surface'
                          }`}
                        >
                          <Mascot
                            directions={pet.directions}
                            reactions={pet.reactions}
                            tier={pet.tier}
                            size={28}
                            interactive={false}
                            label={pet.name}
                          />
                          <div className="min-w-0 flex-1">
                            <div className="truncate font-semibold">{pet.name}</div>
                            <div className="font-mono text-[10px] text-foreground-muted">
                              {TIER_LABELS[pet.tier]}
                            </div>
                          </div>
                          {selected && <Check className="h-3.5 w-3.5 shrink-0 text-primary" />}
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>
            </div>

            <div className="flex flex-col gap-3 border-t border-border pt-4 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex flex-wrap items-center gap-4">
                {distSelectedMascotIds.length > 0 && (
                  <>
                    <div className="flex items-center gap-2">
                      <label
                        htmlFor="dist-modal-price"
                        className="text-xs font-semibold text-foreground-secondary"
                      >
                        Pet Price Paid
                      </label>
                      <input
                        id="dist-modal-price"
                        type="number"
                        min="0"
                        max="1000000"
                        disabled={submittingDistribute}
                        value={distPricePaid}
                        onChange={(e) => setDistPricePaid(e.target.value)}
                        className="w-24 rounded-xl border border-border bg-surface-secondary px-2.5 py-1.5 font-mono text-xs font-semibold tabular-nums text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                      />
                    </div>

                    <label className="inline-flex items-center gap-2 text-xs font-semibold text-foreground-secondary select-none cursor-pointer">
                      <input
                        type="checkbox"
                        checked={distEquip}
                        disabled={submittingDistribute}
                        onChange={(e) => setDistEquip(e.target.checked)}
                        className="h-4 w-4 rounded border-border text-primary focus:ring-primary"
                      />
                      Equip pet immediately
                    </label>
                  </>
                )}
              </div>

              <div className="flex items-center justify-end gap-2">
                <button
                  type="button"
                  disabled={submittingDistribute}
                  onClick={closeDistributeModal}
                  className="rounded-xl bg-surface-secondary px-4 py-2 text-xs font-semibold text-foreground-secondary transition-colors hover:text-foreground disabled:opacity-50 cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  disabled={
                    submittingDistribute
                    || distSelectedUserIds.length === 0
                    || !hasAnyDistPayload
                  }
                  onClick={handleSubmitDistribute}
                  className="inline-flex items-center gap-1.5 rounded-xl bg-primary px-4 py-2 text-xs font-semibold text-on-primary transition-colors hover:bg-primary-hover disabled:opacity-50 cursor-pointer"
                >
                  <Check className="h-4 w-4" />
                  Apply
                </button>
              </div>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
}
