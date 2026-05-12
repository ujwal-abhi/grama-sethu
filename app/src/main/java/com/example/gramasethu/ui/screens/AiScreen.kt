package com.example.gramasethu.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gramasethu.data.BridgeData
import com.example.gramasethu.data.BridgeRepository
import com.example.gramasethu.data.GeminiRepository
import com.example.gramasethu.ui.components.BottomNavBar
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun AiScreen(navController: androidx.navigation.NavController) {
    var messages by remember {
        mutableStateOf(
            listOf(ChatMessage("Hi! I am your flood safety assistant. Ask me about bridge conditions or safe routes.", false))
        )
    }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var bridges by remember { mutableStateOf(listOf<BridgeUi>()) }
    var visible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val geminiRepo = GeminiRepository()

    LaunchedEffect(Unit) {
        visible = true
        BridgeRepository().getBridges { bridges = it }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun sendMessage() {
        if (inputText.isNotBlank() && !isLoading) {
            val question = inputText.trim()
            inputText = ""
            messages = messages + ChatMessage(question, true)
            isLoading = true
            val bridgeData = bridges.map { BridgeData(name = it.name, status = it.status) }
            scope.launch {
                val reply = geminiRepo.askGemini(question, bridgeData)
                messages = messages + ChatMessage(reply, false)
                isLoading = false
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
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "AI Assistant",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Ask about road & bridge safety",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
            }

            // ── Chat messages ─────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
                if (isLoading) {
                    item {
                        Row {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AI is thinking",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF0F6E56)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Quick suggestions ─────────────────────────────────
            if (messages.size == 1) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Quick questions:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    listOf(
                        "Which bridges are safe right now?",
                        "Are there any submerged bridges?",
                        "What is the safest route today?"
                    ).forEach { suggestion ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE1F5EE), RoundedCornerShape(20.dp))
                                .clickable {
                                    inputText = suggestion
                                    sendMessage()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 12.sp,
                                color = Color(0xFF0F6E56)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ── Input bar ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask about road safety...", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0F6E56),
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { sendMessage() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F6E56)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    enabled = !isLoading && inputText.isNotBlank()
                ) {
                    Text(text = "Send", color = Color.White, fontSize = 13.sp)
                }
            }

            BottomNavBar(currentScreen = "ai", navController = navController)
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .background(
                    if (msg.isUser) Color(0xFF0F6E56) else Color(0xFFF0F0F0),
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (msg.isUser) 12.dp else 2.dp,
                        bottomEnd = if (msg.isUser) 2.dp else 12.dp
                    )
                )
                .padding(12.dp)
        ) {
            Text(
                text = msg.text,
                fontSize = 13.sp,
                color = if (msg.isUser) Color.White else Color.DarkGray
            )
        }
    }
}