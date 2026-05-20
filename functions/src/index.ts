// Entry point — Firebase deploys whichever functions are exported here.

export { onLikeCreate } from "./triggers/onLikeCreate.js";
export { onMatchCreate } from "./triggers/onMatchCreate.js";
export { onMessageCreate } from "./triggers/onMessageCreate.js";
export { onMatchDelete } from "./triggers/onMatchDelete.js";
export { onPetUpdate } from "./triggers/onPetUpdate.js";

export { purgeArchivedPets } from "./scheduled/purgeArchivedPets.js";
export { purgeDeletedAccounts } from "./scheduled/purgeDeletedAccounts.js";
