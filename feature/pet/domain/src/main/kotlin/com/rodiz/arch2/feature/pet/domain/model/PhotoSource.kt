package com.rodiz.arch2.feature.pet.domain.model

/**
 * A pet photo's storage location.
 *
 * [Local] is an unsaved URI from the gallery/camera picker, used inside the Add/Edit form
 * before submit. [Remote] is a Firebase Storage upload with a stable download URL.
 *
 * Both variants are pure strings so this type stays JVM-only.
 */
sealed interface PhotoSource {
    data class Local(val uri: String) : PhotoSource

    data class Remote(
        val storagePath: String,
        val downloadUrl: String,
    ) : PhotoSource
}
