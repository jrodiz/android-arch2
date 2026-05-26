package com.rodiz.arch2.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme
import com.rodiz.arch2.feature.deck.nav.DeckHome
import com.rodiz.arch2.feature.likes.nav.LikesHome
import com.rodiz.arch2.feature.match.nav.MatchesHome
import com.rodiz.arch2.feature.profile.nav.ProfileHome

data class NavBarItem(
    val route: Any,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val badgeCount: Int = 0,
)

@Composable
fun FloatingChipNavBar(
    items: List<NavBarItem>,
    selectedRoute: Any?,
    onSelected: (Any) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("floating_nav"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = BrandColors.NavSurface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val isSelected = selectedRoute == item.route
                NavItem(
                    item = item,
                    selected = isSelected,
                    onClick = { if (!isSelected) onSelected(item.route) },
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    item: NavBarItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    AnimatedContent(
        targetState = selected,
        transitionSpec = {
            (fadeIn(tween(180)) togetherWith fadeOut(tween(140)))
        },
        label = "nav_item_${item.label}",
    ) { isSelected ->
        Box(
            modifier = Modifier
                .clip(if (isSelected) RoundedCornerShape(percent = 50) else CircleShape)
                .clickable(role = Role.Tab, onClick = onClick),
        ) {
            if (isSelected) SelectedItemContent(item) else UnselectedItemContent(item)
        }
    }
}

@Composable
private fun SelectedItemContent(item: NavBarItem) {
    Box(
        modifier = Modifier
            .background(BrandColors.Coral, RoundedCornerShape(percent = 50))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = item.selectedIcon,
                contentDescription = item.label,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
            )
        }
        if (item.badgeCount > 0) {
            CountBadge(
                count = item.badgeCount,
                background = Color.White,
                textColor = BrandColors.CoralDeep,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp),
            )
        }
    }
}

@Composable
private fun UnselectedItemContent(item: NavBarItem) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(22.dp),
        )
        if (item.badgeCount > 0) {
            CountBadge(
                count = item.badgeCount,
                background = BrandColors.CoralDeep,
                textColor = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp),
            )
        }
    }
}

@Composable
private fun CountBadge(
    count: Int,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val label = if (count > 99) "99+" else count.toString()
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .background(background, CircleShape)
            .padding(horizontal = 5.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
        )
    }
}

// ---------- Previews ----------

private val SampleItems = listOf(
    NavBarItem(DeckHome, "Deck", Icons.Outlined.Pets, Icons.Filled.Pets),
    NavBarItem(LikesHome, "Likes", Icons.Outlined.Favorite, Icons.Filled.Favorite),
    NavBarItem(MatchesHome, "Matches", Icons.Outlined.Bolt, Icons.Filled.Bolt),
    NavBarItem(ProfileHome, "Profile", Icons.Outlined.Person, Icons.Filled.Person),
)

@Preview(name = "Empty badges", showBackground = true, backgroundColor = 0xFFFDF3EE)
@Composable
private fun FloatingChipNavBarPreviewEmpty() {
    TinPetTheme {
        FloatingChipNavBar(
            items = SampleItems,
            selectedRoute = DeckHome,
            onSelected = {},
        )
    }
}

@Preview(name = "Likes selected, badges 6 + 1", showBackground = true, backgroundColor = 0xFFFDF3EE)
@Composable
private fun FloatingChipNavBarPreviewLikesSelected() {
    TinPetTheme {
        FloatingChipNavBar(
            items = listOf(
                SampleItems[0],
                SampleItems[1].copy(badgeCount = 6),
                SampleItems[2].copy(badgeCount = 1),
                SampleItems[3],
            ),
            selectedRoute = LikesHome,
            onSelected = {},
        )
    }
}

@Preview(name = "Cap 99+ on selected", showBackground = true, backgroundColor = 0xFFFDF3EE)
@Composable
private fun FloatingChipNavBarPreviewCap() {
    TinPetTheme {
        FloatingChipNavBar(
            items = listOf(
                SampleItems[0],
                SampleItems[1].copy(badgeCount = 120),
                SampleItems[2].copy(badgeCount = 8),
                SampleItems[3],
            ),
            selectedRoute = LikesHome,
            onSelected = {},
        )
    }
}
