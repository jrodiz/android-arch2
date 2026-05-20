import { App, getApps, initializeApp } from "firebase-admin/app";

/**
 * Idempotent initializer. Every entry-point file imports this once at the top
 * before touching getFirestore/getMessaging — those throw if the default app
 * isn't initialized.
 */
export function adminApp(): App {
  return getApps()[0] ?? initializeApp();
}
