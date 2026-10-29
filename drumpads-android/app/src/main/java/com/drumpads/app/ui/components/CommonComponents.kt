package com.drumpads.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drumpads.app.ui.pads.NavTab
import com.drumpads.app.ui.theme.*

@Composable
fun AppBottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(NavySurface.copy(alpha = 0.95f))
            .border(1.dp, NavyBorder, RoundedCornerShape(32.dp))
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                label = "Sounds",
                icon = Icons.Default.PlayArrow,
                isSelected = selectedTab == NavTab.SOUNDS,
                onClick = { onTabSelected(NavTab.SOUNDS) }
            )
            NavItem(
                label = "Feed",
                icon = Icons.Default.List,
                isSelected = selectedTab == NavTab.FEED,
                onClick = { onTabSelected(NavTab.FEED) }
            )
            NavItem(
                label = "My",
                icon = Icons.Default.Favorite,
                isSelected = selectedTab == NavTab.MY,
                onClick = { onTabSelected(NavTab.MY) }
            )
            NavItem(
                label = "Account",
                icon = Icons.Default.Person,
                isSelected = selectedTab == NavTab.ACCOUNT,
                onClick = { onTabSelected(NavTab.ACCOUNT) }
            )
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pillBackground = if (isSelected) NavyCardElevated else Color.Transparent

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(pillBackground)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) AccentYellow else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) AccentYellow else TextSecondary
        )
    }
}

@Composable
fun MiniPlayerBar(
    trackTitle: String,
    duration: String,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(NavySurface.copy(alpha = 0.9f))
            .border(1.dp, NavyBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                .clickable { onTogglePlay() },
            contentAlignment = Alignment.Center
        ) {
            if (isPlaying) {
                // Pause indicator: 2 vertical bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(width = 3.dp, height = 12.dp).background(Color.White))
                    Box(modifier = Modifier.size(width = 3.dp, height = 12.dp).background(Color.White))
                }
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = trackTitle,
            color = TextPrimary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = duration,
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}
