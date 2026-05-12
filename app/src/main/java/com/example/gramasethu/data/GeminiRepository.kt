package com.example.gramasethu.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRepository {

    // ← paste your Groq API key here
    private val apiKey = "YOUR_API_KEY"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun askGemini(
        userQuestion: String,
        bridges: List<BridgeData>
    ): String = withContext(Dispatchers.IO) {

        android.util.Log.d("GROQ", "=== API CALL STARTED ===")
        android.util.Log.d("GROQ", "Question: $userQuestion")
        android.util.Log.d("GROQ", "Bridge count: ${bridges.size}")

        val bridgeInfo = if (bridges.isEmpty()) {
            "No bridge data available"
        } else {
            bridges.joinToString("\n") { "- ${it.name}: ${it.status}" }
        }

        val systemPrompt = """
            You are Grama-Sethu, an intelligent flood and road safety assistant for rural India.
            You help villagers and travelers stay safe during floods and monsoon seasons.
            
            You can answer questions about:
            - Current bridge safety based on the data provided
            - Safe routes and alternate paths
            - Flood safety tips and precautions
            - What to do during heavy rain or floods
            - How to prepare for monsoon season
            - Emergency actions during flooding
            - General road and travel safety in rural areas
            
            Always be helpful, concise, and use simple English.
            When bridge data shows submerged or warning status, always warn the user clearly.
            If asked something unrelated to safety or floods, politely redirect to your purpose.
        """.trimIndent()

        val userPrompt = """
            Current bridge statuses in the area:
            $bridgeInfo
            
            User question: $userQuestion
            
            Please give a helpful and relevant answer based on the bridge data and your knowledge of flood safety.
        """.trimIndent()

        val json = JSONObject().apply {
            put("model", "llama-3.3-70b-versatile")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
            put("max_tokens", 500)
            put("temperature", 0.7)
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        return@withContext try {
            android.util.Log.d("GROQ", "Sending request to Groq...")
            val response = client.newCall(request).execute()
            val responseCode = response.code
            val responseBody = response.body?.string() ?: ""

            android.util.Log.d("GROQ", "Response code: $responseCode")
            android.util.Log.d("GROQ", "Response body: $responseBody")

            if (!response.isSuccessful) {
                android.util.Log.e("GROQ", "Request failed with code: $responseCode")
                getDemoResponse(userQuestion, bridges)
            } else {
                val jsonResponse = JSONObject(responseBody)

                if (jsonResponse.has("error")) {
                    val errorMsg = jsonResponse
                        .getJSONObject("error")
                        .getString("message")
                    android.util.Log.e("GROQ", "API Error: $errorMsg")
                    getDemoResponse(userQuestion, bridges)
                } else {
                    val result = jsonResponse
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")

                    android.util.Log.d("GROQ", "Success! Response: $result")
                    result
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GROQ", "Exception: ${e.javaClass.simpleName}: ${e.message}")
            getDemoResponse(userQuestion, bridges)
        }
    }

    private fun getDemoResponse(
        question: String,
        bridges: List<BridgeData>
    ): String {
        val submerged = bridges.filter { it.status == "SUBMERGED" }
        val warning = bridges.filter { it.status == "WARNING" }
        val open = bridges.filter { it.status == "OPEN" }

        return buildString {
            appendLine("Based on current bridge data:")
            appendLine()
            if (submerged.isNotEmpty()) {
                appendLine("🚨 DANGER — The following bridges are submerged and must be avoided:")
                submerged.forEach { appendLine("  • ${it.name}") }
                appendLine()
            }
            if (warning.isNotEmpty()) {
                appendLine("⚠️ WARNING — The following bridges have unsafe conditions:")
                warning.forEach { appendLine("  • ${it.name}") }
                appendLine()
            }
            if (open.isNotEmpty()) {
                appendLine("✅ SAFE — The following bridges are currently open:")
                open.forEach { appendLine("  • ${it.name}") }
                appendLine()
            }
            if (bridges.isEmpty()) {
                appendLine("✅ No bridge data available at the moment.")
                appendLine("Please check the map screen for the latest bridge statuses.")
                appendLine()
            }
            appendLine("Please avoid submerged routes and use safe alternate paths.")
            appendLine("Stay alert during heavy rainfall and rising water levels.")
        }
    }
}

data class BridgeData(
    val name: String,
    val status: String
)