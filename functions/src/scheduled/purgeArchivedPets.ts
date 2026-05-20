import { onSchedule } from "firebase-functions/v2/scheduler";
import { FieldValue, Timestamp, getFirestore } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { logger } from "firebase-functions";
import { adminApp } from "../shared/admin.js";

adminApp();

const SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000;

/**
 * Runs once a day. Hard-deletes archived pets past their 7-day grace window:
 *   - removes the pet's images from Firebase Storage (best effort)
 *   - flips the pet doc to state=PURGED with photos=[] so chats can still render
 *     "<pet> is no longer on TinPet" for any historical match reference
 *
 * Replaces the client-side WorkManager job (PetPurgeWorker) — that's removable
 * once this is deployed and verified once.
 */
export const purgeArchivedPets = onSchedule("every 24 hours", async () => {
  const db = getFirestore();
  const cutoff = Timestamp.fromMillis(Date.now() - SEVEN_DAYS_MS);

  const snap = await db
    .collection("pets")
    .where("state", "==", "ARCHIVED")
    .where("deletedAt", "<=", cutoff)
    .limit(500)
    .get();

  if (snap.empty) {
    logger.info("purgeArchivedPets: nothing to purge");
    return;
  }

  const bucket = getStorage().bucket();
  for (const doc of snap.docs) {
    const petId = doc.id;
    // Best-effort photo cleanup — storage failures don't block the state flip.
    await bucket
      .deleteFiles({ prefix: `pets/${petId}/photos/` })
      .catch((e) => logger.warn(`purgeArchivedPets: failed to delete photos for ${petId}: ${e}`));

    await doc.ref.update({
      state: "PURGED",
      photos: [],
      updatedAt: FieldValue.serverTimestamp(),
    });
  }

  logger.info(`purgeArchivedPets: purged ${snap.size} pet(s)`);
});
