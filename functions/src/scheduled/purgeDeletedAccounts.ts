import { onSchedule } from "firebase-functions/v2/scheduler";
import { Timestamp, getFirestore } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { getAuth } from "firebase-admin/auth";
import { logger } from "firebase-functions";
import { adminApp } from "../shared/admin.js";

adminApp();

/**
 * Runs once a day. For each pending account deletion past its 30-day grace,
 * removes every trace of the user from Firestore + Storage + Auth.
 *
 * Order matters slightly — we delete the auth user last so a partial failure
 * leaves the user able to sign back in and cancel (which won't help recover
 * the deleted Firestore docs, but at least the account stays openable for support).
 */
export const purgeDeletedAccounts = onSchedule("every 24 hours", async () => {
  const db = getFirestore();
  const now = Timestamp.now();

  const overdue = await db
    .collection("accountDeletions")
    .where("hardDeleteAt", "<=", now)
    .limit(50)
    .get();

  if (overdue.empty) {
    logger.info("purgeDeletedAccounts: nothing to purge");
    return;
  }

  for (const doc of overdue.docs) {
    const uid = doc.id;
    try {
      await purgeUserData(uid);
      await doc.ref.delete();
      logger.info(`purgeDeletedAccounts: purged ${uid}`);
    } catch (e) {
      logger.error(`purgeDeletedAccounts: failed to purge ${uid}: ${e}`);
      // Leave the accountDeletions row in place; next run will retry.
    }
  }
});

async function purgeUserData(uid: string): Promise<void> {
  const db = getFirestore();
  const bucket = getStorage().bucket();

  // 1. Pets owned by this user — collect ids first for Storage cleanup.
  const myPets = await db.collection("pets").where("ownerId", "==", uid).get();
  for (const pet of myPets.docs) {
    await bucket
      .deleteFiles({ prefix: `pets/${pet.id}/photos/` })
      .catch((e) => logger.warn(`storage cleanup failed for pet ${pet.id}: ${e}`));
  }
  await deleteDocs(myPets.docs.map((d) => d.ref));

  // 2. Likes I sent and likes I received.
  const likesFrom = await db.collection("likes").where("fromOwnerId", "==", uid).get();
  const likesTo = await db.collection("likes").where("toOwnerId", "==", uid).get();
  await deleteDocs([...likesFrom.docs, ...likesTo.docs].map((d) => d.ref));

  // 3. Passes I made; passedLikes I dismissed.
  const passes = await db.collection("passes").where("ownerId", "==", uid).get();
  const passedLikes = await db.collection("passedLikes").where("toOwnerId", "==", uid).get();
  await deleteDocs([...passes.docs, ...passedLikes.docs].map((d) => d.ref));

  // 4. Matches I'm part of — messages subcollection is wiped by onMatchDelete.
  const myMatches = await db
    .collection("matches")
    .where("participants", "array-contains", uid)
    .get();
  await deleteDocs(myMatches.docs.map((d) => d.ref));

  // 5. FCM tokens.
  const tokens = await db.collection("fcmTokens").where("ownerId", "==", uid).get();
  await deleteDocs(tokens.docs.map((d) => d.ref));

  // 6. Blocks I initiated, and blocks that target me.
  const blocksMine = await db.collection("blocks").where("ownerId", "==", uid).get();
  const blocksAgainst = await db.collection("blocks").where("blockedOwnerId", "==", uid).get();
  await deleteDocs([...blocksMine.docs, ...blocksAgainst.docs].map((d) => d.ref));

  // 7. Data-export request doc + the exported JSON it wrote to Storage.
  await bucket
    .deleteFiles({ prefix: `data-exports/${uid}/` })
    .catch((e) => logger.warn(`storage cleanup failed for data-exports/${uid}: ${e}`));
  await db.collection("dataExports").doc(uid).delete().catch(() => undefined);

  // 8. Profile docs. `owners/{uid}` is the canonical profile — location, geohash,
  //    bio, pause state, notification prefs — and was previously left behind, leaking
  //    PII past account deletion; `users/{uid}` is the legacy mirror. Both must go. [D-015]
  await db.collection("owners").doc(uid).delete().catch(() => undefined);
  await db.collection("users").doc(uid).delete().catch(() => undefined);

  // 9. Auth user — last step.
  await getAuth().deleteUser(uid).catch((e) => {
    logger.warn(`auth.deleteUser(${uid}) failed: ${e}`);
  });
}

async function deleteDocs(refs: FirebaseFirestore.DocumentReference[]): Promise<void> {
  if (refs.length === 0) return;
  const db = getFirestore();
  // Firestore batches cap at 500 ops; chunk to 400 for headroom.
  for (let i = 0; i < refs.length; i += 400) {
    const batch = db.batch();
    refs.slice(i, i + 400).forEach((r) => batch.delete(r));
    await batch.commit();
  }
}
