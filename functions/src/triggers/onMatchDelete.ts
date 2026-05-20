import { onDocumentDeleted } from "firebase-functions/v2/firestore";
import { getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { adminApp } from "../shared/admin.js";

adminApp();

/**
 * When a match doc is deleted (unmatch from chat overflow, or as part of an
 * account purge), Firestore does NOT auto-delete subcollections. This trigger
 * walks the messages subcollection and deletes everything in batches of 400
 * (Firestore's batch limit is 500; 400 leaves headroom).
 */
export const onMatchDelete = onDocumentDeleted("matches/{matchId}", async (event) => {
  const matchId = event.params.matchId;
  const messagesRef = getFirestore().collection("matches").doc(matchId).collection("messages");

  let deleted = 0;
  // Loop until the subcollection is empty.
  while (true) {
    const snap = await messagesRef.limit(400).get();
    if (snap.empty) break;
    const batch = getFirestore().batch();
    snap.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
    deleted += snap.size;
    if (snap.size < 400) break;
  }

  if (deleted > 0) logger.info(`onMatchDelete: cleared ${deleted} messages from match ${matchId}`);
});
