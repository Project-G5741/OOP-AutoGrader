import { STUDENT_MASCOTS } from '../components/student/studentMascots';

/** Map API pull row → UI roll outcome shape used by MascotGachaModal animations. */
export function mapServerPullToOutcome(pull) {
  const type = pull?.type ?? 'points';
  const mascotId = pull?.mascotId ?? null;
  const pointsRefund = pull?.pointsRefund ?? 0;
  const crystalAmount = pull?.crystalAmount ?? 0;

  let itemType = type;
  if (type === 'legendary') {
    itemType = 'mascot';
  }

  let id = `points_${pointsRefund}`;
  if (type === 'crystal') {
    id = 'crystal';
  } else if (mascotId) {
    id = `${type === 'legendary' ? 'legendary' : 'mascot'}_${mascotId}`;
  }

  const tierName =
    type === 'crystal'
      ? 'Crystal'
      : type === 'legendary'
        ? 'Legendary Mascot'
        : type === 'mascot'
          ? `${String(pull?.rarity || 'rare').toUpperCase()} Mascot`
          : 'Reward points';

  const image = mascotId ? STUDENT_MASCOTS[mascotId]?.image : undefined;

  return {
    item: {
      id,
      name: pull?.displayName ?? '',
      rarity: pull?.rarity ?? 'point',
      tierName,
      type: itemType,
      mascotId: mascotId ?? undefined,
      crystalAmount,
      pointsRefund,
      image,
      iconEmoji: type === 'crystal' ? undefined : type === 'points' ? undefined : '🐾',
    },
    wasPity: Boolean(pull?.wasPity),
  };
}

export function formatPercent(rate) {
  if (!Number.isFinite(rate)) return '—';
  return `${(rate * 100).toFixed(4).replace(/\.?0+$/, '')}%`;
}