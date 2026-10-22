package com.larkliquid.player.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.catalog.components.LiquidBottomTab
import com.kyant.backdrop.catalog.components.LiquidBottomTabs
import com.kyant.backdrop.catalog.components.LocalLiquidBottomTabMagnified
import com.kyant.shapes.Capsule

@Immutable
data class KernelSuBottomTabItem(
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

/**
 * 2026 KernelSU-Style Watery Liquid Glass Bottom Navigation Bar (`KernelSuLiquidBottomBar.kt`).
 */
@Composable
fun KernelSuLiquidBottomBar(
    tabs: List<KernelSuBottomTabItem>,
    selectedTabIndex: () -> Int,
    onTabSelected: (Int) -> Unit,
    backdrop: Backdrop,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val activeIdx = selectedTabIndex()
    val kernelSuBlue = if (isDarkTheme) Color(0xFF0A84FF) else Color(0xFF007AFF)
    val baseIconTextColor = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)

    LiquidBottomTabs(
        selectedTabIndex = selectedTabIndex,
        onTabSelected = onTabSelected,
        backdrop = backdrop,
        tabsCount = tabs.size,
        isDarkThemeOverride = isDarkTheme,
        modifier = modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { index, item ->
            val isMagnifiedPass = LocalLiquidBottomTabMagnified.current
            val isSelected = activeIdx == index

            val iconBounceScale by animateFloatAsState(
                targetValue = when {
                    isMagnifiedPass -> 1.16f
                    isSelected -> 1.05f
                    else -> 1.0f
                },
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
                label = "KernelSuIconBounce"
            )

            val tintColor = when {
                isMagnifiedPass -> kernelSuBlue
                isSelected -> kernelSuBlue
                else -> baseIconTextColor.copy(alpha = 0.88f)
            }

            LiquidBottomTab(
                onClick = { onTabSelected(index) }
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = tintColor,
                        modifier = Modifier
                            .size(23.dp)
                            .graphicsLayer {
                                scaleX = iconBounceScale
                                scaleY = iconBounceScale
                            }
                    )

                    if (item.badgeCount > 0) {
                        Box(
                            modifier = Modifier
                                .offset(x = 10.dp, y = (-5).dp)
                                .clip(Capsule())
                                .background(kernelSuBlue)
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = item.title,
                        color = tintColor,
                        fontSize = if (isMagnifiedPass) 12.sp else 11.sp,
                        fontWeight = if (isSelected || isMagnifiedPass) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
