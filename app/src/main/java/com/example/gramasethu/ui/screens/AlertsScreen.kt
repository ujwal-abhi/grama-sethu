package com.example.gramasethu.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gramasethu.data.AlertData
import com.example.gramasethu.data.AlertRepository
import com.example.gramasethu.ui.components.BottomNavBar

@Composable
fun AlertsScreen(navController: androidx.navigation.NavController) {
    var alerts by remember { mutableStateOf(listOf<AlertData>()) }
    var isLoading by remember { mutableStateOf(true) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        AlertRepository().getAlerts {
            alerts = it
            isLoading = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) + slideInVertically(
            initialOffsetY = { it / 10 },
            animationSpec = tween(400)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // ── Top bar ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F6E56))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Flood Alerts",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isLoading) "Loading..." else "${alerts.size} active alerts",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
            }

            // ── Content ───────────────────────────────────────────
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(12.dp)
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .background(Color(0xFFF0F0F0), RoundedCornerShape(10.dp))
                            )
                        }
                    }
                }
            } else if (alerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "✅", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No active alerts",
                            fontSize = 16.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "All routes are currently safe",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(alerts) { alert ->
                        AlertCard(alert = alert)
                    }
                }
            }

            BottomNavBar(currentScreen = "alerts", navController = navController)
        }
    }
}

@Composable
fun AlertCard(alert: AlertData) {
    val (bgColor, textColor, labelColor) = when (alert.type) {
        "DANGER"  -> Triple(Color(0xFFFCEBEB), Color(0xFF791F1F), Color(0xFFA32D2D))
        "WARNING" -> Triple(Color(0xFFFAEEDA), Color(0xFF633806), Color(0xFFBA7517))
        else      -> Triple(Color(0xFFE1F5EE), Color(0xFF085041), Color(0xFF0F6E56))
    }

    val typeIcon = when (alert.type) {
        "DANGER"  -> "🚨"
        "WARNING" -> "⚠️"
        else      -> "✅"
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(
            initialOffsetY = { it / 5 },
            animationSpec = tween(300)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor, RoundedCornerShape(10.dp))
                .padding(14.dp)
        ) {
            // ── Header row ────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = typeIcon, fontSize = 16.sp)
                Box(
                    modifier = Modifier
                        .background(labelColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = alert.type,
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Community badge for custom reports
                if (alert.isCustom) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF6B5ECD), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "👥 Community",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Severity badge
                    if (alert.severity.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    when (alert.severity) {
                                        "High"   -> Color(0xFFA32D2D)
                                        "Medium" -> Color(0xFFBA7517)
                                        else     -> Color(0xFF1D9E75)
                                    },
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = alert.severity,
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Title ─────────────────────────────────────────
            Text(
                text = alert.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Message ───────────────────────────────────────
            Text(
                text = alert.message,
                fontSize = 12.sp,
                color = textColor.copy(alpha = 0.8f)
            )
        }
    }
}