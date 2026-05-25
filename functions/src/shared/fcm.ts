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

interface NotificationPrefs {
  newMatch?: boolean;
  newMessage?: boolean;
  someoneLiked?: boolean;
  weeklyDigest?: boolean;
  quietHoursEnabled?: boolean;
  quietHoursStartMinutes?: number;
  quietHoursEndMinutes?: number;
}

/**
 * Fans a push to every FCM token registered for [uid]. Quietly skips when:
 *   - the user has no tokens (no devices opted in)
 *   - the user opted out of this channel (notifications.newMatch / .newMessage /
 *     .someoneLiked on /owners/{uid})
 *   - the user enabled quiet hours and the server clock is currently inside
 *     that window (caveat below)
 *
 * Quiet-hours TZ caveat: the user's timezone isn't stored yet, so the window
 * is evaluated against server UTC. This is wrong for users not on UTC — a
 * follow-up adds `notifications.tz` to /owners/{uid} so the client can write
 * its IANA zone alongside the start/end minutes. For now: acceptable v1
 * because the alternative is zero server-side enforcement.
 *
 * Stale tokens (UNREGISTERED / INVALID_ARGUMENT from FCM) are deleted in-place
 * so the next push doesn't waste a multicast slot on them.
 */
export async function sendToUser(uid: string, payload: PushPayload): Promise<void> {
  const db = getFirestore();

  // Per-channel + quiet-hours guard. Defaults to "send" when prefs are missing
  // (owner doc never written, or notifications map absent) so first-run users
  // still get pushes until they explicitly opt out.
  const ownerSnap = await db.collection("owners").doc(uid).get();
  const prefs = (ownerSnap.get("notifications") as NotificationPrefs | undefined) ?? {};
  if (!isChannelEnabled(payload.channel, prefs)) {
    logger.info(`Push to ${uid} skipped — channel ${payload.channel} opted out`);
    return;
  }
  if (isInQuietHours(prefs)) {
    logger.info(`Push to ${uid} skipped — within quiet hours (UTC)`);
    return;
  }

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

function isChannelEnabled(channel: Channel, prefs: NotificationPrefs): boolean {
  const key: keyof NotificationPrefs =
    channel === "matches" ? "newMatch"
      : channel === "messages" ? "newMessage"
        : "someoneLiked";
  // Absent / true → allowed. Only an explicit `false` blocks.
  return prefs[key] !== false;
}

function isInQuietHours(prefs: NotificationPrefs): boolean {
  if (prefs.quietHoursEnabled !== true) return false;
  const start = prefs.quietHoursStartMinutes;
  const end = prefs.quietHoursEndMinutes;
  if (typeof start !== "number" || typeof end !== "number" || start === end) return false;
  const now = new Date();
  const nowMinutes = now.getUTCHours() * 60 + now.getUTCMinutes();
  if (start < end) {
    return nowMinutes >= start && nowMinutes < end;
  }
  // Wrap past midnight: e.g. 22:00 → 08:00 means nowMinutes >= 22:00 OR < 08:00.
  return nowMinutes >= start || nowMinutes < end;
}
