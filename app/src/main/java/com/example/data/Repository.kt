package com.example.data

import com.example.BuildConfig
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.GenerationConfig
import com.example.network.Part
import com.example.network.RetrofitClient
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class Repository(
    private val draftDao: DraftDao,
    private val voiceSessionDao: VoiceSessionDao,
    private val dailyReportDao: DailyReportDao
) {
    private val moshi = RetrofitClient.moshiInstance

    // Database Flows
    val allDrafts: Flow<List<Draft>> = draftDao.getAllDrafts()
    val allVoiceSessions: Flow<List<VoiceSession>> = voiceSessionDao.getAllSessions()
    val allDailyReports: Flow<List<DailyReport>> = dailyReportDao.getAllReports()

    suspend fun insertDraft(draft: Draft) = withContext(Dispatchers.IO) {
        draftDao.insertDraft(draft)
    }

    suspend fun deleteDraft(id: Int) = withContext(Dispatchers.IO) {
        draftDao.deleteDraft(id)
    }

    suspend fun insertVoiceSession(session: VoiceSession) = withContext(Dispatchers.IO) {
        voiceSessionDao.insertSession(session)
    }

    suspend fun deleteVoiceSession(id: Int) = withContext(Dispatchers.IO) {
        voiceSessionDao.deleteSession(id)
    }

    suspend fun insertDailyReport(report: DailyReport) = withContext(Dispatchers.IO) {
        dailyReportDao.insertReport(report)
    }

    suspend fun getReportForDate(dateStr: String): DailyReport? = withContext(Dispatchers.IO) {
        dailyReportDao.getReportByDate(dateStr)
    }

    /**
     * Gemini Call: Analyzes draft texts, provides personalized corrections and mistake details.
     */
    suspend fun analyzeAndCorrectDraft(text: String, category: String): DraftAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext DraftAnalysisResult(
                correctedText = text,
                explanation = "Your draft was saved locally. Note: Gemini API key is missing. Please add your GEMINI_API_KEY in the Secrets panel or .env to enable instant AI grammar analysis and friendly correction tips!",
                mistakes = emptyList()
            )
        }

        val prompt = """
            You are a warm, deeply empathetic, and highly personalized communications coach and writing assistant for someone who struggles with English, overthinks, and feels anxious about speaking or writing to others (particularly in social or financial management contexts).
            
            Analyze the following text draft in the category "$category".
            Text to analyze: "$text"
            
            Perform spelling, grammar, phrasing, and style corrections. Be supportive, constructive, and show extreme care. Avoid dry or clinical feedback.
            
            You MUST return a JSON response matching the following schema. Do not include markdown backticks like ```json or any trailing characters other than valid JSON.
            
            JSON schema:
            {
              "correctedText": "The fully corrected, warm, natural-sounding English text",
              "explanation": "A very encouraging, friendly, and simple high-level summary of the enhancements made, validating the user's efforts and calming their overthinking.",
              "mistakes": [
                {
                  "original": "the exact stumbling or incorrect word/phrase",
                  "corrected": "the corrected word/phrase",
                  "explanation": "A kind, simple explanation of why this correction was made or how it sounds more confident and natural.",
                  "errorType": "Grammar" or "Vocabulary" or "Confidence" or "Clarity"
                }
              ]
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from Gemini")
            
            // Clean JSON string in case there are markdown backticks
            val cleanedJson = jsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val adapter = moshi.adapter(DraftAnalysisResult::class.java)
            adapter.fromJson(cleanedJson) ?: throw Exception("JSON conversion returned null")
        } catch (e: Exception) {
            e.printStackTrace()
            DraftAnalysisResult(
                correctedText = text,
                explanation = "We encountered a temporary issue connecting to the AI helper. Don't worry! Here is your original draft: \"$text\". Let's try again in a bit.",
                mistakes = listOf(
                    DraftMistake(
                        original = "N/A",
                        corrected = "N/A",
                        explanation = "Error: ${e.message}. Check your internet connection or API key.",
                        errorType = "System"
                    )
                )
            )
        }
    }

    /**
     * Gemini Call: Analyzes spoken transcript, pitch metrics, and returns speech therapy exercises.
     */
    suspend fun analyzeVoiceSession(
        scenarioName: String,
        userSpeechText: String,
        avgPitchHz: Float
    ): VoiceAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext VoiceAnalysisResult(
                analysisResult = "Voice practicing saved! (Mic analysis registered avg pitch of ${avgPitchHz.toInt()} Hz). Please add your GEMINI_API_KEY in the Secrets panel to unlock personalized AI speech coach suggestions, pitch reviews, and calming social skills guides.",
                pitchFeedback = "Your voice level averaged around ${avgPitchHz.toInt()} Hz.",
                exercises = listOf(
                    VoiceExercise(
                        name = "Diaphragmatic Breathing",
                        steps = "Inhale slowly through your nose for 4 seconds, expand your abdomen, hold for 2, and exhale gently for 6. This calms the nervous system and lowers pitch spiking.",
                        benefit = "Reduces anxiety-induced throat tightness."
                    )
                )
            )
        }

        val pitchDetails = """
            The user's measured average fundamental frequency (pitch) during this practice was ${avgPitchHz.toInt()} Hz.
            For reference, typical conversational voice pitches:
            - Adult Males: 85 Hz to 180 Hz
            - Adult Females: 165 Hz to 255 Hz
            A sudden spike high (above normal ranges) often signals anxiety, throat constriction, or fast pacing.
        """.trimIndent()

        val prompt = """
            You are an expert Speech Coach and empathetic executive communications trainer assisting someone with social overthinking, speech stumbles, and financial conversation anxiety.
            
            Analyze this speech practice session:
            - Practice Scenario: "$scenarioName"
            - What the user spoke: "$userSpeechText"
            - Pitch Data: $pitchDetails
            
            Evaluate their speaking flow, speed, and confidence.
            Provide compassionate, warm, constructive analysis. Address their pitch. If their pitch is in an anxious/high range for their category, recommend calming vocal relief exercises.
            
            Return a JSON response matching the following schema. No markdown backticks or text wrapper.
            
            JSON schema:
            {
              "analysisResult": "Empathetic review of speech pacing, clarity, confidence, and reassurance.",
              "pitchFeedback": "Specific, constructive coaching regarding their calculated pitch of ${avgPitchHz.toInt()} Hz, explaining what it implies (e.g., anxiety, tightness) and how to soften it.",
              "exercises": [
                {
                  "name": "Exercise Name",
                  "steps": "Clear, detailed, step-by-step instructions on how to do this exercise.",
                  "benefit": "Why this specific exercise helps relax their throat, calm their mind, or stabilize pitch."
                }
              ]
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.3f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from speech coach")
            
            val cleanedJson = jsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val adapter = moshi.adapter(VoiceAnalysisResult::class.java)
            adapter.fromJson(cleanedJson) ?: throw Exception("JSON conversion failed")
        } catch (e: Exception) {
            e.printStackTrace()
            VoiceAnalysisResult(
                analysisResult = "Registered pitch: ${avgPitchHz.toInt()} Hz. Transcription: \"$userSpeechText\". Let's practice taking slow deep breaths.",
                pitchFeedback = "Vocal energy was captured successfully. Please try analyzing again in a quiet room.",
                exercises = listOf(
                    VoiceExercise(
                        name = "Sigh of Relief Technique",
                        steps = "Inhale fully, then let out a vocalized sigh starting high and gliding all the way down to a low pitch. Repeat 3 times.",
                        benefit = "Instantly releases neck and larynx muscle tension."
                    )
                )
            )
        }
    }

    /**
     * Gemini Call: Generates a comforting consolidated Daily Report based on the day's logs.
     */
    suspend fun generateDailyReport(
        dateStr: String,
        drafts: List<Draft>,
        sessions: List<VoiceSession>
    ): DailyReportResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext DailyReportResult(
                summary = "Awesome work practicing today! You completed ${drafts.size} writing drafts and ${sessions.size} voice sessions. Set your Gemini API key in AI Studio to get personalized reports outlining common errors and growth plans.",
                commonMistakeAreas = "Practice logs indicate effort in writing categories: " + drafts.map { it.category }.distinct().joinToString(),
                customizedExercises = listOf(
                    CustomizedExercise(
                        name = "Anxiety & Finance Mastery",
                        details = "Write a mock text to a friend about split bills or shared costs. Use simple words. Remember: talking about finances is normal, and mistakes are how we learn!"
                    )
                )
            )
        }

        val draftsText = drafts.joinToString("\n") {
            "- [${it.category}] Original: \"${it.originalText}\" | Corrected: \"${it.correctedText}\" | Mistakes: ${it.mistakesJson}"
        }
        val sessionsText = sessions.joinToString("\n") {
            "- [${it.scenarioName}] Speech: \"${it.userSpeechText}\" | Pitch: ${it.averagePitchHz.toInt()} Hz (${it.pitchCategory}) | Feedback: ${it.analysisResult}"
        }

        val prompt = """
            You are a compassionate, highly supportive personal development companion.
            Generate a personalized consolidated daily growth report for: $dateStr.
            
            Here are the user's practice accomplishments today:
            
            WRITING DRAFTS COMPLETED TODAY:
            $draftsText
            
            VOICE CONVERSATION SESSIONS COMPLETED TODAY:
            $sessionsText
            
            Provide:
            1. An encouraging, comforting summary of their practice day. Acknowledge their anxiety and struggles (especially around conversations, financials, or English speaking) with deep validation.
            2. Identify common mistake areas or stumbling patterns.
            3. Propose 2 personalized growth exercises/topics they should focus on tomorrow to become more confident.
            
            Return a JSON response matching the following schema. No markdown backticks or text wrapper.
            
            JSON schema:
            {
              "summary": "Comforting, supportive summary praising their practice efforts today, easing overthinking.",
              "commonMistakeAreas": "Helpful, kind identification of common grammar, style, or vocal pitch stumbling blocks.",
              "customizedExercises": [
                {
                  "name": "Topic or Exercise Name",
                  "details": "Clear description of how they can practice this specific skill tomorrow."
                }
              ]
            }
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.4f
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("Empty response from Daily Report generator")

            val cleanedJson = jsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val adapter = moshi.adapter(DailyReportResult::class.java)
            adapter.fromJson(cleanedJson) ?: throw Exception("JSON conversion returned null")
        } catch (e: Exception) {
            e.printStackTrace()
            DailyReportResult(
                summary = "You practiced so hard today! ${drafts.size} drafts and ${sessions.size} voice lessons completed. You are making continuous progress.",
                commonMistakeAreas = "We found minor stumbling blocks. Focus on writing slow and breathing steady.",
                customizedExercises = listOf(
                    CustomizedExercise(
                        name = "Patience & Self-Compassion",
                        details = "Take 5 minutes to write down three things you did well today, even if they felt small. Celebrate taking action!"
                    )
                )
            )
        }
    }
}

// --- Moshi Compatible JSON Deserialization Classes ---

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class DraftAnalysisResult(
    val correctedText: String,
    val explanation: String,
    val mistakes: List<DraftMistake>
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class DraftMistake(
    val original: String,
    val corrected: String,
    val explanation: String,
    val errorType: String
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class VoiceAnalysisResult(
    val analysisResult: String,
    val pitchFeedback: String,
    val exercises: List<VoiceExercise>
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class VoiceExercise(
    val name: String,
    val steps: String,
    val benefit: String
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class DailyReportResult(
    val summary: String,
    val commonMistakeAreas: String,
    val customizedExercises: List<CustomizedExercise>
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class CustomizedExercise(
    val name: String,
    val details: String
)
