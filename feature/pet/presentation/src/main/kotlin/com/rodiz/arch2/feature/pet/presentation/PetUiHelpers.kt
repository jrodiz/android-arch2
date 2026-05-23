package com.rodiz.arch2.feature.pet.presentation

import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.PetEnergy
import com.rodiz.arch2.feature.pet.domain.model.PetSize
import com.rodiz.arch2.feature.pet.domain.model.PetValidationError
import com.rodiz.arch2.feature.pet.domain.model.Species

internal fun Species.label(): String = when (this) {
    Species.DOG -> "Dog"
    Species.CAT -> "Cat"
    Species.RABBIT -> "Rabbit"
    Species.HAMSTER -> "Hamster"
    Species.GUINEA_PIG -> "Guinea pig"
    Species.FERRET -> "Ferret"
    Species.OTHER_SMALL_MAMMAL -> "Other small mammal"
}

internal fun Species.emoji(): String = when (this) {
    Species.DOG -> "🐕"
    Species.CAT -> "🐈"
    Species.RABBIT -> "🐇"
    Species.HAMSTER -> "🐹"
    Species.GUINEA_PIG -> "🐹"
    Species.FERRET -> "🦝"
    Species.OTHER_SMALL_MAMMAL -> "🐾"
}

internal fun Intent.label(): String = when (this) {
    Intent.PLAYDATE -> "Playdate"
    Intent.ADOPTION -> "Adoption"
    Intent.FRIENDSHIP -> "Friendship"
}

internal fun PetSize.label(): String = when (this) {
    PetSize.SMALL -> "Small"
    PetSize.MEDIUM -> "Medium"
    PetSize.LARGE -> "Large"
}

internal fun PetEnergy.label(): String = when (this) {
    PetEnergy.CALM -> "Calm"
    PetEnergy.MEDIUM -> "Medium"
    PetEnergy.HIGH -> "High"
}

internal fun PetValidationError.message(): String = when (this) {
    PetValidationError.NameMissing -> "Add a name"
    PetValidationError.NameTooLong -> "Name is too long (max 30)"
    PetValidationError.AgeOutOfRange -> "Age must be between 0 and 25"
    PetValidationError.SpeciesMissing -> "Pick a species"
    PetValidationError.NoPhotos -> "Add at least one photo"
    PetValidationError.TooManyPhotos -> "Up to 6 photos"
    PetValidationError.NoIntent -> "Pick at least one intent"
    PetValidationError.BioTooLong -> "Bio is too long (max 300)"
}
