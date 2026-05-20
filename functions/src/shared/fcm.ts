import { getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { logger } from "firebase-functions";

export type Channel = "matches" | "messages" | "likes";

export interface PushPayload {
  title: string;
  body: string;
  channel: Channel;
  deepLink: string; // e.g. "tinpet://chat/abc123"
  data?: Record<string, string>;
}

/**
 * Fans a push to every FCM token registered for [uid]. Quietly skips when:
 *   - the user has no tokens (no devices opted in)
 *   - notifications.* in /users (when we have a profile collection) opts out
 *     for the relevant channel (deferred — owner-profile-settings.md follow-up)
 *
 * Stale tokens (UNREGISTERED / INVALID_ARGUMENT from FCM) are deleted in-place
 * so the next push doesn't waste a multicast slot on them.
 */
export async function sendToUser(uid: string, payload: PushPayload): Promise<void> {
  const db = getFirestore();
  const tokensSnap = await db.collection("fcmTokens").where("ownerId", "==", uid).get();
  if (tokensSnap.empty) {
    logger.info(`No FCM tokens registered for ${uid} — push skipped`);
    return;
  }

  const tokenDocs = tokensSnap.docs;
  const tokens = tokenDocs.map((d) => d.data().token as string).filter(Boolean);
  if (tokens.length === 0) return;

  const response = await getMessaging().sendEachForMulticast({
    tokens,
    notification: { title: payload.title, body: payload.body },
    android: {
      notification: { channelId: payload.channel },
      priority: payload.channel === "messages" || payload.channel === "matches" ? "high" : "normal",
    },
    data: {
      deepLink: payload.deepLink,
      ...(payload.data ?? {}),
    },
  });

  // Best-effort cleanup of dead tokens.
  const stale: Promise<unknown>[] = [];
  response.responses.forEach((resp, i) => {
    if (resp.success) return;
    const code = resp.error?.code ?? "";
    if (
      code === "messaging/registration-token-not-registered" ||
      code === "messaging/invalid-registration-token" ||
      code === "messaging/invalid-argument"
    ) {
      stale.push(tokenDocs[i].ref.delete());
    } else {
      logger.warn(`FCM error for token ${tokens[i].slice(0, 8)}…: ${code}`);
    }
  });
  await Promise.all(stale);
}
