package com.rodiz.arch2.feature.pet.presentation

import androidx.compose.runtime.Composable

/**
 * Entry composable for the Add a pet flow. Currently renders Step 1 of a 3-step
 * wizard (basics: photos, name, age, species, intents). Steps 2 & 3 plus persistence
 * land in a follow-up — see `plans/add-pet-step-1.md` §9 (Out of scope).
 */
@Composable
internal fun AddPetRoute(
    onDone: () -> Unit,
    onContinue: () -> Unit = onDone,
) {
    AddPetStep1Route(
        onBack = onDone,
        onContinue = onContinue,
    )
}
