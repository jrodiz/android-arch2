package com.rodiz.arch2.feature.login.data.remote

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Uploads avatar images to Firebase Storage at users/{uid}/avatar.jpg and returns
 * the public download URL on success. Soft-fail: callers should keep going if the
 * upload returns null — account creation already succeeded by the time this runs.
 *
 * Wrapped in a 20s timeout so a blocked TCP socket (Samsung Sleeping-Apps
 * firewall, captive portal, etc.) surfaces as a Result.failure with a
 * TimeoutCancellationException instead of hanging the sign-up flow forever.
 */
@Singleton
internal class AvatarUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun upload(uid: String, source: Uri): Result<String> = withContext(io) {
        runCatching {
            withTimeout(20.seconds) {
                val ref = storage.reference.child("users/$uid/avatar.jpg")
                context.contentResolver.openInputStream(source).use { stream ->
                    requireNotNull(stream) { "Cannot open avatar source" }
                    ref.putStream(stream).await()
                }
                ref.downloadUrl.await().toString()
            }
        }
    }
}
