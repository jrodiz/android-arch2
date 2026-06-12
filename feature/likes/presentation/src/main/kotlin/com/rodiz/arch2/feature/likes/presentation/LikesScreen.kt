package com.rodiz.arch2.feature.likes.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rodiz.arch2.core.designsystem.component.EmptyTabState
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.feature.deck.domain.model.DistanceBucket
import com.rodiz.arch2.feature.likes.domain.model.IncomingLike
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource
import com.rodiz.arch2.feature.pet.domain.model.Intent as PetIntent

@Composable
internal fun LikesYouRoute(
    onGoToDeck: () -> Unit,
    onOpenPetDetail: (PetId) -> Unit,
    viewModel: LikesYouViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.matchMessage) {
        state.matchMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMatch()
        }
    }

    LikesScreen(
        state = state,
        onFilterSelected = viewModel::onFilterSelected,
        onCardTap = { like ->
            viewModel.markSeen(like.key)
            onOpenPetDetail(like.anchorPet.id)
        },
        onDecline = { like -> viewModel.pass(like.key) },
        onGoToDeck = onGoToDeck,
        snackbarHostState = snackbar,
    )
}

@Composable
private fun LikesScreen(
    state: LikesYouUiState,
    onFilterSelected: (LikesFilter) -> Unit,
    onCardTap: (IncomingLike) -> Unit,
    onDecline: (IncomingLike) -> Unit,
    onGoToDeck: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val unseenCount = remember(state.likes, state.seenKeys) {
        state.likes.count { it.key.value !in state.seenKeys }
    }
    val filteredLikes = remember(state.likes, state.activeFilter) {
        when (val f = state.activeFilter) {
            LikesFilter.All -> state.likes
            is LikesFilter.ByIntent -> state.likes.filter { f.intent in it.anchorPet.intents }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LikesHeader(unseenCount = unseenCount)
            Spacer(Modifier.height(14.dp))
            FilterChipRow(
                activeFilter = state.activeFilter,
                onFilterSelected = onFilterSelected,
            )
            Spacer(Modifier.height(16.dp))

            when {
                state.isLoading && state.likes.isEmpty() -> {
                    LoadingBody()
                }
                state.likes.isEmpty() -> {
                    EmptyTabState(
                        icon = Icons.Outlined.Favorite,
                        headline = stringResource(R.string.likes_empty_headline),
                        body = stringResource(R.string.likes_empty_body),
                        cta = stringResource(R.string.likes_empty_cta),
                        onCta = onGoToDeck,
                    )
                }
                filteredLikes.isEmpty() -> {
                    FilteredEmptyBody(filter = state.activeFilter)
                }
                else -> {
                    LikesGrid(
                        likes = filteredLikes,
                        seenKeys = state.seenKeys,
                        onCardTap = onCardTap,
                        onDecline = onDecline,
                    )
                }
            }
        }
    }
}

@Composable
private fun LikesHeader(unseenCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.likes_eyebrow),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.6.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.likes_headline),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (unseenCount > 0) {
                Spacer(Modifier.width(12.dp))
                CountPill(count = unseenCount)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.likes_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CountPill(count: Int) {
    Surface(
        shape = CircleShape,
        color = BrandColors.Coral,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = stringResource(R.string.likes_count_format, count),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun FilterChipRow(
    activeFilter: LikesFilter,
    onFilterSelected: (LikesFilter) -> Unit,
) {
    val all = stringResource(R.string.likes_filter_all)
    val playdate = stringResource(R.string.likes_filter_playdate)
    val adoption = stringResource(R.string.likes_filter_adoption)
    val friendship = stringResource(R.string.likes_filter_friendship)

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            FilterPill(
                label = all,
                selected = activeFilter == LikesFilter.All,
                onClick = { onFilterSelected(LikesFilter.All) },
            )
        }
        item {
            FilterPill(
                label = playdate,
                selected = activeFilter is LikesFilter.ByIntent && activeFilter.intent == PetIntent.PLAYDATE,
                onClick = { onFilterSelected(LikesFilter.ByIntent(PetIntent.PLAYDATE)) },
            )
        }
        item {
            FilterPill(
                label = adoption,
                selected = activeFilter is LikesFilter.ByIntent && activeFilter.intent == PetIntent.ADOPTION,
                onClick = { onFilterSelected(LikesFilter.ByIntent(PetIntent.ADOPTION)) },
            )
        }
        item {
            FilterPill(
                label = friendship,
                selected = activeFilter is LikesFilter.ByIntent && activeFilter.intent == PetIntent.FRIENDSHIP,
                onClick = { onFilterSelected(LikesFilter.ByIntent(PetIntent.FRIENDSHIP)) },
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) BrandColors.NavSurface else Color.Transparent
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    val border = if (selected) {
        null
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
    }
    Surface(
        shape = CircleShape,
        color = bg,
        border = border,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = fg,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun LikesGrid(
    likes: List<IncomingLike>,
    seenKeys: Set<String>,
    onCardTap: (IncomingLike) -> Unit,
    onDecline: (IncomingLike) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(likes, key = { it.key.value }) { like ->
            LikeCard(
                like = like,
                isUnseen = like.key.value !in seenKeys,
                onClick = { onCardTap(like) },
                onDecline = { onDecline(like) },
            )
        }
    }
}

@Composable
private fun LikeCard(
    like: IncomingLike,
    isUnseen: Boolean,
    onClick: () -> Unit,
    onDecline: () -> Unit,
) {
    val nowMs = remember { System.currentTimeMillis() }
    val likedAtMs = like.likedAt.toEpochMilliseconds()
    val timeLabel = relativeTimeLabel(nowMs = nowMs, thenMs = likedAtMs)
    val distanceBucket = like.distanceBucket
    val intent = remember(like.anchorPet.intents) { displayIntent(like.anchorPet.intents) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clickable(onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val photoUrl = like.anchorPet.photos.firstOrNull()?.let { p ->
                (p.source as? PhotoSource.Remote)?.downloadUrl
                    ?: (p.source as? PhotoSource.Local)?.uri
            }
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = like.anchorPet.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Pets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            // Bottom scrim so the white name/distance reads on any photo.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.6f),
                        ),
                    ),
            )

            // Top row: relative time (left) + NEW pill + decline ✕ (right).
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RelativeTimePill(label = timeLabel)
                Spacer(Modifier.weight(1f))
                if (isUnseen) {
                    NewPill()
                    Spacer(Modifier.width(6.dp))
                }
                DeclineButton(onClick = onDecline)
            }

            // Intent chip (bottom-left, just above the text overlay).
            if (intent != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 10.dp, bottom = 60.dp),
                ) {
                    IntentPill(intent = intent)
                }
            }

            // Bottom overlay: name + age, distance bucket.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.likes_card_age_format,
                        like.anchorPet.name,
                        like.anchorPet.ageYears,
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = distanceBucket?.let { distanceLabel(it) }
                            ?: stringResource(R.string.likes_distance_unknown),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RelativeTimePill(label: String) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.55f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun DeclineButton(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.45f),
        modifier = Modifier
            .size(28.dp)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.likes_decline_cd),
            tint = Color.White,
            modifier = Modifier.padding(6.dp),
        )
    }
}

@Composable
private fun NewPill() {
    Surface(
        shape = CircleShape,
        color = BrandColors.Coral,
    ) {
        Text(
            text = stringResource(R.string.likes_card_new),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun IntentPill(intent: PetIntent) {
    val (labelRes, color, icon) = when (intent) {
        PetIntent.PLAYDATE -> Triple(
            R.string.likes_intent_playdate,
            BrandColors.Coral,
            Icons.Outlined.Pets,
        )
        PetIntent.FRIENDSHIP -> Triple(
            R.string.likes_intent_friendship,
            BrandColors.MintLeaf,
            Icons.AutoMirrored.Outlined.Chat,
        )
        PetIntent.ADOPTION -> Triple(
            R.string.likes_intent_adoption,
            BrandColors.CoralDeep,
            Icons.Outlined.Favorite,
        )
    }
    Surface(
        shape = CircleShape,
        color = color,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp),
            )
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun LoadingBody() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = BrandColors.Coral)
    }
}

@Composable
private fun FilteredEmptyBody(filter: LikesFilter) {
    val label = when (filter) {
        LikesFilter.All -> stringResource(R.string.likes_filter_all)
        is LikesFilter.ByIntent -> when (filter.intent) {
            PetIntent.PLAYDATE -> stringResource(R.string.likes_filter_playdate)
            PetIntent.FRIENDSHIP -> stringResource(R.string.likes_filter_friendship)
            PetIntent.ADOPTION -> stringResource(R.string.likes_filter_adoption)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.likes_filtered_empty_format, label.lowercase()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------- helpers ----------

/** Choose a single intent chip per card in a stable order: Playdate > Friendship > Adoption. */
private fun displayIntent(intents: Set<PetIntent>): PetIntent? = when {
    PetIntent.PLAYDATE in intents -> PetIntent.PLAYDATE
    PetIntent.FRIENDSHIP in intents -> PetIntent.FRIENDSHIP
    PetIntent.ADOPTION in intents -> PetIntent.ADOPTION
    else -> null
}

@Composable
private fun distanceLabel(bucket: DistanceBucket): String = when (bucket) {
    DistanceBucket.UNDER_5_KM -> stringResource(R.string.likes_distance_under_5)
    DistanceBucket.BUCKET_5_15_KM -> stringResource(R.string.likes_distance_5_15)
    DistanceBucket.BUCKET_15_50_KM -> stringResource(R.string.likes_distance_15_50)
    DistanceBucket.OVER_50_KM -> stringResource(R.string.likes_distance_over_50)
}

@Composable
private fun relativeTimeLabel(nowMs: Long, thenMs: Long): String {
    val deltaSeconds = ((nowMs - thenMs) / 1_000L).coerceAtLeast(0)
    return when {
        deltaSeconds < 60 -> stringResource(R.string.likes_time_just_now)
        deltaSeconds < 3_600 -> stringResource(R.string.likes_time_minutes_format, (deltaSeconds / 60).toInt())
        deltaSeconds < 86_400 -> stringResource(R.string.likes_time_hours_format, (deltaSeconds / 3_600).toInt())
        deltaSeconds < 7 * 86_400 -> stringResource(R.string.likes_time_days_format, (deltaSeconds / 86_400).toInt())
        else -> stringResource(R.string.likes_time_weeks_format, (deltaSeconds / (7 * 86_400)).toInt())
    }
}
