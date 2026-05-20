import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { adminApp } from "../shared/admin.js";
import { sendToUser } from "../shared/fcm.js";

adminApp();

/**
 * Pushes "New match!" to both participants when a match doc lands. Source can
 * be either client-side or our own onLikeCreate — `set + merge` semantics on the
 * client + server converge on a single doc, so this fires exactly once.
 */
export const onMatchCreate = onDocumentCreated("matches/{matchId}", async (event) => {
  const match = event.data?.data();
  if (!match) return;
  const matchId = event.params.matchId;
  const participants = (match.participants as string[] | undefined) ?? [];

  await Promise.all(
    participants.map((uid) =>
      sendToUser(uid, {
        title: "New match!",
        body: "Say hello and start the chat.",
        channel: "matches",
        deepLink: `tinpet://chat/${matchId}`,
      }),
    ),
  );
});
