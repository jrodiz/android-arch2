# Avatar image compression / downscale

Reduce profile-picture size before uploading to Firebase Storage, in both the
**Sign Up** flow (`:feature:login`) and the **Edit Profile** flow
(`:feature:profile` upload, driven from `:feature:settings` UI), **without
changing the aspect ratio** and without visible quality loss.

See `ANDROID_APP_SCAFFOLD_PROMPT.md` for module conventions.

## Problem

Today both upload paths stream the **raw, full-resolution** picked image
straight to Storage with no processing:

- `feature/profile/data/.../AvatarUploader.kt` → `putStream(openInputStream(uri))`
- `feature/login/data/.../remote/AvatarUploader.kt` → same

A 12 MP phone photo (~5–8 MB) is uploaded as-is. Slow uploads, wasted Storage
and bandwidth, and EXIF-rotated photos can display sideways.

## Decisions (confirmed with user)

- **Home:** existing `:core:firebase` module (Android + Hilt; already provides
  `FirebaseStorage` at `SingletonComponent` and is already a dependency of both
  `:feature:profile:data` and `:feature:login:data`).
- **Output:** JPEG, longest edge ≤ **2048 px**, quality **90**.
- **Aspect ratio:** preserved (uniform downscale by longest edge; **no crop**).
  UI already renders with `ContentScale.Crop` into a rounded square at display
  time, so the stored image stays the original shape.

## Design

New public, injectable class in `:core:firebase`:

```
core/firebase/src/main/kotlin/com/rodiz/arch2/core/firebase/image/AvatarImageProcessor.kt
```

```kotlin
@Singleton
class AvatarImageProcessor @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher io: CoroutineDispatcher,
) {
    /** Decode → EXIF-correct → downscale (≤2048 longest edge, ratio kept) → JPEG q90 bytes. */
    suspend fun process(source: Uri): ByteArray
}
```

Decode strategy (minSdk 26):
- **API ≥ 28:** `ImageDecoder` with `setTargetSize` — auto-applies EXIF
  orientation and decodes already-scaled (low memory).
- **API 26–27:** `BitmapFactory` two-pass: read bounds → `inSampleSize` (power
  of two, avoids OOM) → exact `Bitmap.createScaledBitmap` to the target →
  rotate per `ExifInterface` orientation tag.

Then `bitmap.compress(JPEG, 90, ByteArrayOutputStream)` → `ByteArray`.
Bitmaps are `recycle()`d in `finally`.

### Uploader integration

Switch both uploaders from `putStream(stream)` to `putBytes(processedBytes)` —
no temp file, no cleanup, and after compression the payload (~300–500 KB) is
well within `putBytes` limits. Existing 20 s timeout + logging structure kept.

- `:feature:profile:data` `AvatarUploader`: inject `AvatarImageProcessor`, call
  `process(source)` before upload; keep `String` return + rich logging.
- `:feature:login:data` `AvatarUploader`: same; keep `Result<String>` soft-fail.
  If `process` throws (corrupt/unsupported image), the existing `runCatching`
  turns it into `Result.failure` → sign-up still succeeds (soft-fail intact).

Set `StorageMetadata { contentType = "image/jpeg" }` on the put.

## Files

- **add** `gradle/libs.versions.toml`: `androidx-exifinterface` (version `1.3.7`).
- **add** `core/firebase/build.gradle.kts`: `implementation(libs.androidx.exifinterface)`.
- **add** `core/firebase/.../image/AvatarImageProcessor.kt`.
- **edit** `feature/profile/data/.../AvatarUploader.kt`.
- **edit** `feature/login/data/.../remote/AvatarUploader.kt`.
- **add** unit test for `AvatarImageProcessor` (Robolectric) — verifies a
  3000×4000 bitmap comes back ≤ 2048 longest edge with ratio preserved.

## Verify

- `JAVA_HOME=…jbr-17… ./gradlew assembleDebug` and `./gradlew test`.
- Install on emulator, change avatar in Edit Profile and pick one at Sign Up;
  confirm upload succeeds and `users/{uid}/avatar.jpg` is small. Screenshot.
