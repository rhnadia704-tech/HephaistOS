package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RailState
import com.example.model.Screen

@Composable
fun HephaistNavRail(
    currentScreen: Screen,
    railState: RailState,
    isPlusDrawerOpen: Boolean,
    onScreenSelected: (Screen) -> Unit,
    onTogglePlusDrawer: () -> Unit,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpanded = railState == RailState.EXPANDED
    val targetWidth = when (railState) {
        RailState.HIDDEN -> 0.dp
        RailState.COMPACT -> 72.dp
        RailState.EXPANDED -> 232.dp
    }

    val railWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "rail_width_anim"
    )

    if (railWidth <= 0.5.dp) return

    Surface(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .clipToBounds(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: App Icon / Logo & Expand Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onToggleExpand() }
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "H",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandHorizontally(
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                            expandFrom = Alignment.Start
                        ) + fadeIn(animationSpec = tween(180, delayMillis = 60)),
                        exit = shrinkHorizontally(
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                            shrinkTowards = Alignment.Start
                        ) + fadeOut(animationSpec = tween(100))
                    ) {
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = "HephaistOS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "v1.0 • Moteur UKA",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Navigation Items
                NavRailItem(
                    label = "Accueil",
                    selected = currentScreen == Screen.HOME && !isPlusDrawerOpen,
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    isExpanded = isExpanded,
                    testTag = "nav_home_button",
                    onClick = { onScreenSelected(Screen.HOME) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                NavRailItem(
                    label = "Unpack/Repack",
                    selected = currentScreen == Screen.UNPACK_REPACK && !isPlusDrawerOpen,
                    selectedIcon = Icons.Filled.Memory,
                    unselectedIcon = Icons.Outlined.Memory,
                    isExpanded = isExpanded,
                    testTag = "nav_unpack_repack_button",
                    onClick = { onScreenSelected(Screen.UNPACK_REPACK) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                NavRailItem(
                    label = "Extractor",
                    selected = currentScreen == Screen.EXTRACTOR && !isPlusDrawerOpen,
                    selectedIcon = Icons.Filled.Build,
                    unselectedIcon = Icons.Outlined.Build,
                    isExpanded = isExpanded,
                    testTag = "nav_extractor_button",
                    onClick = { onScreenSelected(Screen.EXTRACTOR) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                NavRailItem(
                    label = "Géméau",
                    selected = currentScreen == Screen.GEMEAU && !isPlusDrawerOpen,
                    selectedIcon = Icons.Filled.VpnKey,
                    unselectedIcon = Icons.Outlined.VpnKey,
                    isExpanded = isExpanded,
                    testTag = "nav_gemeau_button",
                    onClick = { onScreenSelected(Screen.GEMEAU) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                NavRailItem(
                    label = "Analyst",
                    selected = currentScreen == Screen.ANALYST && !isPlusDrawerOpen,
                    selectedIcon = Icons.Filled.Analytics,
                    unselectedIcon = Icons.Outlined.Analytics,
                    isExpanded = isExpanded,
                    testTag = "nav_analyst_button",
                    onClick = { onScreenSelected(Screen.ANALYST) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                NavRailItem(
                    label = "SCEAU",
                    selected = (currentScreen == Screen.SCEAU || currentScreen == Screen.VERISIN) && !isPlusDrawerOpen,
                    selectedIcon = Icons.Filled.Security,
                    unselectedIcon = Icons.Outlined.Security,
                    isExpanded = isExpanded,
                    testTag = "nav_sceau_button",
                    onClick = { onScreenSelected(Screen.SCEAU) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                NavRailItem(
                    label = "Paramètres",
                    selected = currentScreen == Screen.SETTINGS && !isPlusDrawerOpen,
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    isExpanded = isExpanded,
                    testTag = "nav_settings_button",
                    onClick = { onScreenSelected(Screen.SETTINGS) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Plus (+) Action Button
                PlusActionButton(
                    isOpen = isPlusDrawerOpen,
                    isExpanded = isExpanded,
                    onClick = onTogglePlusDrawer
                )
            }

            // Bottom spacer
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun NavRailItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isExpanded: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        label = "nav_item_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "nav_item_content"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag(testTag),
        color = bgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                    expandFrom = Alignment.Start
                ) + fadeIn(animationSpec = tween(180, delayMillis = 60)),
                exit = shrinkHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                    shrinkTowards = Alignment.Start
                ) + fadeOut(animationSpec = tween(100))
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(start = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun PlusActionButton(
    isOpen: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        label = "plus_btn_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isOpen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
        label = "plus_btn_content"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("nav_plus_button"),
        color = bgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isExpanded) Arrangement.Start else Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Ajouter un module",
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                    expandFrom = Alignment.Start
                ) + fadeIn(animationSpec = tween(180, delayMillis = 60)),
                exit = shrinkHorizontally(
                    animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                    shrinkTowards = Alignment.Start
                ) + fadeOut(animationSpec = tween(100))
            ) {
                Text(
                    text = "Ajouter (+)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(start = 14.dp)
                )
            }
        }
    }
}
