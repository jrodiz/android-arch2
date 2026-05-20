package com.rodiz.arch2.feature.pet.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.feature.pet.domain.model.PetId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class EditPetFactoryHolder @Inject constructor(
    val factory: EditPetViewModel.Factory,
) : ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditPetRoute(
    petId: PetId,
    onDone: () -> Unit,
    holder: EditPetFactoryHolder = hiltViewModel(),
) {
    val viewModel = remember(petId) { holder.factory.create(petId.value) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.completed.collect { onDone() }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit pet") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            PetForm(
                state = state,
                onEvent = viewModel::onEvent,
                submitLabel = "Save changes",
                onSubmit = viewModel::submit,
                contentPadding = PaddingValues(16.dp),
            )
        }
    }
}
