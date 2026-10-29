package com.drumpads.app.ui.pads

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpads.app.domain.model.Pad
import com.drumpads.app.ui.settings.SettingsDialog
import com.drumpads.app.ui.theme.DarkBackground
import com.drumpads.app.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrumPadsScreen(
    viewModel: DrumPadsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "24-PAD BEAT MAKER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                        if (uiState.isStressTestActive) {
                            Text(
                                text = "STRESS MODE (${uiState.stressSecondsRemaining}s) - XRuns: ${uiState.xrunCount}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSettings(true) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Cài đặt",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.pads, key = { it.index }) { pad ->
                    val isPressed = uiState.activePads.contains(pad.index)
                    PadCell(
                        pad = pad,
                        isPressed = isPressed,
                        onTouchDown = { viewModel.onPadDown(pad.index) },
                        onTouchUp = { viewModel.onPadUp(pad.index) }
                    )
                }
            }
        }
    }

    if (uiState.isSettingsOpen) {
        SettingsDialog(
            uiState = uiState,
            onHapticChanged = { viewModel.setHapticEnabled(it) },
            onStressTestChanged = { viewModel.setStressTest(it) },
            onDismiss = { viewModel.toggleSettings(false) }
        )
    }
}

@Composable
fun PadCell(
    pad: Pad,
    isPressed: Boolean,
    onTouchDown: () -> Unit,
    onTouchUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseColor = remember(pad.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(pad.colorHex))
        } catch (_: Throwable) {
            Color(0xFFE53935)
        }
    }

    val animatedColor by animateColorAsState(
        targetValue = if (isPressed) Color.White else baseColor,
        label = "padColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        label = "padScale"
    )

    Box(
        modifier = modifier
            .aspectRatio(1.0f)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(animatedColor.copy(alpha = if (isPressed) 0.95f else 0.85f))
            .pointerInput(pad.index) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onTouchDown()
                    // Wait for release or cancellation
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    onTouchUp()
                }
            }
            .padding(8.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            Text(
                text = "${pad.index + 1}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPressed) Color.Black else Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = pad.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPressed) Color.Black else Color.White,
                maxLines = 1
            )
        }
    }
}
