package com.rodiz.arch2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.likes.domain.usecase.ObserveLikesYouUseCase
import com.rodiz.arch2.feature.match.domain.usecase.ObserveInboxUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BottomNavBadges(val likes: Int = 0, val matches: Int = 0)

@HiltViewModel
class BottomNavViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    observeLikesYou: ObserveLikesYouUseCase,
    observeInbox: ObserveInboxUseCase,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val badges: StateFlow<BottomNavBadges> = sessionRepository.observe()
        .flatMapLatest { session ->
            val uid = session?.userId
            if (uid == null) {
                flowOf(BottomNavBadges())
            } else {
                combine(observeLikesYou(), observeInbox(uid)) { likes, inbox ->
                    BottomNavBadges(likes = likes.size, matches = inbox.newMatches.size)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), BottomNavBadges())
}
