package com.rodiz.arch2.feature.pet.domain.model

/**
 * Owner-self-reported size band for a pet. Nullable on [Pet] — older docs and
 * pets created before the field existed simply don't render the chip.
 */
enum class PetSize { SMALL, MEDIUM, LARGE }
