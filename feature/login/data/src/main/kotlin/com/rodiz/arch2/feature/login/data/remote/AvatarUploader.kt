package com.rodiz.arch2.feature.login.data.remote

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Uploads avatar images to Firebase Storage at users/{uid}/avatar.jpg and returns
 * the public download URL on success. Soft-fail: callers should keep going if the
 * upload returns null — account creation already succeeded by the time this runs.
 */
@Singleton
internal class AvatarUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun upload(uid: String, source: Uri): Result<String> = withContext(io) {
        runCatching {
            val ref = storage.reference.child("users/$uid/avatar.jpg")
            context.contentResolver.openInputStream(source).use { stream ->
                requireNotNull(stream) { "Cannot open avatar source" }
                ref.putStream(stream).await()
            }
            ref.downloadUrl.await().toString()
        }
    }
}
