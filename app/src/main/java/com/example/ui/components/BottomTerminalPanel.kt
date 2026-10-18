package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TerminalEntry
import kotlinx.coroutines.launch

@Composable
fun BottomTerminalPanel(
    isOpen: Boolean,
    terminalEntries: List<TerminalEntry>,
    onClose: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val quickCommands = listOf("help", "status", "apps", "clear")

    // Height fraction: 0.0f (hidden) to 1.0f (100% full screen)
    val heightFractionAnim = remember { Animatable(0f) }

    // When isOpen turns true, animate in to a comfortable default height (~0.62f)
    LaunchedEffect(isOpen) {
        if (isOpen && heightFractionAnim.value < 0.2f) {
            heightFractionAnim.animateTo(
                targetValue = 0.62f,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    // Auto-scroll on new entries
    LaunchedEffect(terminalEntries.size) {
        if (terminalEntries.isNotEmpty()) {
            listState.animateScrollToItem(terminalEntries.size - 1)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = (heightFractionAnim.value * 0.6f).coerceIn(0.2f, 0.7f)))
    ) {
        val totalScreenHeightPx = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        val isFullScreen = heightFractionAnim.value >= 0.92f
        val cornerRadius = if (isFullScreen) 0.dp else 24.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(heightFractionAnim.value.coerceIn(0.15f, 1f))
                .align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)),
                color = Color(0xFF0C0E12),
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (isFullScreen) Modifier.statusBarsPadding() else Modifier)
                        .navigationBarsPadding()
                ) {
                    // Draggable Top Section (Handle + Header)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(totalScreenHeightPx) {
                                detectVerticalDragGestures(
                                    onDragStart = { },
                                    onVerticalDrag = { _, dragAmount ->
                                        // Continuous real-time stretch tracking finger position
                                        val deltaFraction = -dragAmount / totalScreenHeightPx
                                        val nextFraction = (heightFractionAnim.value + deltaFraction).coerceIn(0.1f, 1.0f)
                                        coroutineScope.launch {
                                            heightFractionAnim.snapTo(nextFraction)
                                        }
                                    },
                                    onDragEnd = {
                                        // Natural spring snap physics:
                                        coroutineScope.launch {
                                            val current = heightFractionAnim.value
                                            val target = when {
                                                current >= 0.80f -> 1.0f // Snap to full screen
                                                current <= 0.28f -> 0.0f // Fold and close
                                                else -> 0.62f // Rest at standard height
                                            }
                                            if (target == 0.0f) {
                                                heightFractionAnim.animateTo(
                                                    targetValue = 0.0f,
                                                    animationSpec = spring(dampingRatio = 0.85f)
                                                )
                                                onClose()
                                            } else {
                                                heightFractionAnim.animateTo(
                                                    targetValue = target,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.85f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                    ) {
                        // Visual Drag Handle
                        if (!isFullScreen) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 46.dp, height = 5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF3E4554))
                                )
                            }
                        }

                        // Top Bar Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = if (isFullScreen) 10.dp else 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E232E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB77D),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Terminal",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (isFullScreen) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4CAF50))
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isFullScreen) "Plein écran" else "Glisser pour étirer",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = Color(0xFF8B949E)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Full Screen Toggle / Stretch button
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val target = if (isFullScreen) 0.62f else 1.0f
                                            heightFractionAnim.animateTo(
                                                targetValue = target,
                                                animationSpec = spring(
                                                    dampingRatio = 0.85f,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E232E))
                                        .testTag("terminal_toggle_stretch_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = if (isFullScreen) "Réduire" else "Compléter l'écran",
                                        tint = Color(0xFFFFB77D),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Clear Output Button
                                IconButton(
                                    onClick = { onExecuteCommand("clear") },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E232E))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Effacer l'écran",
                                        tint = Color(0xFF8B949E),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Prominent Close Button (Requested: once full screen, close button in header closes it completely)
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            heightFractionAnim.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.85f,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                            onClose()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFullScreen) MaterialTheme.colorScheme.primary else Color(0xFF242A36)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = if (isFullScreen) 14.dp else 10.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("terminal_header_close_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isFullScreen) MaterialTheme.colorScheme.onPrimary else Color.White
                                    )
                                    if (isFullScreen) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Fermer",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Quick Command Pills
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(quickCommands) { cmd ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onExecuteCommand(cmd) },
                                color = Color(0xFF191E28),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = cmd,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF79B8FF),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Terminal Logs Output Area
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        items(terminalEntries) { entry ->
                            TerminalLogRow(entry)
                        }
                    }

                    // Command Input Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF14171E))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "hephaist@os:~$",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFFFB77D),
                            modifier = Modifier.padding(end = 6.dp)
                        )

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stretch_terminal_input"),
                            placeholder = {
                                Text(
                                    "Commande...",
                                    color = Color(0xFF6E7681),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                fontSize = 12.sp
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (inputText.isNotBlank()) {
                                        onExecuteCommand(inputText)
                                        inputText = ""
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFB77D),
                                unfocusedBorderColor = Color(0xFF2E3440),
                                cursorColor = Color(0xFFFFB77D)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    onExecuteCommand(inputText)
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFB77D))
                                .testTag("stretch_terminal_send")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Envoyer",
                                tint = Color(0xFF1E2128),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalLogRow(entry: TerminalEntry) {
    val textColor = when {
        entry.isCommand -> Color(0xFFFFB77D)
        entry.isError -> Color(0xFFFF7B72)
        entry.isSuccess -> Color(0xFF7EE787)
        else -> Color(0xFFC9D1D9)
    }

    Text(
        text = entry.text,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.5.sp,
        fontWeight = if (entry.isCommand) FontWeight.Bold else FontWeight.Normal,
        color = textColor,
        lineHeight = 16.sp,
        modifier = Modifier.padding(vertical = 1.5.dp)
    )
}
