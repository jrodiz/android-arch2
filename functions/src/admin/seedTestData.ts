import { onRequest } from "firebase-functions/v2/https";
import { FieldValue, GeoPoint, getFirestore } from "firebase-admin/firestore";
import { getAuth, UserRecord } from "firebase-admin/auth";
import { logger } from "firebase-functions";
import { randomUUID } from "node:crypto";
import { adminApp } from "../shared/admin.js";

adminApp();

/**
 * One-off seed endpoint to populate the dev project with several fake
 * owners + pets so the user can sign in as each and exercise multi-account
 * flows (matching, chatting, blocking, etc.) without leaving the emulator.
 *
 * All accounts share the same password so they're trivial to flip between.
 * Locations are scattered around midtown Manhattan so they all fall inside
 * the default 25 km filter radius. Photos use the deterministic
 * picsum/pravatar URLs — no Storage upload, just public download URLs that
 * survive multiple runs.
 *
 * Idempotent: re-running upserts each owner doc + pets. The auth user is
 * recreated on demand if it already exists (we soft-fail the create and
 * look up the existing uid by email).
 *
 * Invoke:
 *   curl "https://us-central1-arch2-cac87.cloudfunctions.net/seedTestData?secret=<SECRET>"
 */
const SEED_SECRET = "tinpet-seed-2026-05-25";
const SHARED_PASSWORD = "TinPetTest2026!";

interface SeedOwner {
  email: string;
  firstName: string;
  bio: string;
  /** Approximate IRL coords; small spread keeps everyone inside the 25 km filter. */
  lat: number;
  lng: number;
}

interface SeedPet {
  name: string;
  ageYears: number;
  species: Species;
  breed?: string;
  intents: Intent[];
  bio?: string;
  size?: PetSize;
  energy?: PetEnergy;
  /** Public photo URLs, no Storage upload. */
  photoUrls: string[];
}

type Species =
  | "DOG"
  | "CAT"
  | "RABBIT"
  | "HAMSTER"
  | "GUINEA_PIG"
  | "FERRET"
  | "OTHER_SMALL_MAMMAL";
type Intent = "PLAYDATE" | "ADOPTION" | "FRIENDSHIP";
type PetSize = "SMALL" | "MEDIUM" | "LARGE";
type PetEnergy = "CALM" | "MEDIUM" | "HIGH";

const OWNERS: Array<SeedOwner & { pets: SeedPet[] }> = [
  {
    email: "lena@tinpet.dev",
    firstName: "Lena",
    bio: "Vet tech in Midtown — always down for a park visit.",
    lat: 40.7484,
    lng: -73.9857,
    pets: [
      {
        name: "Mochi",
        ageYears: 3,
        species: "DOG",
        breed: "Shiba Inu",
        intents: ["PLAYDATE", "FRIENDSHIP"],
        bio: "Loves long walks across the bridge. Will trade peanut butter for affection.",
        size: "MEDIUM",
        energy: "HIGH",
        photoUrls: [
          "https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=720",
          "https://images.unsplash.com/photo-1583511655826-05700d52f4d9?w=720",
        ],
      },
      {
        name: "Sprout",
        ageYears: 1,
        species: "RABBIT",
        breed: "Holland Lop",
        intents: ["FRIENDSHIP"],
        bio: "Tiny but mighty. Excellent at sitting in laps for exactly 7 minutes.",
        size: "SMALL",
        energy: "CALM",
        photoUrls: ["https://images.unsplash.com/photo-1535241749838-299277b6305f?w=720"],
      },
    ],
  },
  {
    email: "marcus@tinpet.dev",
    firstName: "Marcus",
    bio: "Architect, runner, dad to one very dramatic cat.",
    lat: 40.7580,
    lng: -73.9855,
    pets: [
      {
        name: "Otto",
        ageYears: 5,
        species: "DOG",
        breed: "Pug",
        intents: ["PLAYDATE", "ADOPTION"],
        bio: "Snores loudly. Stares deeply into your soul before eating your sandwich.",
        size: "SMALL",
        energy: "MEDIUM",
        photoUrls: [
          "https://images.unsplash.com/photo-1517849845537-4d257902454a?w=720",
          "https://images.unsplash.com/photo-1546238232-20216dec9f72?w=720",
        ],
      },
      {
        name: "Pumpkin",
        ageYears: 2,
        species: "CAT",
        breed: "Maine Coon",
        intents: ["FRIENDSHIP"],
        bio: "Owns the apartment. Tolerates Marcus.",
        size: "LARGE",
        energy: "CALM",
        photoUrls: ["https://images.unsplash.com/photo-1561948955-570b270e7c36?w=720"],
      },
    ],
  },
  {
    email: "ayaan@tinpet.dev",
    firstName: "Ayaan",
    bio: "Grad student in Queens. Looking for puppy playdates.",
    lat: 40.7282,
    lng: -73.7949,
    pets: [
      {
        name: "Biscuit",
        ageYears: 1,
        species: "DOG",
        breed: "Corgi",
        intents: ["PLAYDATE"],
        bio: "Smol legs, big personality. Hates getting wet, loves stealing socks.",
        size: "SMALL",
        energy: "HIGH",
        photoUrls: [
          "https://images.unsplash.com/photo-1612536057832-2ff7ead58194?w=720",
          "https://images.unsplash.com/photo-1591946614720-90a587da4a36?w=720",
        ],
      },
    ],
  },
  {
    email: "sofia@tinpet.dev",
    firstName: "Sofia",
    bio: "Bike commuter, weekend hiker — pups welcome to tag along.",
    lat: 40.7300,
    lng: -74.0000,
    pets: [
      {
        name: "Cloud",
        ageYears: 4,
        species: "CAT",
        breed: "British Shorthair",
        intents: ["FRIENDSHIP", "ADOPTION"],
        bio: "Adopt-don't-shop ambassador. Available to a quiet home with a sunny window.",
        size: "MEDIUM",
        energy: "CALM",
        photoUrls: [
          "https://images.unsplash.com/photo-1574144611937-0df059b5ef3e?w=720",
          "https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=720",
        ],
      },
      {
        name: "Pearl",
        ageYears: 2,
        species: "RABBIT",
        breed: "Mini Rex",
        intents: ["FRIENDSHIP"],
        bio: "Velveteen. Will headbutt your hand until you pet her ears.",
        size: "SMALL",
        energy: "MEDIUM",
        photoUrls: ["https://images.unsplash.com/photo-1452857297128-d9c29adba80b?w=720"],
      },
      {
        name: "Bandit",
        ageYears: 3,
        species: "FERRET",
        intents: ["PLAYDATE"],
        bio: "Has stolen 12 hair ties this month. We've stopped counting.",
        size: "SMALL",
        energy: "HIGH",
        photoUrls: ["https://images.unsplash.com/photo-1591343395082-e120087004b4?w=720"],
      },
    ],
  },
  {
    email: "noor@tinpet.dev",
    firstName: "Noor",
    bio: "Mostly cats. Sometimes humans.",
    lat: 40.7500,
    lng: -73.9700,
    pets: [
      {
        name: "Rex",
        ageYears: 7,
        species: "DOG",
        breed: "Golden Retriever",
        intents: ["FRIENDSHIP"],
        bio: "Silver muzzle, golden heart. Greets every dog at the dog run by name.",
        size: "LARGE",
        energy: "MEDIUM",
        photoUrls: [
          "https://images.unsplash.com/photo-1552053831-71594a27632d?w=720",
          "https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=720",
        ],
      },
      {
        name: "Saffron",
        ageYears: 5,
        species: "HAMSTER",
        breed: "Syrian",
        intents: ["FRIENDSHIP"],
        bio: "Nocturnal. Don't ask about the wheel.",
        size: "SMALL",
        energy: "MEDIUM",
        photoUrls: ["https://images.unsplash.com/photo-1425082661705-1834bfd09dca?w=720"],
      },
    ],
  },
];

export const seedTestData = onRequest(
  { timeoutSeconds: 300, memory: "512MiB" },
  async (req, res) => {
    if (req.query.secret !== SEED_SECRET) {
      res.status(401).send("Unauthorized");
      return;
    }

    const db = getFirestore();
    const auth = getAuth();
    const created: Array<{ email: string; firstName: string; uid: string; petCount: number }> = [];

    for (const owner of OWNERS) {
      try {
        const authUser = await ensureAuthUser(auth, owner.email, SHARED_PASSWORD, owner.firstName);
        const uid = authUser.uid;

        await db.collection("owners").doc(uid).set(
          {
            firstName: owner.firstName,
            avatarUrl: `https://i.pravatar.cc/300?u=${encodeURIComponent(owner.email)}`,
            paused: false,
            bio: owner.bio,
            location: new GeoPoint(owner.lat, owner.lng),
            geohash: encodeGeohash(owner.lat, owner.lng, 6),
            updatedAt: FieldValue.serverTimestamp(),
          },
          { merge: true },
        );

        // Wipe any pets this seed user previously had so re-runs stay deterministic
        // (otherwise repeated invocations would multiply each owner's pet count).
        const existingPets = await db.collection("pets").where("ownerId", "==", uid).get();
        await Promise.all(existingPets.docs.map((d) => d.ref.delete()));

        for (const pet of owner.pets) {
          const petRef = db.collection("pets").doc();
          const photos = pet.photoUrls.map((url) => ({
            id: randomUUID(),
            storagePath: `seeded/${owner.email}/${url.split("/").pop()}`,
            downloadUrl: url,
          }));
          await petRef.set({
            ownerId: uid,
            name: pet.name,
            ageYears: pet.ageYears,
            ageIsApproximate: false,
            species: pet.species,
            speciesCategory: speciesCategoryFor(pet.species),
            breed: pet.breed ?? null,
            intents: pet.intents,
            photos,
            bio: pet.bio ?? null,
            size: pet.size ?? null,
            energy: pet.energy ?? null,
            state: "ACTIVE",
            enabled: true,
            createdAt: FieldValue.serverTimestamp(),
            updatedAt: FieldValue.serverTimestamp(),
            deletedAt: null,
          });
        }

        created.push({ email: owner.email, firstName: owner.firstName, uid, petCount: owner.pets.length });
        logger.info(`seedTestData: ${owner.email} (${uid}) — ${owner.pets.length} pets`);
      } catch (e) {
        logger.error(`seedTestData failed for ${owner.email}`, e);
      }
    }

    res.json({
      password: SHARED_PASSWORD,
      created,
      tip: "Sign in with any of these emails + the shared password to test multi-account flows.",
    });
  },
);

async function ensureAuthUser(
  auth: ReturnType<typeof getAuth>,
  email: string,
  password: string,
  displayName: string,
): Promise<UserRecord> {
  try {
    return await auth.createUser({ email, password, displayName, emailVerified: true });
  } catch (e: unknown) {
    const code = (e as { code?: string } | undefined)?.code;
    if (code === "auth/email-already-exists") {
      const existing = await auth.getUserByEmail(email);
      // Reset password so the documented shared one always works.
      await auth.updateUser(existing.uid, { password, displayName, emailVerified: true });
      return existing;
    }
    throw e;
  }
}

function speciesCategoryFor(species: Species): string {
  switch (species) {
    case "DOG":
      return "DOGS";
    case "CAT":
      return "CATS";
    default:
      return "SMALL_MAMMALS";
  }
}

/**
 * Port of core/common/geo/Geohash.kt — same algorithm so seeded owners match
 * the precision the Android client writes (6 chars ≈ 1.2 × 0.6 km cells).
 */
const BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
function encodeGeohash(latitude: number, longitude: number, precision = 6): string {
  let latLo = -90.0;
  let latHi = 90.0;
  let lngLo = -180.0;
  let lngHi = 180.0;
  let out = "";
  let bit = 0;
  let ch = 0;
  let even = true;
  while (out.length < precision) {
    if (even) {
      const mid = (lngLo + lngHi) / 2;
      if (longitude >= mid) {
        ch = (ch << 1) | 1;
        lngLo = mid;
      } else {
        ch = ch << 1;
        lngHi = mid;
      }
    } else {
      const mid = (latLo + latHi) / 2;
      if (latitude >= mid) {
        ch = (ch << 1) | 1;
        latLo = mid;
      } else {
        ch = ch << 1;
        latHi = mid;
      }
    }
    even = !even;
    bit++;
    if (bit === 5) {
      out += BASE32[ch];
      bit = 0;
      ch = 0;
    }
  }
  return out;
}
