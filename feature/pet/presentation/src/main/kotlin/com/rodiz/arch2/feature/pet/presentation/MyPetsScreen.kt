package com.rodiz.arch2.feature.pet.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MyPetsRoute(
    onAddPet: () -> Unit,
    onOpenPet: (PetId) -> Unit,
    onBack: () -> Unit,
    viewModel: MyPetsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Pets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddPet) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add a pet")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                state.activePets.isEmpty() && state.archivedPets.isEmpty() -> EmptyMyPets(onAddPet = onAddPet)
                else -> PetsGrid(
                    active = state.activePets,
                    archived = state.archivedPets,
                    onOpenPet = onOpenPet,
                    onRestore = viewModel::restore,
                )
            }
        }
    }
}

@Composable
private fun EmptyMyPets(onAddPet: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Pets,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No pets yet",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Add your first pet to start matching.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        FilledTonalButton(onClick = onAddPet) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Add a pet")
        }
    }
}

@Composable
private fun PetsGrid(
    active: List<Pet>,
    archived: List<Pet>,
    onOpenPet: (PetId) -> Unit,
    onRestore: (Pet) -> Unit,
) {
    var showArchived by remember { mutableStateOf(false) }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(active, key = { it.id.value }) { pet ->
            PetThumbnailCard(pet = pet, onClick = { onOpenPet(pet.id) })
        }
        if (archived.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(top = 8.dp).fillMaxWidth()) {
                    OutlinedButton(onClick = { showArchived = !showArchived }) {
                        Text(
                            if (showArchived) "Hide archived (${archived.size})"
                            else "Show archived (${archived.size})",
                        )
                    }
                }
            }
            if (showArchived) {
                items(archived, key = { "archived-${it.id.value}" }) { pet ->
                    Column {
                        PetThumbnailCard(pet = pet, onClick = { onOpenPet(pet.id) })
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { onRestore(pet) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Restore")
                        }
                    }
                }
            }
        }
    }
}
