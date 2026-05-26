package com.rodiz.arch2.feature.profile.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.storageMetadata
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.firebase.image.AvatarImageProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
internal class AvatarUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    private val imageProcessor: AvatarImageProcessor,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    // Re-uses the users/{uid}/avatar.jpg path that sign-up writes to, so editing
    // the avatar overwrites the same blob instead of leaving an orphan.
    //
    // Wrapped in a 20s timeout because Firebase Storage's chunked resumable
    // upload can silently hang indefinitely if the underlying TCP socket is
    // blocked (Samsung Sleeping-Apps firewall, captive portal mid-session,
    // etc.) — the SDK retries internally without surfacing an error.
    //
    // Every phase is Log.i'd with the `TAG` so a logcat scan immediately shows
    // which step failed (open-stream → putStream → downloadUrl). StorageException
    // is caught explicitly and its errorCode + httpResultCode are logged — those
    // are the only signals that disambiguate "rules denied", "object too large",
    // "quota exceeded", "network", etc.
    suspend fun upload(uid: String, source: Uri): String = withContext(io) {
        val started = System.currentTimeMillis()
        Log.i(TAG, "upload: start uid=$uid source=$source")
        try {
            withTimeout(20.seconds) {
                val ref = storage.reference.child("users/$uid/avatar.jpg")
                Log.i(TAG, "upload: source mimeType=${context.contentResolver.getType(source)}")
                Log.i(TAG, "upload: processing image (downscale + re-encode)")
                val bytes = imageProcessor.process(source)
                Log.i(TAG, "upload: processed to ${bytes.size} bytes, starting putBytes")
                val metadata = storageMetadata { contentType = "image/jpeg" }
                val snapshot = ref.putBytes(bytes, metadata).await()
                Log.i(
                    TAG,
                    "upload: putBytes complete bytes=${snapshot.bytesTransferred}/${snapshot.totalByteCount}",
                )
                Log.i(TAG, "upload: requesting downloadUrl")
                val url = ref.downloadUrl.await().toString()
                Log.i(
                    TAG,
                    "upload: downloadUrl resolved in ${System.currentTimeMillis() - started}ms url=$url",
                )
                url
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "upload: TIMED OUT after 20s — last log line above marks the stuck step", e)
            throw e
        } catch (e: StorageException) {
            // errorCode is one of StorageException.ERROR_* (e.g. ERROR_NOT_AUTHORIZED,
            // ERROR_QUOTA_EXCEEDED, ERROR_OBJECT_NOT_FOUND, ERROR_RETRY_LIMIT_EXCEEDED).
            // httpResultCode is the underlying HTTP status from the GCS endpoint.
            Log.e(
                TAG,
                "upload: StorageException errorCode=${e.errorCode} httpResultCode=${e.httpResultCode} cause=${e.cause?.javaClass?.simpleName}: ${e.cause?.message}",
                e,
            )
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            Log.e(TAG, "upload: unexpected ${e.javaClass.simpleName}: ${e.message}", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "TinPet.AvatarUploader"
    }
}
