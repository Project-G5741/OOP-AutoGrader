export function formatQualifiedClassName(cls, classes) {
  if (!cls?.name) return 'Untitled class';
  if (!cls.outerClassId) return cls.name;
  const outer = (classes || []).find((candidate) => candidate.id === cls.outerClassId);
  return outer ? `${outer.name}.${cls.name}` : cls.name;
}
