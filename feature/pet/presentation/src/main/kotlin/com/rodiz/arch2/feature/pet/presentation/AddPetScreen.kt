package com.rodiz.arch2.feature.pet.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Entry composable for the Add a pet flow. Owns the shared [AddPetViewModel] and
 * switches between Step 1 / 2 / 3 based on the VM's `currentStep`. The system back
 * button steps backward inside the wizard until Step 1, at which point it exits
 * the flow via [onDone].
 */
@Composable
internal fun AddPetRoute(
    onDone: () -> Unit,
    onContinue: () -> Unit = onDone,
    viewModel: AddPetViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentStep = state.currentStep

    BackHandler(enabled = currentStep > 1) {
        viewModel.goToPreviousStep()
    }

    AnimatedContent(
        targetState = currentStep,
        label = "add_pet_wizard_step",
        transitionSpec = {
            val forward = targetState > initialState
            val direction = if (forward) {
                AnimatedContentTransitionScope.SlideDirection.Left
            } else {
                AnimatedContentTransitionScope.SlideDirection.Right
            }
            (slideIntoContainer(direction, tween(220)) + fadeIn(tween(220))) togetherWith
                (slideOutOfContainer(direction, tween(220)) + fadeOut(tween(220)))
        },
    ) { step ->
        when (step) {
            1 -> AddPetStep1Route(
                onBack = onDone,
                viewModel = viewModel,
            )
            2 -> AddPetStep2Route(
                onBack = viewModel::goToPreviousStep,
                viewModel = viewModel,
            )
            else -> AddPetStep3Route(
                onBack = viewModel::goToPreviousStep,
                onPublished = onContinue,
                viewModel = viewModel,
            )
        }
    }
}
