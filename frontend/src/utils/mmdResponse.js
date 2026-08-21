/** Normalize `/mmd` API and `lab_result.mmd` shapes to `{ classes, parseError }`. */
export function parseMmdResponse(json) {
  if (Array.isArray(json)) {
    return { classes: json, parseError: null };
  }
  return {
    classes: json?.classes ?? [],
    parseError: json?.parseError ?? null,
  };
}

/** Extract MMD payload from a challenge bundle (`lab_result.challenge_N`). */
export function mmdFromChallengeBundle(bundle) {
  if (!bundle) {
    return { classes: [], parseError: null };
  }
  return parseMmdResponse(bundle.mmd);
}
