package com.rodiz.arch2.feature.pet.presentation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.nav.AddPet
import com.rodiz.arch2.feature.pet.nav.EditPet
import com.rodiz.arch2.feature.pet.nav.MyPets
import com.rodiz.arch2.feature.pet.nav.PetDetail
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object PetNavModule {
    @Provides
    @IntoSet
    fun providePetEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<MyPets> {
            MyPetsRoute(
                onAddPet = { navigator.goTo(AddPet) },
                onOpenPet = { navigator.goTo(PetDetail(it.value)) },
                // Pencil overlay skips the detail preview and jumps straight to Edit.
                onEditPet = { navigator.goTo(EditPet(it.value)) },
                onBack = { navigator.goBack() },
            )
        }
        entry<AddPet> {
            AddPetRoute(
                onDone = { navigator.goBack() },
                // TODO(addpet-step-2): wire to Step 2 destination once the wizard ships.
                onContinue = { navigator.goBack() },
            )
        }
        entry<PetDetail> { key ->
            PetPreviewRoute(
                petId = PetId(key.petId),
                onBack = { navigator.goBack() },
                onEdit = { navigator.goTo(EditPet(key.petId)) },
                onDeleted = { navigator.goBack() },
            )
        }
        entry<EditPet> { key ->
            EditPetRoute(
                petId = PetId(key.petId),
                onDone = { navigator.goBack() },
            )
        }
    }
}
