import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { adminApp } from "../shared/admin.js";
import { sendToUser } from "../shared/fcm.js";
import { matchIdFor } from "../shared/matching.js";

adminApp();

/**
 * Fires on every new like. Two responsibilities:
 *   1. Push "Someone liked your pet" to the recipient (likes channel).
 *   2. Check reciprocity: did the recipient previously like any of the sender's
 *      pets? If yes, atomically create the deterministic match doc — replaces
 *      the client-side racy reciprocity check from deck-swipe.md §5.4.
 */
export const onLikeCreate = onDocumentCreated("likes/{likeId}", async (event) => {
  const like = event.data?.data();
  if (!like) return;
  const fromOwnerId = like.fromOwnerId as string;
  const toOwnerId = like.toOwnerId as string;
  const toPetId = like.toPetId as string;

  // 1. Push to recipient.
  await sendToUser(toOwnerId, {
    title: "Someone liked your pet",
    body: "Tap to see who's interested.",
    channel: "likes",
    deepLink: "tinpet://likes",
  });

  // 2. Reciprocity check.
  const db = getFirestore();
  const reciprocal = await db
    .collection("likes")
    .where("fromOwnerId", "==", toOwnerId)
    .where("toOwnerId", "==", fromOwnerId)
    .limit(1)
    .get();
  if (reciprocal.empty) return;

  // 3. Mutual — write the match doc. Idempotent: deterministic id + merge means
  //    concurrent likes from both sides converge on identical data.
  const matchId = matchIdFor(fromOwnerId, toOwnerId);
  const [ownerAId, ownerBId] = matchId.split("_");
  await db.collection("matches").doc(matchId).set(
    {
      ownerAId,
      ownerBId,
      participants: [ownerAId, ownerBId],
      createdAt: FieldValue.serverTimestamp(),
      initiatingLike: { fromOwnerId, toPetId },
    },
    { merge: true },
  );

  logger.info(`Match created/updated: ${matchId}`);
});
