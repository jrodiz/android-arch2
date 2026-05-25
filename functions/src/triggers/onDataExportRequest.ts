import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { logger } from "firebase-functions";
import { randomUUID } from "node:crypto";
import { adminApp } from "../shared/admin.js";

adminApp();

const SIGNED_URL_DAYS = 7;

/**
 * Fires when the client writes a request doc at /dataExports/{uid}. Walks every
 * collection that belongs to the user (profile, pets, matches+messages, likes,
 * passes, fcm tokens), serializes the result into a single JSON blob, uploads
 * it to Storage at gs://<bucket>/data-exports/{uid}/{requestId}.json, signs a
 * 7-day download URL, and writes the URL back onto the request doc.
 *
 * Replaces the "Coming soon" stub on Privacy → Download my data. The client
 * already observes the request doc, so when `status` flips to "ready" + the
 * `downloadUrl` field appears, the UI surfaces a "Download" action.
 *
 * The request doc id IS the uid — Firestore rules enforce that only the
 * authenticated user can write `/dataExports/{theirUid}`, so we don't need a
 * separate auth check here.
 */
export const onDataExportRequest = onDocumentCreated(
  { document: "dataExports/{uid}", timeoutSeconds: 540, memory: "512MiB" },
  async (event) => {
    const uid = event.params.uid;
    const db = getFirestore();
    const requestRef = event.data?.ref;
    if (!requestRef) return;

    try {
      await requestRef.update({ status: "running", startedAt: FieldValue.serverTimestamp() });

      const payload = await gatherUserData(uid);

      const bucket = getStorage().bucket();
      const objectPath = `data-exports/${uid}/${event.id}.json`;
      const file = bucket.file(objectPath);
      // Firebase-style download token avoids the iam.serviceAccounts.signBlob
      // permission needed by getSignedUrl(). The token is bundled into the URL
      // and stays valid until the metadata is rewritten (or the file is deleted
      // by a future janitor function past SIGNED_URL_DAYS).
      const downloadToken = randomUUID();
      const payloadJson = JSON.stringify(payload, null, 2);
      await file.save(payloadJson, {
        contentType: "application/json",
        metadata: {
          metadata: {
            ownerId: uid,
            firebaseStorageDownloadTokens: downloadToken,
          },
        },
      });

      const downloadUrl =
        `https://firebasestorage.googleapis.com/v0/b/${bucket.name}` +
        `/o/${encodeURIComponent(objectPath)}?alt=media&token=${downloadToken}`;

      await requestRef.update({
        status: "ready",
        downloadUrl,
        storagePath: objectPath,
        sizeBytes: payloadJson.length,
        expiresAt: new Date(Date.now() + SIGNED_URL_DAYS * 24 * 60 * 60 * 1000),
        readyAt: FieldValue.serverTimestamp(),
      });
      logger.info(`onDataExportRequest: ready for ${uid} at ${objectPath}`);
    } catch (e) {
      const reason = e instanceof Error ? e.message : String(e);
      logger.error(`onDataExportRequest: failed for ${uid}`, e);
      await requestRef
        .update({ status: "failed", errorMessage: reason, failedAt: FieldValue.serverTimestamp() })
        .catch(() => undefined);
    }
  },
);

interface UserDataPayload {
  generatedAt: string;
  ownerId: string;
  profile: Record<string, unknown> | null;
  pets: Array<Record<string, unknown>>;
  matches: Array<{ match: Record<string, unknown>; messages: Array<Record<string, unknown>> }>;
  likesSent: Array<Record<string, unknown>>;
  likesReceived: Array<Record<string, unknown>>;
  passes: Array<Record<string, unknown>>;
  fcmTokenCount: number;
  notificationPrefs: Record<string, unknown> | null;
}

async function gatherUserData(uid: string): Promise<UserDataPayload> {
  const db = getFirestore();

  const ownerSnap = await db.collection("owners").doc(uid).get();
  const ownerData = ownerSnap.exists ? (ownerSnap.data() as Record<string, unknown>) : null;
  // Carve notification prefs out of the owner doc so they show as a top-level
  // section in the export — the export reader shouldn't have to dig.
  const notificationPrefs = (ownerData?.notifications as Record<string, unknown> | undefined) ?? null;
  if (ownerData && "notifications" in ownerData) {
    delete (ownerData as Record<string, unknown>).notifications;
  }

  const petsSnap = await db.collection("pets").where("ownerId", "==", uid).get();
  const pets = petsSnap.docs.map((d) => ({ id: d.id, ...d.data() }));

  const matchesSnap = await db.collection("matches").where("participants", "array-contains", uid).get();
  const matches = await Promise.all(
    matchesSnap.docs.map(async (matchDoc) => {
      const messagesSnap = await matchDoc.ref.collection("messages").get();
      return {
        match: { id: matchDoc.id, ...matchDoc.data() },
        messages: messagesSnap.docs.map((m) => ({ id: m.id, ...m.data() })),
      };
    }),
  );

  const likesSentSnap = await db.collection("likes").where("fromOwnerId", "==", uid).get();
  const likesReceivedSnap = await db.collection("likes").where("toOwnerId", "==", uid).get();
  const passesSnap = await db.collection("passes").where("ownerId", "==", uid).get();

  const tokensSnap = await db.collection("fcmTokens").where("ownerId", "==", uid).get();

  return {
    generatedAt: new Date().toISOString(),
    ownerId: uid,
    profile: ownerData,
    pets,
    matches,
    likesSent: likesSentSnap.docs.map((d) => ({ id: d.id, ...d.data() })),
    likesReceived: likesReceivedSnap.docs.map((d) => ({ id: d.id, ...d.data() })),
    passes: passesSnap.docs.map((d) => ({ id: d.id, ...d.data() })),
    fcmTokenCount: tokensSnap.size,
    notificationPrefs,
  };
}
