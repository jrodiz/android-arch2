package com.rodiz.arch2.feature.login.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPet
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Surfaces the locally-cached featured pets for the Login hero. Kept separate
 * from [LoginViewModel] so its auth-flow state (and its 12 existing tests)
 * stay decoupled from the cross-cutting cache concern. The Login route hosts
 * both ViewModels and passes the list down to LoginScreen.
 */
@HiltViewModel
class LoginFeaturedPetsViewModel @Inject constructor(
    repo: FeaturedPetsRepository,
) : ViewModel() {

    val featured: StateFlow<List<FeaturedPet>> = repo.observe()
        .map { it.featured }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
