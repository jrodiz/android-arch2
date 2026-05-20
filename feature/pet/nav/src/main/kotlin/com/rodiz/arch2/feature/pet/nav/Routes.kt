package com.rodiz.arch2.feature.pet.nav

import kotlinx.serialization.Serializable

@Serializable
data object MyPets

@Serializable
data object AddPet

@Serializable
data class PetDetail(val petId: String)

@Serializable
data class EditPet(val petId: String)
