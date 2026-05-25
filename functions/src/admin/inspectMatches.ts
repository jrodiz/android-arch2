import { onRequest } from "firebase-functions/v2/https";
import { getFirestore } from "firebase-admin/firestore";
import { getAuth } from "firebase-admin/auth";
import { adminApp } from "../shared/admin.js";

adminApp();

const INSPECT_SECRET = "tinpet-inspect-2026-05-25";

/**
 * Dev-only debugging endpoint. Dumps the matches collection with
 * participant emails resolved from Auth so a human can read who's in each
 * match without juggling raw UIDs. Used after the post-seed test set
 * showed an empty Matches inbox even though backfillMatchPetIds reported
 * a non-zero match count — needed to confirm whether the orphaned match
 * still pointed at any signed-in account.
 */
export const inspectMatches = onRequest(async (req, res) => {
  if (req.query.secret !== INSPECT_SECRET) {
    res.status(401).send("Unauthorized");
    return;
  }

  const db = getFirestore();
  const auth = getAuth();
  const snap = await db.collection("matches").get();

  const matches = await Promise.all(
    snap.docs.map(async (doc) => {
      const data = doc.data();
      const participants = (data.participants as string[] | undefined) ?? [];
      const resolved = await Promise.all(
        participants.map(async (uid) => {
          try {
            const user = await auth.getUser(uid);
            return { uid, email: user.email ?? null };
          } catch {
            return { uid, email: null, note: "auth user missing" };
          }
        }),
      );
      return {
        id: doc.id,
        participants: resolved,
        petAId: data.petAId ?? null,
        petBId: data.petBId ?? null,
        hasLastMessage: data.lastMessageAt != null,
        lastMessagePreview: data.lastMessagePreview ?? null,
      };
    }),
  );

  res.json({ total: snap.size, matches });
});
