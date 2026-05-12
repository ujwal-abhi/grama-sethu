package com.example.gramasethu.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gramasethu.data.BridgeRepository
import com.example.gramasethu.data.ReportRepository
import com.example.gramasethu.ui.components.BottomNavBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

@Composable
fun ReportScreen(navController: androidx.navigation.NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F6E56))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Report",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Known Bridge", "Custom Report").forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selectedTab == index) Color(0xFF0F6E56) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            color = if (selectedTab == index) Color.White else Color.Gray,
                            fontWeight = if (selectedTab == index) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> KnownBridgeTab()
                    1 -> CustomReportTab()
                }
            }

            BottomNavBar(currentScreen = "report", navController = navController)
        }
    }
}

@Composable
fun KnownBridgeTab() {
    var bridges by remember { mutableStateOf(listOf<BridgeUi>()) }
    var selectedBridge by remember { mutableStateOf<BridgeUi?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var messageIsError by remember { mutableStateOf(false) }

    val reportRepo = ReportRepository()

    LaunchedEffect(Unit) {
        BridgeRepository().getBridges { bridges = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "1. Select a bridge",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (bridges.isEmpty()) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .height(44.dp)
                        .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                )
            }
        } else {
            bridges.forEach { bridge ->
                val isSelected = selectedBridge?.id == bridge.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            if (isSelected) Color(0xFFE1F5EE) else Color(0xFFF8F8F8),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedBridge = bridge }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = bridge.name,
                        fontSize = 14.sp,
                        color = if (isSelected) Color(0xFF0F6E56) else Color.DarkGray,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                    if (isSelected) {
                        Text(text = "✓", color = Color(0xFF0F6E56), fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "2. Report status",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = if (selectedBridge == null) Color.LightGray else Color.DarkGray
        )
        Spacer(modifier = Modifier.height(10.dp))

        listOf(
            Triple("OPEN", Color(0xFF1D9E75), Color(0xFFE1F5EE)),
            Triple("WARNING", Color(0xFFBA7517), Color(0xFFFAEEDA)),
            Triple("SUBMERGED", Color(0xFFA32D2D), Color(0xFFFCEBEB))
        ).forEach { (status, textColor, bgColor) ->
            Button(
                onClick = {
                    val bridge = selectedBridge ?: return@Button
                    isSubmitting = true
                    message = ""
                    reportRepo.submitReport(
                        bridgeId = bridge.id,
                        bridgeName = bridge.name,
                        status = status,
                        onSuccess = {
                            isSubmitting = false
                            message = "Report submitted! Thank you."
                            messageIsError = false
                            selectedBridge = null
                        },
                        onFailure = { err ->
                            isSubmitting = false
                            message = "Failed: $err"
                            messageIsError = true
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = bgColor),
                shape = RoundedCornerShape(8.dp),
                enabled = !isSubmitting && selectedBridge != null
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = textColor,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Report as $status",
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (message.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (messageIsError) Color(0xFFFCEBEB) else Color(0xFFE1F5EE),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message,
                    color = if (messageIsError) Color(0xFFA32D2D) else Color(0xFF0F6E56),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CustomReportTab() {
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }
    var pickedLat by remember { mutableStateOf(0.0) }
    var pickedLng by remember { mutableStateOf(0.0) }
    var isGettingLocation by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val locationPermission = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(12.9716, 77.5946), 10f)
    }

    if (showSuccessDialog) {
        Dialog(onDismissRequest = { showSuccessDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🙏", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Report Submitted!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F6E56)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Thank you for keeping your community safe. Your report has been recorded and will appear on the map.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showSuccessDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F6E56)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(text = "Done", color = Color.White)
                    }
                }
            }
        }
    }

    if (showMapPicker) {
        Dialog(onDismissRequest = { showMapPicker = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Tap on map to pin location",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap anywhere on the map to drop a pin",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        var markerPosition by remember {
                            mutableStateOf(
                                if (pickedLat != 0.0) LatLng(pickedLat, pickedLng) else null
                            )
                        }

                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            onMapClick = { latLng ->
                                markerPosition = latLng
                                pickedLat = latLng.latitude
                                pickedLng = latLng.longitude
                            }
                        ) {
                            markerPosition?.let { pos ->
                                Marker(
                                    state = MarkerState(position = pos),
                                    title = "Report location"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (pickedLat != 0.0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE1F5EE), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "📍 ${String.format(Locale.getDefault(), "%.4f", pickedLat)}, ${String.format(Locale.getDefault(), "%.4f", pickedLng)}",
                                fontSize = 12.sp,
                                color = Color(0xFF0F6E56),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { showMapPicker = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F6E56)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = pickedLat != 0.0
                    ) {
                        Text(
                            text = if (pickedLat != 0.0) "✓ Confirm Location" else "Tap map to select",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "1. What are you reporting?",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(10.dp))

        listOf(
            Pair("🌊 Flood", "FLOOD"),
            Pair("🌉 Unknown Bridge", "BRIDGE"),
            Pair("🚧 Road Blocked", "ROAD"),
            Pair("⚠️ Danger Zone", "DANGER")
        ).chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { (label, type) ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) Color(0xFF0F6E56) else Color(0xFFF8F8F8),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedType = type }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            color = if (isSelected) Color.White else Color.DarkGray,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "2. Severity",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("Low", Color(0xFF1D9E75), Color(0xFFE1F5EE)),
                Triple("Medium", Color(0xFFBA7517), Color(0xFFFAEEDA)),
                Triple("High", Color(0xFFA32D2D), Color(0xFFFCEBEB))
            ).forEach { (level, textColor, bgColor) ->
                val isSelected = selectedSeverity == level
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) textColor else bgColor,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedSeverity = level }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = level,
                        fontSize = 13.sp,
                        color = if (isSelected) Color.White else textColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "3. Location name",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Near Mysuru highway, village name...") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF0F6E56),
                unfocusedBorderColor = Color.LightGray
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "4. Pin on map",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (locationPermission.status.isGranted) {
                        isGettingLocation = true
                        scope.launch {
                            try {
                                val fusedLocationClient = LocationServices
                                    .getFusedLocationProviderClient(context)
                                val cancellationToken = CancellationTokenSource()
                                @SuppressLint("MissingPermission")
                                val loc = fusedLocationClient.getCurrentLocation(
                                    Priority.PRIORITY_HIGH_ACCURACY,
                                    cancellationToken.token
                                ).await()
                                if (loc != null) {
                                    pickedLat = loc.latitude
                                    pickedLng = loc.longitude
                                    cameraPositionState.position =
                                        CameraPosition.fromLatLngZoom(
                                            LatLng(loc.latitude, loc.longitude), 15f
                                        )
                                }
                                isGettingLocation = false
                            } catch (e: Exception) {
                                isGettingLocation = false
                                errorMessage = "Could not get location"
                            }
                        }
                    } else {
                        locationPermission.launchPermissionRequest()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE1F5EE)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isGettingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF0F6E56)
                    )
                } else {
                    Text(
                        text = "📍 My location",
                        fontSize = 13.sp,
                        color = Color(0xFF0F6E56)
                    )
                }
            }

            Button(
                onClick = { showMapPicker = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (pickedLat != 0.0) Color(0xFF0F6E56) else Color(0xFFF8F8F8)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (pickedLat != 0.0) "✓ Pinned" else "🗺️ Pick on map",
                    fontSize = 13.sp,
                    color = if (pickedLat != 0.0) Color.White else Color.Gray
                )
            }
        }

        if (pickedLat != 0.0) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE1F5EE), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "📍 ${String.format(Locale.getDefault(), "%.4f", pickedLat)}, ${String.format(Locale.getDefault(), "%.4f", pickedLng)}",
                    fontSize = 12.sp,
                    color = Color(0xFF0F6E56),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "5. Description",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text("Describe what you see — water level, road condition, danger...") },
            maxLines = 5,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF0F6E56),
                unfocusedBorderColor = Color.LightGray
            )
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFCEBEB), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(text = errorMessage, color = Color(0xFFA32D2D), fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                when {
                    selectedType.isEmpty() -> {
                        errorMessage = "Please select what you are reporting"
                        return@Button
                    }
                    selectedSeverity.isEmpty() -> {
                        errorMessage = "Please select severity level"
                        return@Button
                    }
                    location.isEmpty() -> {
                        errorMessage = "Please enter the location name"
                        return@Button
                    }
                    pickedLat == 0.0 -> {
                        errorMessage = "Please pin the location on the map"
                        return@Button
                    }
                    description.isEmpty() -> {
                        errorMessage = "Please add a description"
                        return@Button
                    }
                }
                isSubmitting = true
                errorMessage = ""

                val customReport = hashMapOf(
                    "type" to selectedType,
                    "severity" to selectedSeverity,
                    "location" to location,
                    "description" to description,
                    "lat" to pickedLat,
                    "lng" to pickedLng,
                    "userId" to (auth.currentUser?.uid ?: "anonymous"),
                    "userEmail" to (auth.currentUser?.email ?: "anonymous"),
                    "timestamp" to System.currentTimeMillis()
                )

                db.collection("custom_reports")
                    .add(customReport)
                    .addOnSuccessListener {
                        isSubmitting = false
                        showSuccessDialog = true
                        selectedType = ""
                        selectedSeverity = ""
                        location = ""
                        description = ""
                        pickedLat = 0.0
                        pickedLng = 0.0
                    }
                    .addOnFailureListener { e ->
                        isSubmitting = false
                        errorMessage = "Failed to submit: ${e.message}"
                    }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0F6E56)
            ),
            shape = RoundedCornerShape(10.dp),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Submit Report",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}