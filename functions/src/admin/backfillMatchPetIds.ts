import { onRequest } from "firebase-functions/v2/https";
import { getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { adminApp } from "../shared/admin.js";

adminApp();

// Shared secret for the one-off backfill. Dev project only; rotate or remove the
// function entirely once the existing matches are populated.
const BACKFILL_SECRET = "pet-id-backfill-2026-05-23";

/**
 * One-off HTTPS endpoint that walks the `matches` collection and fills in the
 * `petAId` / `petBId` fields for documents created before `onLikeCreate` started
 * writing them. For each match, looks up the two reciprocal likes:
 *   - the like from ownerA → ownerB has `toPetId` = ownerB's pet (petBId)
 *   - the like from ownerB → ownerA has `toPetId` = ownerA's pet (petAId)
 *
 * Idempotent: matches that already have both pet IDs are skipped. Invoke via
 *   curl "https://<region>-<project>.cloudfunctions.net/backfillMatchPetIds?secret=<SECRET>"
 *
 * Safe to leave deployed (it's protected by the shared secret and is a no-op on
 * already-backfilled docs) but can be removed once verified.
 */
export const backfillMatchPetIds = onRequest({ timeoutSeconds: 540 }, async (req, res) => {
  if (req.query.secret !== BACKFILL_SECRET) {
    res.status(401).send("Unauthorized");
    return;
  }

  const db = getFirestore();
  const matchesSnap = await db.collection("matches").get();

  let updated = 0;
  let alreadyComplete = 0;
  let stillIncomplete = 0;
  let failed = 0;
  const failures: Array<{ matchId: string; reason: string }> = [];

  for (const matchDoc of matchesSnap.docs) {
    const data = matchDoc.data();
    const ownerAId = data.ownerAId as string | undefined;
    const ownerBId = data.ownerBId as string | undefined;
    const existingPetAId = data.petAId as string | undefined;
    const existingPetBId = data.petBId as string | undefined;

    if (existingPetAId && existingPetBId) {
      alreadyComplete++;
      continue;
    }
    if (!ownerAId || !ownerBId) {
      failed++;
      failures.push({ matchId: matchDoc.id, reason: "missing owner ids" });
      continue;
    }

    try {
      const patch: Record<string, string> = {};

      if (!existingPetAId) {
        const bToA = await db.collection("likes")
          .where("fromOwnerId", "==", ownerBId)
          .where("toOwnerId", "==", ownerAId)
          .limit(1)
          .get();
        const petAId = bToA.docs[0]?.data().toPetId as string | undefined;
        if (petAId) patch.petAId = petAId;
      }
      if (!existingPetBId) {
        const aToB = await db.collection("likes")
          .where("fromOwnerId", "==", ownerAId)
          .where("toOwnerId", "==", ownerBId)
          .limit(1)
          .get();
        const petBId = aToB.docs[0]?.data().toPetId as string | undefined;
        if (petBId) patch.petBId = petBId;
      }

      if (Object.keys(patch).length > 0) {
        await matchDoc.ref.update(patch);
        updated++;
        // If we still don't have both after the patch (one of the reciprocal
        // likes was deleted), surface that as incomplete rather than success.
        if (!(existingPetAId || patch.petAId) || !(existingPetBId || patch.petBId)) {
          stillIncomplete++;
        }
      } else {
        stillIncomplete++;
        failures.push({ matchId: matchDoc.id, reason: "reciprocal likes not found" });
      }
    } catch (e) {
      failed++;
      const reason = e instanceof Error ? e.message : String(e);
      failures.push({ matchId: matchDoc.id, reason });
      logger.error(`Backfill failed for ${matchDoc.id}`, e);
    }
  }

  const report = {
    totalMatches: matchesSnap.size,
    updated,
    alreadyComplete,
    stillIncomplete,
    failed,
    failures: failures.slice(0, 20),
  };
  logger.info("backfillMatchPetIds report", report);
  res.json(report);
});
