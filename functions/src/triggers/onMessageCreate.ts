import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { adminApp } from "../shared/admin.js";
import { sendToUser } from "../shared/fcm.js";

adminApp();

/**
 * Pushes "<name> sent you a message" to the recipient (not the sender). The
 * client already writes lastMessageAt / lastMessagePreview / lastMessageFromOwnerId
 * on the parent match doc atomically (match-and-chat.md §5.2), so no DB writes
 * are needed here.
 */
export const onMessageCreate = onDocumentCreated(
  "matches/{matchId}/messages/{messageId}",
  async (event) => {
    const message = event.data?.data();
    if (!message) return;
    const matchId = event.params.matchId;
    const fromOwnerId = message.fromOwnerId as string;
    const text = (message.text as string | undefined) ?? "";

    const matchSnap = await getFirestore().collection("matches").doc(matchId).get();
    const participants = (matchSnap.get("participants") as string[] | undefined) ?? [];
    const recipient = participants.find((uid) => uid !== fromOwnerId);
    if (!recipient) {
      logger.warn(`onMessageCreate: no recipient on match ${matchId}`);
      return;
    }

    await sendToUser(recipient, {
      title: "New message",
      body: text.slice(0, 100),
      channel: "messages",
      deepLink: `tinpet://chat/${matchId}`,
      data: { matchId, messageId: event.params.messageId },
    });
  },
);
