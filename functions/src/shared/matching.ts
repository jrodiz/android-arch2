/**
 * Deterministic match id from two owner uids — lexicographic sort joined by "_".
 * Lets `set(matchId)` be safely idempotent under concurrent mutual likes.
 */
export function matchIdFor(uidA: string, uidB: string): string {
  return [uidA, uidB].sort().join("_");
}
