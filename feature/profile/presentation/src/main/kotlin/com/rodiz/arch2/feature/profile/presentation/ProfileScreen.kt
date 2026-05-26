package com.rodiz.arch2.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource
import com.rodiz.arch2.feature.profile.domain.model.OwnerProfile
import com.rodiz.arch2.feature.settings.domain.model.AccountDeletion
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun ProfileRoute(
    onSignedOut: () -> Unit,
    onOpenMyPets: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onAddPet: () -> Unit,
    onOpenPet: (PetId) -> Unit,
    onOpenHelpSafety: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.pendingDeletion?.let { deletion ->
                CancelDeletionBanner(
                    deletion = deletion,
                    isCancelling = state.isCancellingDeletion,
                    onCancel = viewModel::cancelPendingDeletion,
                )
            }

            ProfileHeroCard(
                profile = state.profile,
                petCount = state.pets.size,
                isPetsLoading = state.isPetsLoading,
                isProfileLoading = state.isLoading,
                onEditProfile = onEditProfile,
            )

            MyPetsSection(
                pets = state.pets,
                onOpenMyPets = onOpenMyPets,
                onOpenPet = onOpenPet,
                onAddPet = onAddPet,
            )

            ManageSection(
                petCount = state.pets.size,
                isPetsLoading = state.isPetsLoading,
                onOpenMyPets = onOpenMyPets,
                onOpenSettings = onOpenSettings,
                onOpenHelp = onOpenHelpSafety,
            )

            SignOutPill(onClick = { viewModel.signOut(onSignedOut) })

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ----- Hero card ------------------------------------------------------------

@Composable
private fun ProfileHeroCard(
    profile: OwnerProfile?,
    petCount: Int,
    isPetsLoading: Boolean,
    isProfileLoading: Boolean,
    onEditProfile: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = BrandColors.Coral,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp)
            .clickable(onClick = onEditProfile)
            .testTag("profile_hero"),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val ringColor = Color.White.copy(alpha = 0.18f)
                    drawOval(
                        color = ringColor,
                        topLeft = Offset(size.width * 0.55f, size.height * 0.08f),
                        size = Size(size.width * 0.45f, size.height * 0.18f),
                    )
                    drawOval(
                        color = ringColor,
                        topLeft = Offset(size.width * 0.62f, size.height * 0.35f),
                        size = Size(size.width * 0.40f, size.height * 0.16f),
                    )
                },
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    HeroAvatar(profile = profile)
                    Spacer(Modifier.weight(1f))
                    EditBadge(onClick = onEditProfile)
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = profile?.firstName?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.profile_welcome_fallback),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                profile?.email?.takeIf { it.isNotBlank() }?.let { email ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(20.dp))

                val unavailable = stringResource(R.string.profile_stat_unavailable)
                val memberYear = remember(profile?.createdAt) {
                    profile?.createdAt?.toLocalDateTime(TimeZone.currentSystemDefault())
                        ?.year?.toString()
                        ?: unavailable
                }
                val petValue = when {
                    isPetsLoading -> unavailable
                    else -> petCount.toString()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatPill(
                        value = petValue,
                        label = stringResource(R.string.profile_stat_pets),
                        modifier = Modifier.weight(1f),
                    )
                    StatPill(
                        // Match count is deferred — there is no owner-scoped match
                        // count use case yet, and the architecture rules forbid
                        // depending on :feature:match:domain from here without the
                        // shared :core:* JVM pattern. Placeholder keeps the slot.
                        value = unavailable,
                        label = stringResource(R.string.profile_stat_matches),
                        modifier = Modifier.weight(1f),
                    )
                    StatPill(
                        value = if (isProfileLoading) unavailable else memberYear,
                        label = stringResource(R.string.profile_stat_member),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroAvatar(profile: OwnerProfile?) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BrandColors.CoralLight.copy(alpha = 0.6f))
            .border(3.dp, Color.White, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val url = profile?.avatarUrl
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(15.dp)),
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun EditBadge(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(36.dp).testTag("profile_edit_button"),
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.profile_edit_cd),
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun StatPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.18f),
        modifier = modifier.heightIn(min = 62.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ----- MY PETS section ------------------------------------------------------

@Composable
private fun MyPetsSection(
    pets: List<Pet>,
    onOpenMyPets: () -> Unit,
    onOpenPet: (PetId) -> Unit,
    onAddPet: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = stringResource(R.string.profile_section_my_pets),
            trailing = {
                TextButton(
                    onClick = onOpenMyPets,
                    modifier = Modifier.testTag("profile_see_all_pets"),
                ) {
                    Text(
                        text = stringResource(R.string.profile_see_all),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = BrandColors.CoralDeep,
                    )
                }
            },
        )
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
        ) {
            items(items = pets.take(8), key = { it.id.value }) { pet ->
                PetMiniCard(pet = pet, onClick = { onOpenPet(pet.id) })
            }
            item(key = "add-pet") {
                AddPetMiniCard(onClick = onAddPet)
            }
        }
    }
}

@Composable
private fun PetMiniCard(
    pet: Pet,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .height(80.dp)
            .width(150.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val url = pet.photos.firstOrNull()?.source?.let { src ->
                    (src as? PhotoSource.Remote)?.downloadUrl
                        ?: (src as? PhotoSource.Local)?.uri
                }
                if (url != null) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Pets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.profile_pet_age_short, pet.ageYears),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AddPetMiniCard(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BrandColors.Coral.copy(alpha = 0.12f),
        modifier = Modifier
            .height(80.dp)
            .width(80.dp)
            .clickable(onClick = onClick)
            .testTag("profile_add_pet"),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.profile_add_pet_cd),
                tint = BrandColors.CoralDeep,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

// ----- MANAGE section -------------------------------------------------------

@Composable
private fun ManageSection(
    petCount: Int,
    isPetsLoading: Boolean,
    onOpenMyPets: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
) {
    val petsSubtitle = when {
        isPetsLoading -> stringResource(R.string.profile_stat_unavailable)
        petCount == 0 -> stringResource(R.string.profile_manage_my_pets_subtitle_empty)
        petCount == 1 -> stringResource(R.string.profile_manage_my_pets_subtitle_one)
        else -> stringResource(R.string.profile_manage_my_pets_subtitle_plural, petCount)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = stringResource(R.string.profile_section_manage))
        ManageRow(
            icon = Icons.Outlined.Pets,
            title = stringResource(R.string.profile_section_my_pets),
            subtitle = petsSubtitle,
            onClick = onOpenMyPets,
            testTag = "profile_manage_my_pets",
        )
        ManageRow(
            icon = Icons.Outlined.Settings,
            title = stringResource(R.string.profile_manage_settings_title),
            subtitle = stringResource(R.string.profile_manage_settings_subtitle),
            onClick = onOpenSettings,
            testTag = "profile_manage_settings",
        )
        ManageRow(
            icon = Icons.Outlined.Shield,
            title = stringResource(R.string.profile_manage_help_title),
            subtitle = stringResource(R.string.profile_manage_help_subtitle),
            onClick = onOpenHelp,
            testTag = "profile_manage_help",
        )
    }
}

@Composable
private fun ManageRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandColors.Coral.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BrandColors.CoralDeep,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ----- Sign out pill --------------------------------------------------------

@Composable
private fun SignOutPill(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("profile_sign_out"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = BrandColors.CoralDeep,
            )
            Text(
                text = stringResource(R.string.profile_sign_out),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = BrandColors.CoralDeep,
            )
        }
    }
}

// ----- Section header -------------------------------------------------------

@Composable
private fun SectionHeader(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (trailing != null) trailing()
    }
}

// ----- Cancel deletion banner (unchanged) -----------------------------------

@Composable
private fun CancelDeletionBanner(
    deletion: AccountDeletion,
    isCancelling: Boolean,
    onCancel: () -> Unit,
) {
    val daysLeft = remember(deletion.hardDeleteAt) {
        val now = Clock.System.now()
        val seconds = (deletion.hardDeleteAt - now).inWholeSeconds
        max(0, (seconds / 86_400.0).roundToInt())
    }
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome back",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = "Your account is scheduled for deletion in $daysLeft day${if (daysLeft == 1) "" else "s"}.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.size(8.dp))
            TextButton(onClick = onCancel, enabled = !isCancelling) {
                Text(if (isCancelling) "…" else "Cancel deletion")
            }
        }
    }
}

// ----- Previews -------------------------------------------------------------

@Preview(name = "Profile — populated", showBackground = true, heightDp = 1100)
@Composable
private fun ProfileScreenPreviewPopulated() {
    TinPetTheme {
        val now = Clock.System.now()
        val owner = OwnerProfile(
            id = "owner-1",
            firstName = "Maya R.",
            avatarUrl = null,
            email = "maya@tinpet.com",
            paused = false,
            location = null,
            createdAt = now,
            updatedAt = now,
        )
        ProfileScreenScaffoldPreview(
            profile = owner,
            pets = emptyList(),
        )
    }
}

@Preview(name = "Profile — no pets", showBackground = true, heightDp = 1100)
@Composable
private fun ProfileScreenPreviewEmpty() {
    TinPetTheme {
        val now = Clock.System.now()
        val owner = OwnerProfile(
            id = "owner-1",
            firstName = "Maya",
            avatarUrl = null,
            email = "maya@tinpet.com",
            paused = false,
            location = null,
            createdAt = now,
            updatedAt = now,
        )
        ProfileScreenScaffoldPreview(
            profile = owner,
            pets = emptyList(),
        )
    }
}

@Composable
private fun ProfileScreenScaffoldPreview(
    profile: OwnerProfile,
    pets: List<Pet>,
) {
    // A minimal stand-in for ProfileRoute that lets previews render without Hilt.
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileHeroCard(
                profile = profile,
                petCount = pets.size,
                isPetsLoading = false,
                isProfileLoading = false,
                onEditProfile = {},
            )
            MyPetsSection(
                pets = pets,
                onOpenMyPets = {},
                onOpenPet = {},
                onAddPet = {},
            )
            ManageSection(
                petCount = pets.size,
                isPetsLoading = false,
                onOpenMyPets = {},
                onOpenSettings = {},
                onOpenHelp = {},
            )
            SignOutPill(onClick = {})
        }
    }
}
