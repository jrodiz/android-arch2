package com.rodiz.arch2.feature.pet.domain.model

enum class SpeciesCategory { DOGS, CATS, SMALL_MAMMALS }

enum class Species(val category: SpeciesCategory) {
    DOG(SpeciesCategory.DOGS),
    CAT(SpeciesCategory.CATS),
    RABBIT(SpeciesCategory.SMALL_MAMMALS),
    HAMSTER(SpeciesCategory.SMALL_MAMMALS),
    GUINEA_PIG(SpeciesCategory.SMALL_MAMMALS),
    FERRET(SpeciesCategory.SMALL_MAMMALS),
    OTHER_SMALL_MAMMAL(SpeciesCategory.SMALL_MAMMALS),
}
