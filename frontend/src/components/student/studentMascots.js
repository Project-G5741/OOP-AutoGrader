export const STUDENT_MASCOTS = {
  fox: {
    name: 'Fox',
    directions: '/mascots/fox-directions.webp',
    reactions: '/mascots/fox-reactions.webp',
  },
  bear: {
    name: 'Bear',
    directions: '/mascots/bear-directions.webp',
    reactions: '/mascots/bear-reactions.webp',
  },
  panda: {
    name: 'Panda',
    directions: '/mascots/panda-directions.webp',
    reactions: '/mascots/panda-reactions.webp',
  },
  penguin: {
    name: 'Penguin',
    directions: '/mascots/penguin-directions.webp',
    reactions: '/mascots/penguin-reactions.webp',
  },
  bunny: {
    name: 'Bunny',
    directions: '/mascots/bunny-directions.webp',
    reactions: '/mascots/bunny-reactions.webp',
  },
  cat: {
    name: 'Cat',
    directions: '/mascots/cat-directions.webp',
    reactions: '/mascots/cat-reactions.webp',
  },
};

export function mascotArt(id) {
  return STUDENT_MASCOTS[id] || STUDENT_MASCOTS.fox;
}
