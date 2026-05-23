package com.rodiz.arch2.feature.pet.domain.model

data class PetDraft(
    val name: String,
    val ageYears: Int,
    val ageIsApproximate: Boolean,
    val species: Species?,
    val intents: Set<Intent>,
    val photos: List<PetPhoto>,
    val bio: String?,
    /** Optional size band collected on Add Pet Step 2. Null until the owner picks one. */
    val size: PetSize? = null,
    /** Optional energy level collected on Add Pet Step 2. Null until the owner picks one. */
    val energy: PetEnergy? = null,
) {
    companion object {
        val EMPTY = PetDraft(
            name = "",
            ageYears = 0,
            ageIsApproximate = false,
            species = null,
            intents = emptySet(),
            photos = emptyList(),
            bio = null,
            size = null,
            energy = null,
        )

        const val NAME_MAX_LEN = 30
        const val AGE_MIN = 0
        const val AGE_MAX = 25
        const val PHOTOS_MIN = 1
        const val PHOTOS_MAX = 6
        const val BIO_MAX_LEN = 300
    }
}

sealed interface PetValidationError {
    data object NameMissing : PetValidationError
    data object NameTooLong : PetValidationError
    data object AgeOutOfRange : PetValidationError
    data object SpeciesMissing : PetValidationError
    data object NoPhotos : PetValidationError
    data object TooManyPhotos : PetValidationError
    data object NoIntent : PetValidationError
    data object BioTooLong : PetValidationError
}

sealed interface PetDraftValidation {
    data class Valid(val draft: PetDraft) : PetDraftValidation
    data class Invalid(val errors: List<PetValidationError>) : PetDraftValidation
}

fun PetDraft.validate(): PetDraftValidation {
    val errors = mutableListOf<PetValidationError>()
    val trimmedName = name.trim()
    when {
        trimmedName.isEmpty() -> errors += PetValidationError.NameMissing
        trimmedName.length > PetDraft.NAME_MAX_LEN -> errors += PetValidationError.NameTooLong
    }
    if (ageYears !in PetDraft.AGE_MIN..PetDraft.AGE_MAX) errors += PetValidationError.AgeOutOfRange
    if (species == null) errors += PetValidationError.SpeciesMissing
    if (intents.isEmpty()) errors += PetValidationError.NoIntent
    if (photos.isEmpty()) errors += PetValidationError.NoPhotos
    if (photos.size > PetDraft.PHOTOS_MAX) errors += PetValidationError.TooManyPhotos
    val trimmedBio = bio?.trim()
    if (trimmedBio != null && trimmedBio.length > PetDraft.BIO_MAX_LEN) {
        errors += PetValidationError.BioTooLong
    }

    return if (errors.isEmpty()) {
        PetDraftValidation.Valid(
            copy(
                name = trimmedName,
                bio = trimmedBio?.takeIf { it.isNotEmpty() },
            ),
        )
    } else {
        PetDraftValidation.Invalid(errors)
    }
}
