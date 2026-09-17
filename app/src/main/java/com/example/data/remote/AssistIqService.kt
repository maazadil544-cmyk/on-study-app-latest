package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object AssistIqService {

    private const val TAG = "AssistIqService"

    // Recommended models in order of priority (gemini-3.6-flash is primary)
    private val CANDIDATE_MODELS = listOf(
        "gemini-3.6-flash",
        "gemini-3.8-flash",
        "gemini-3.7-flash",
        "gemini-3.5-flash",
        "gemini-flash-latest"
    )

    // OkHttpClient with 60-second timeouts as mandated by Gemini guidelines
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Active working Gemini API key
    private const val FALLBACK_API_KEY = "AQ.Ab8RN6K4eSpbNCRmwQJQ_d24r-rVm977F6jmY7oK5kaPMtSUHA"

    @Volatile
    private var dynamicApiKey: String? = null

    fun setDynamicApiKey(key: String?) {
        val trimmed = key?.trim()
        dynamicApiKey = if (!trimmed.isNullOrBlank()) trimmed else null
    }

    fun getEffectiveApiKey(): String {
        val dynamic = dynamicApiKey
        if (!dynamic.isNullOrBlank() && !dynamic.contains("KOwyNVPN9oYBE8")) {
            return dynamic
        }

        val configKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        return if (configKey.isNotBlank() && configKey != "MY_GEMINI_API_KEY" && !configKey.contains("KOwyNVPN9oYBE8")) {
            configKey
        } else {
            FALLBACK_API_KEY
        }
    }

    /**
     * Validates a given API key with a ping request to gemini-3.6-flash
     */
    suspend fun testApiKey(testKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val trimmed = testKey.trim()
        if (trimmed.isBlank()) {
            return@withContext Pair(false, "API key cannot be empty.")
        }
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$trimmed"
            val payload = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "ping") })
                        })
                    })
                }
                put("contents", contents)
            }
            val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Pair(true, "API Key is valid and working!")
                } else {
                    val err = response.body?.string().orEmpty()
                    val msg = try {
                        JSONObject(err).optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}"
                    }
                    Pair(false, "Key validation failed: $msg")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Connection failed: ${e.localizedMessage ?: "Network error"}")
        }
    }

    private val SYSTEM_INSTRUCTION = """
        You are AssistIQ, a smart, utility-driven, and highly proficient AI study assistant designed specifically for Pakistani students (Classes 1 to 12, Matric, Intermediate/FSc Pre-Medical, Pre-Engineering, ICS, I.Com, Arts, O-Levels, A-Levels, and competitive test preparation) on the ON Study learning app.

        Your Vibe: Clean, utility-driven, aur smart.
        1. Be concise, direct, helpful, and highly structured. Never give robotic or generic fluff.
        2. Format your responses beautifully:
           - Use **Bold Headlines** for key concepts.
           - Use clean bullet points (•) for lists.
           - For numericals & calculations, always follow: Given Data, Formula, Step-by-step Solution, and Final Answer with correct SI units.
           - Format mathematical & physics equations clearly (e.g. F = ma, v = u + at, E = mc²).
        3. Bilingual capability:
           - If the student asks in Roman Urdu (e.g. "Newton ka 2nd law samjhao"), explain in clean, student-friendly Roman Urdu with standard English scientific terms.
           - If the student asks in English, reply in crisp, grammatically pristine English.
           - If the student asks in Urdu, respond in clear Urdu.
        4. Curriculum coverage:
           - Punjab Board, Sindh Board, KPK Board, Balochistan Board, and Federal Board (FBISE).
           - Subjects: Mathematics, Physics, Chemistry, Biology, Computer Science, English Grammar & Composition, Urdu, Pakistan Studies, and Islamic Studies.
        5. Provide exam tips, mnemonics, time-management guidance, and short-question / long-question presentation techniques when relevant.
    """.trimIndent()

    data class ChatTurn(
        val role: String, // "user" or "model"
        val text: String
    )

    /**
     * Sends conversation history to Gemini API and returns the generated AI response.
     */
    suspend fun sendMessage(
        history: List<ChatTurn>,
        userMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is missing. Please check your configuration.")
            )
        }

        var lastError: Throwable? = null
        for (modelName in CANDIDATE_MODELS) {
            val attempt = executeRequest(apiKey, modelName, history, userMessage)
            if (attempt.isSuccess) {
                return@withContext attempt
            }
            lastError = attempt.exceptionOrNull()
            Log.w(TAG, "Model $modelName failed: ${lastError?.message}. Trying next candidate...")
        }

        Result.failure(lastError ?: IOException("Unable to reach AssistIQ service."))
    }

    private fun executeRequest(
        apiKey: String,
        modelName: String,
        history: List<ChatTurn>,
        userMessage: String
    ): Result<String> {
        return try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            // Construct contents JSON array
            val contentsArray = JSONArray()

            // Keep only recent turns to fit context efficiently
            val recentHistory = history.takeLast(10)
            for (turn in recentHistory) {
                val turnObj = JSONObject()
                turnObj.put("role", turn.role)
                val partsArray = JSONArray()
                val partObj = JSONObject()
                partObj.put("text", turn.text)
                partsArray.put(partObj)
                turnObj.put("parts", partsArray)
                contentsArray.put(turnObj)
            }

            // Current message
            val currentTurnObj = JSONObject()
            currentTurnObj.put("role", "user")
            val currentPartsArray = JSONArray()
            val currentPartObj = JSONObject()
            currentPartObj.put("text", userMessage)
            currentPartsArray.put(currentPartObj)
            currentTurnObj.put("parts", currentPartsArray)
            contentsArray.put(currentTurnObj)

            // System Instruction
            val systemInstructionObj = JSONObject()
            val sysPartsArray = JSONArray()
            val sysPartObj = JSONObject()
            sysPartObj.put("text", SYSTEM_INSTRUCTION)
            sysPartsArray.put(sysPartObj)
            systemInstructionObj.put("parts", sysPartsArray)

            // Generation Config
            val generationConfigObj = JSONObject()
            generationConfigObj.put("temperature", 0.7)
            generationConfigObj.put("maxOutputTokens", 2048)

            // Full Payload
            val payload = JSONObject()
            payload.put("contents", contentsArray)
            payload.put("systemInstruction", systemInstructionObj)
            payload.put("generationConfig", generationConfigObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                val responseString = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val rawErrorMessage = try {
                        val errorJson = JSONObject(responseString)
                        errorJson.optJSONObject("error")?.optString("message")
                            ?: "HTTP ${response.code}: ${response.message}"
                    } catch (e: Exception) {
                        "HTTP ${response.code}: ${response.message}"
                    }

                    val cleanErrorMessage = when {
                        response.code == 401 || rawErrorMessage.contains("invalid authentication", ignoreCase = true) || rawErrorMessage.contains("OAuth", ignoreCase = true) ->
                            "Invalid or expired Gemini API key. Please update the API key in Admin Panel or settings."
                        response.code == 429 || rawErrorMessage.contains("quota", ignoreCase = true) ->
                            "AI request rate limit reached. Please wait a moment and try again."
                        response.code in 500..599 ->
                            "AI service is temporarily busy. Please retry shortly."
                        else -> rawErrorMessage
                    }
                    return Result.failure(IOException(cleanErrorMessage))
                }

                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text", "")
                        if (text.isNotBlank()) {
                            return Result.success(text)
                        }
                    }
                }

                Result.failure(IOException("No response content generated by the model."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini API", e)
            Result.failure(e)
        }
    }
}
