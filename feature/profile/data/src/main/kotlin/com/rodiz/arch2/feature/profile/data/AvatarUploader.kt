package com.rodiz.arch2.feature.profile.data

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

@Singleton
internal class AvatarUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    // Re-uses the users/{uid}/avatar.jpg path that sign-up writes to, so editing
    // the avatar overwrites the same blob instead of leaving an orphan.
    suspend fun upload(uid: String, source: Uri): String = withContext(io) {
        val ref = storage.reference.child("users/$uid/avatar.jpg")
        context.contentResolver.openInputStream(source).use { stream ->
            requireNotNull(stream) { "Cannot open avatar source" }
            ref.putStream(stream).await()
        }
        ref.downloadUrl.await().toString()
    }
}
