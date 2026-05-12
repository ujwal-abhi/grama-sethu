package com.example.gramasethu.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gramasethu.data.BridgeRepository
import com.example.gramasethu.data.ReportRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.gramasethu.ui.components.BottomNavBar

data class ReportHistoryItem(
    val bridgeName: String = "",
    val status: String = "",
    val timestamp: Long = 0
)

@Composable
fun ProfileScreen(
    navController: androidx.navigation.NavController,
    onLogout: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    val email = user?.email ?: "Unknown"
    val initials = email.take(2).uppercase()

    var reportCount by remember { mutableStateOf(0) }
    var recentReports by remember { mutableStateOf(listOf<ReportHistoryItem>()) }
    var visible by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        // Load user's reports from Firestore
        FirebaseFirestore.getInstance()
            .collection("reports")
            .whereEqualTo("userId", user?.uid ?: "")
            .addSnapshotListener { snapshot, _ ->
                val reports = snapshot?.documents?.map { doc ->
                    ReportHistoryItem(
                        bridgeName = doc.getString("bridgeName") ?: "",
                        status = doc.getString("status") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0
                    )
                }?.sortedByDescending { it.timestamp } ?: emptyList()
                reportCount = reports.size
                recentReports = reports.take(5)
            }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        auth.signOut()
                        onLogout()
                    }
                ) {
                    Text("Logout", color = Color(0xFFA32D2D))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = Color(0xFF0F6E56))
                }
            }
        )
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
            // ── Scrollable content ────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Profile header ────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F6E56))
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = email,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Grama-Sethu member",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 12.sp
                        )
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {

                    // ── Stats cards ───────────────────────────────
                    Text(
                        text = "MY ACTIVITY",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            number = reportCount.toString(),
                            label = "Reports submitted"
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            number = recentReports.map { it.bridgeName }.distinct().size.toString(),
                            label = "Bridges monitored"
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Recent reports ────────────────────────────
                    Text(
                        text = "RECENT REPORTS",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (recentReports.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8F8F8), RoundedCornerShape(10.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No reports yet — go report a bridge!",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(10.dp)),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            recentReports.forEachIndexed { index, report ->
                                ReportHistoryCard(
                                    report = report,
                                    showDivider = index < recentReports.size - 1
                                )
                            }
                        }
                    }



                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Logout button ─────────────────────────────
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFCEBEB)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Logout",
                            color = Color(0xFFA32D2D),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            BottomNavBar(currentScreen = "profile", navController = navController)
        }
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, number: String, label: String) {
    Column(
        modifier = modifier
            .background(Color(0xFFE1F5EE), RoundedCornerShape(10.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F6E56)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF085041)
        )
    }
}

@Composable
fun ReportHistoryCard(report: ReportHistoryItem, showDivider: Boolean) {
    val statusColor = when (report.status) {
        "OPEN"    -> Color(0xFF1D9E75)
        "WARNING" -> Color(0xFFBA7517)
        else      -> Color(0xFFA32D2D)
    }
    val statusBg = when (report.status) {
        "OPEN"    -> Color(0xFFE1F5EE)
        "WARNING" -> Color(0xFFFAEEDA)
        else      -> Color(0xFFFCEBEB)
    }

    val timeAgo = remember(report.timestamp) {
        val diff = System.currentTimeMillis() - report.timestamp
        when {
            diff < 3600000  -> "${diff / 60000} mins ago"
            diff < 86400000 -> "${diff / 3600000} hours ago"
            else            -> "${diff / 86400000} days ago"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8), RoundedCornerShape(10.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = report.bridgeName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
                Text(
                    text = timeAgo,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Box(
                modifier = Modifier
                    .background(statusBg, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = report.status,
                    fontSize = 11.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        if (showDivider) {
            Divider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Text(text = value, fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
    }
}