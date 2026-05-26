package com.rodiz.arch2.feature.profile.data

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

@Singleton
internal class AvatarUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    // Re-uses the users/{uid}/avatar.jpg path that sign-up writes to, so editing
    // the avatar overwrites the same blob instead of leaving an orphan.
    //
    // The upload is wrapped in a 60s timeout because Firebase Storage's chunked
    // resumable upload will silently hang indefinitely if the underlying TCP
    // socket is blocked (Samsung Sleeping-Apps firewall, captive portal mid-
    // session, etc.) — the SDK retries internally without surfacing an error,
    // and the UI just spins forever. A `TimeoutCancellationException` here
    // surfaces up to the ViewModel where it's mapped to a user-visible
    // "Upload timed out — check your connection" snackbar.
    suspend fun upload(uid: String, source: Uri): String = withContext(io) {
        withTimeout(60.seconds) {
            val ref = storage.reference.child("users/$uid/avatar.jpg")
            context.contentResolver.openInputStream(source).use { stream ->
                requireNotNull(stream) { "Cannot open avatar source" }
                ref.putStream(stream).await()
            }
            ref.downloadUrl.await().toString()
        }
    }
}
