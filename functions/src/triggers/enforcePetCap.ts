import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { getFirestore } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { logger } from "firebase-functions";
import { adminApp } from "../shared/admin.js";

adminApp();

const PET_CAP = 5;

/**
 * Server-side guard on the 5-active-pets-per-owner cap (see plan
 * `pet-my-pets-redesign.md` §2). The Android UI hides the "+" button when the
 * owner already has 5 active pets, but a malicious client could write directly.
 * On any new pet doc, count the owner's other ACTIVE pets — if we're over the
 * cap, delete this pet's doc + storage photos and log a warning.
 *
 * Race note: two pets created within the same Firestore round-trip can both
 * "see" the under-cap state and both succeed. Deterministic deduping (sort by
 * createdAt, drop tail) would require a transaction; for the dev project's
 * single-user reality this is acceptable. Hardening is a follow-up.
 */
export const enforcePetCap = onDocumentCreated("pets/{petId}", async (event) => {
  const pet = event.data?.data();
  if (!pet) return;
  const petId = event.params.petId;
  const ownerId = pet.ownerId as string | undefined;
  const state = pet.state as string | undefined;
  if (!ownerId || state !== "ACTIVE") return;

  const db = getFirestore();
  const myActive = await db
    .collection("pets")
    .where("ownerId", "==", ownerId)
    .where("state", "==", "ACTIVE")
    .get();

  if (myActive.size <= PET_CAP) return;

  // Over cap — purge this pet. Photos go first (best-effort), then the doc.
  const bucket = getStorage().bucket();
  await bucket
    .deleteFiles({ prefix: `pets/${petId}/photos/` })
    .catch((e) => logger.warn(`enforcePetCap: photo cleanup failed for ${petId}: ${e}`));
  await event.data!.ref.delete().catch((e) =>
    logger.warn(`enforcePetCap: doc delete failed for ${petId}: ${e}`),
  );
  logger.warn(
    `enforcePetCap: deleted ${petId} for owner ${ownerId} (had ${myActive.size} active, cap is ${PET_CAP})`,
  );
});
