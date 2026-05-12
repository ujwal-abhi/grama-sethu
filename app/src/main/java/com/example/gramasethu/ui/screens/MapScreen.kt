package com.example.gramasethu.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gramasethu.data.BridgeRepository
import com.example.gramasethu.data.CustomReportMarker
import com.example.gramasethu.data.CustomReportRepository
import com.example.gramasethu.ui.components.BottomNavBar
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

data class BridgeUi(
    val id: String = "",
    val name: String = "",
    val status: String = "OPEN",
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    background(Color(0xFFE0E0E0).copy(alpha = alpha), RoundedCornerShape(8.dp))
}

@Composable
fun BridgeCardSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.width(140.dp).height(16.dp).shimmer())
        Box(modifier = Modifier.width(70.dp).height(16.dp).shimmer())
    }
}

@Composable
fun MapScreen(navController: androidx.navigation.NavController) {
    var bridges by remember { mutableStateOf(listOf<BridgeUi>()) }
    var customReports by remember { mutableStateOf(listOf<CustomReportMarker>()) }
    var isLoading by remember { mutableStateOf(true) }
    var visible by remember { mutableStateOf(false) }
    var selectedBridge by remember { mutableStateOf<BridgeUi?>(null) }

    val defaultLocation = LatLng(12.9716, 77.5946)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    LaunchedEffect(Unit) {
        visible = true
        BridgeRepository().getBridges {
            bridges = it
            isLoading = false
            if (it.isNotEmpty()) {
                val first = it.first()
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(first.lat, first.lng), 13f
                )
            }
        }
        CustomReportRepository().getCustomReports {
            customReports = it
            android.util.Log.d("MapScreen", "Custom reports loaded: ${it.size}")
            it.forEach { r ->
                android.util.Log.d("MapScreen", "Report at: ${r.lat}, ${r.lng}")
            }
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
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column {
                    Text(
                        text = "Bridge Map",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isLoading) "Loading bridges..." else "${bridges.size} bridges · ${customReports.size} community reports",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
            }

            // ── Google Map ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = false),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        compassEnabled = true
                    )
                ) {
                    // ── Bridge markers ────────────────────────────
                    bridges.forEach { bridge ->
                        val markerColor = when (bridge.status) {
                            "OPEN"    -> BitmapDescriptorFactory.HUE_GREEN
                            "WARNING" -> BitmapDescriptorFactory.HUE_YELLOW
                            else      -> BitmapDescriptorFactory.HUE_RED
                        }
                        Marker(
                            state = MarkerState(
                                position = LatLng(bridge.lat, bridge.lng)
                            ),
                            title = bridge.name,
                            snippet = "Status: ${bridge.status} — tap to report",
                            icon = BitmapDescriptorFactory.defaultMarker(markerColor),
                            onClick = {
                                selectedBridge = bridge
                                false
                            }
                        )
                    }

                    // ── Custom report markers ─────────────────────
                    customReports.forEach { report ->
                        val markerColor = when (report.type) {
                            "FLOOD"  -> BitmapDescriptorFactory.HUE_BLUE
                            "ROAD"   -> BitmapDescriptorFactory.HUE_ORANGE
                            "DANGER" -> BitmapDescriptorFactory.HUE_RED
                            else     -> BitmapDescriptorFactory.HUE_VIOLET
                        }
                        val icon = when (report.type) {
                            "FLOOD"  -> "🌊"
                            "ROAD"   -> "🚧"
                            "DANGER" -> "⚠️"
                            else     -> "🌉"
                        }
                        Marker(
                            state = MarkerState(
                                position = LatLng(report.lat, report.lng)
                            ),
                            title = "$icon ${report.type} — ${report.severity} severity",
                            snippet = "${report.description}\n📍 ${report.location}",
                            icon = BitmapDescriptorFactory.defaultMarker(markerColor)
                        )
                    }
                }

                // ── Legend overlay ────────────────────────────────
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Bridges",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    LegendItem(color = Color(0xFF1D9E75), label = "Open")
                    LegendItem(color = Color(0xFFEF9F27), label = "Warning")
                    LegendItem(color = Color(0xFFE24B4A), label = "Submerged")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Community",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    LegendItem(color = Color(0xFF4285F4), label = "Flood")
                    LegendItem(color = Color(0xFFFF6D00), label = "Road")
                    LegendItem(color = Color(0xFF9C27B0), label = "Bridge")
                }

                // ── Selected bridge popup ─────────────────────────
                selectedBridge?.let { bridge ->
                    val statusColor = when (bridge.status) {
                        "OPEN"    -> Color(0xFF1D9E75)
                        "WARNING" -> Color(0xFFEF9F27)
                        else      -> Color(0xFFE24B4A)
                    }
                    val statusBg = when (bridge.status) {
                        "OPEN"    -> Color(0xFFE1F5EE)
                        "WARNING" -> Color(0xFFFAEEDA)
                        else      -> Color(0xFFFCEBEB)
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bridge.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.DarkGray
                                )
                                Box(
                                    modifier = Modifier
                                        .background(statusBg, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = bridge.status,
                                        fontSize = 12.sp,
                                        color = statusColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📍 ${bridge.lat.toBigDecimal().setScale(4, java.math.RoundingMode.HALF_UP)}, ${bridge.lng.toBigDecimal().setScale(4, java.math.RoundingMode.HALF_UP)}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { selectedBridge = null },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = "✕ Dismiss", color = Color.Gray)
                                }
                                androidx.compose.material3.Button(
                                    onClick = {
                                        selectedBridge = null
                                        navController.navigate("report")
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0F6E56)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = "📋 Report", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // ── Bridge list ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Nearby bridges",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
                if (isLoading) {
                    repeat(3) { BridgeCardSkeleton() }
                } else {
                    bridges.forEach { bridge ->
                        BridgeCard(bridge = bridge)
                    }
                }
            }

            BottomNavBar(currentScreen = "map", navController = navController)
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(text = label, fontSize = 11.sp, color = Color.DarkGray)
    }
}

@Composable
fun BridgeCard(bridge: BridgeUi) {
    val statusColor = when (bridge.status) {
        "OPEN"    -> Color(0xFF1D9E75)
        "WARNING" -> Color(0xFFEF9F27)
        else      -> Color(0xFFE24B4A)
    }
    val statusBg = when (bridge.status) {
        "OPEN"    -> Color(0xFFE1F5EE)
        "WARNING" -> Color(0xFFFAEEDA)
        else      -> Color(0xFFFCEBEB)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = bridge.name,
            fontSize = 14.sp,
            color = Color.DarkGray,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .background(statusBg, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = bridge.status,
                fontSize = 11.sp,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}