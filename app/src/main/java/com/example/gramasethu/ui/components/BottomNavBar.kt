package com.example.gramasethu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

data class NavItem(val label: String, val icon: String, val screen: String)

val navItems = listOf(
    NavItem("Map", "🗺️", "map"),
    NavItem("Report", "📋", "report"),
    NavItem("Alerts", "🚨", "alerts"),
    NavItem("AI", "🤖", "ai"),
    NavItem("Profile", "👤", "profile")
)

@Composable
fun BottomNavBar(currentScreen: String, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        navItems.forEach { item ->
            val isSelected = item.screen == currentScreen
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable {
                        if (!isSelected) {
                            navController.navigate(item.screen) {
                                popUpTo("map") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(text = item.icon, fontSize = 20.sp)
                Text(
                    text = item.label,
                    fontSize = 10.sp,
                    color = if (isSelected) Color(0xFF0F6E56) else Color.Gray,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}