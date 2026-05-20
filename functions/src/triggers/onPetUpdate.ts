import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { adminApp } from "../shared/admin.js";

adminApp();

/**
 * When a pet transitions to PURGED (the terminal state set by purgeArchivedPets),
 * synthesize a system-note message into every chat whose match was originally
 * initiated by a like on this pet. The message uses fromOwnerId = "system" so the
 * client can render it as a centered note rather than a regular bubble.
 *
 * v1 caveat: we only touch matches whose `initiatingLike.toPetId` equals this pet.
 * Matches that involved this pet via other likes (not the initiating one) are not
 * annotated — accepted limitation; can be widened later if needed.
 */
export const onPetUpdate = onDocumentUpdated("pets/{petId}", async (event) => {
  const before = event.data?.before.data();
  const after = event.data?.after.data();
  if (!before || !after) return;
  if (before.state === "PURGED" || after.state !== "PURGED") return;

  const petId = event.params.petId;
  const petName = (after.name as string | undefined) ?? "This pet";

  const db = getFirestore();
  const matches = await db
    .collection("matches")
    .where("initiatingLike.toPetId", "==", petId)
    .get();

  if (matches.empty) {
    logger.info(`onPetUpdate: no matches reference pet ${petId}`);
    return;
  }

  await Promise.all(
    matches.docs.map((m) =>
      m.ref.collection("messages").add({
        fromOwnerId: "system",
        text: `${petName} is no longer on TinPet.`,
        createdAt: FieldValue.serverTimestamp(),
        readBy: {},
      }),
    ),
  );

  logger.info(`onPetUpdate: synthesized system notes in ${matches.size} match(es) for ${petId}`);
});
