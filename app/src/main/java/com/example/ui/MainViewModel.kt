package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioAnalyzer
import com.example.data.AppDatabase
import com.example.data.DailyReport
import com.example.data.Draft
import com.example.data.DraftAnalysisResult
import com.example.data.Repository
import com.example.data.VoiceAnalysisResult
import com.example.data.VoiceSession
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = Repository(db.draftDao(), db.voiceSessionDao(), db.dailyReportDao())
    private val audioAnalyzer = AudioAnalyzer()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    // Database Streams
    val drafts: StateFlow<List<Draft>> = repository.allDrafts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val voiceSessions: StateFlow<List<VoiceSession>> = repository.allVoiceSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyReports: StateFlow<List<DailyReport>> = repository.allDailyReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Input States
    private val _draftInput = MutableStateFlow("")
    val draftInput = _draftInput.asStateFlow()

    private val _draftCategory = MutableStateFlow("Social Media")
    val draftCategory = _draftCategory.asStateFlow()

    // AI Status States
    private val _writingState = MutableStateFlow<WritingUiState>(WritingUiState.Idle)
    val writingState = _writingState.asStateFlow()

    // Voice practice States
    private val _activeScenario = MutableStateFlow("Financial Discussion / Anxiety Practice")
    val activeScenario = _activeScenario.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    val livePitch: StateFlow<Float> = audioAnalyzer.livePitch
    val liveVolume: StateFlow<Float> = audioAnalyzer.liveVolume
    val pitchHistory: StateFlow<List<Float>> = audioAnalyzer.pitchHistory

    private val _voiceState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val voiceState = _voiceState.asStateFlow()

    // Daily report generation states
    private val _reportState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val reportState = _reportState.asStateFlow()

    // Selected Daily Report to show in UI
    private val _selectedReport = MutableStateFlow<DailyReport?>(null)
    val selectedReport = _selectedReport.asStateFlow()

    // Today's aggregated progress stats
    val todayStats = combine(drafts, voiceSessions) { todayDrafts, todaySessions ->
        val todayStr = getTodayDateStr()
        val dCount = todayDrafts.count { isToday(it.timestamp) }
        val sCount = todaySessions.count { isToday(it.timestamp) }
        val dMistakes = todayDrafts.filter { isToday(it.timestamp) }.sumOf { draft ->
            try {
                // Approximate mistake count based on JSON length or basic parse
                draft.mistakesJson.split("\"original\"").size - 1
            } catch (e: Exception) {
                0
            }
        }
        TodayStats(dCount, sCount, dMistakes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayStats(0, 0, 0))

    fun updateDraftInput(text: String) {
        _draftInput.value = text
    }

    fun updateDraftCategory(category: String) {
        _draftCategory.value = category
    }

    fun updateActiveScenario(scenario: String) {
        _activeScenario.value = scenario
    }

    /**
     * Submit user's writing draft, analyze with Gemini, and persist to local Room DB.
     */
    fun analyzeWritingDraft() {
        val text = _draftInput.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _writingState.value = WritingUiState.Loading
            try {
                val category = _draftCategory.value
                val result = repository.analyzeAndCorrectDraft(text, category)

                // Serialize mistakes back to save in local DB
                val mistakesAdapter = moshi.adapter(DraftAnalysisResult::class.java)
                val serializedResult = mistakesAdapter.toJson(result)

                val draft = Draft(
                    originalText = text,
                    correctedText = result.correctedText,
                    category = category,
                    explanation = result.explanation,
                    mistakesJson = serializedResult // stores complete feedback
                )

                repository.insertDraft(draft)
                _writingState.value = WritingUiState.Success(result)
            } catch (e: Exception) {
                _writingState.value = WritingUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun clearWritingState() {
        _writingState.value = WritingUiState.Idle
        _draftInput.value = ""
    }

    /**
     * Start recording & tracking pitch.
     */
    fun startVoiceRecording() {
        _isRecording.value = true
        audioAnalyzer.startAnalyzing()
        _voiceState.value = VoiceUiState.Recording
    }

    /**
     * Stop recording and send voice pitch data and transcript draft to Gemini for analysis.
     */
    fun stopVoiceRecordingAndAnalyze(userSpeechDraft: String) {
        _isRecording.value = false
        audioAnalyzer.stopAnalyzing()

        val history = pitchHistory.value
        val avgPitch = if (history.isNotEmpty()) history.average().toFloat() else 150f
        val pitchCategory = when {
            avgPitch > 250f -> "Anxious / Spiking High"
            avgPitch > 195f -> "Conversational (High)"
            avgPitch > 140f -> "Conversational (Medium)"
            else -> "Conversational (Low)"
        }

        viewModelScope.launch {
            _voiceState.value = VoiceUiState.Analyzing
            try {
                val scenario = _activeScenario.value
                val speechText = userSpeechDraft.ifBlank { "I stumble and overthink whenever I speak. I felt a bit anxious during this finance discussion." }
                val result = repository.analyzeVoiceSession(scenario, speechText, avgPitch)

                // Save list of exercises to JSON
                val exercisesJson = moshi.adapter(List::class.java).toJson(result.exercises)

                val session = VoiceSession(
                    scenarioName = scenario,
                    userSpeechText = speechText,
                    analysisResult = result.analysisResult,
                    averagePitchHz = avgPitch,
                    pitchCategory = pitchCategory,
                    specificExercisesJson = exercisesJson
                )

                repository.insertVoiceSession(session)
                _voiceState.value = VoiceUiState.Success(result, avgPitch, pitchCategory)
            } catch (e: Exception) {
                _voiceState.value = VoiceUiState.Error(e.message ?: "Vocal coach connection error")
            }
        }
    }

    fun clearVoiceState() {
        _voiceState.value = VoiceUiState.Idle
    }

    /**
     * Delete a historical draft
     */
    fun deleteDraft(id: Int) {
        viewModelScope.launch {
            repository.deleteDraft(id)
        }
    }

    /**
     * Delete a voice session
     */
    fun deleteVoiceSession(id: Int) {
        viewModelScope.launch {
            repository.deleteVoiceSession(id)
        }
    }

    /**
     * Generates a supportive summary report for today
     */
    fun generateTodayReport() {
        viewModelScope.launch {
            _reportState.value = ReportUiState.Loading
            try {
                val todayStr = getTodayDateStr()
                val todayDrafts = drafts.value.filter { isToday(it.timestamp) }
                val todaySessions = voiceSessions.value.filter { isToday(it.timestamp) }

                if (todayDrafts.isEmpty() && todaySessions.isEmpty()) {
                    _reportState.value = ReportUiState.Error("Please complete at least one writing draft or voice practice session today to generate a report!")
                    return@launch
                }

                val result = repository.generateDailyReport(todayStr, todayDrafts, todaySessions)
                
                // Convert customized exercises list to JSON
                val exercisesAdapter = moshi.adapter(List::class.java)
                val exercisesJson = exercisesAdapter.toJson(result.customizedExercises)

                val report = DailyReport(
                    dateStr = todayStr,
                    summary = result.summary,
                    mistakesCount = todayStats.value.totalMistakes,
                    commonMistakeAreas = result.commonMistakeAreas,
                    customizedExercisesJson = exercisesJson
                )

                repository.insertDailyReport(report)
                _selectedReport.value = report
                _reportState.value = ReportUiState.Success(report)
            } catch (e: Exception) {
                _reportState.value = ReportUiState.Error(e.message ?: "Failed to generate daily report")
            }
        }
    }

    fun selectReport(report: DailyReport?) {
        _selectedReport.value = report
    }

    // --- Helpers ---
    private fun getTodayDateStr(): String {
        return SimpleDateFormat("yyyy-MM-DD", Locale.getDefault()).format(Date())
    }

    private fun isToday(timestamp: Long): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val itemDate = sdf.format(Date(timestamp))
        val todayDate = sdf.format(Date())
        return itemDate == todayDate
    }
}

// --- UI States ---

sealed interface WritingUiState {
    object Idle : WritingUiState
    object Loading : WritingUiState
    data class Success(val result: DraftAnalysisResult) : WritingUiState
    data class Error(val message: String) : WritingUiState
}

sealed interface VoiceUiState {
    object Idle : VoiceUiState
    object Recording : VoiceUiState
    object Analyzing : VoiceUiState
    data class Success(
        val result: VoiceAnalysisResult,
        val avgPitchHz: Float,
        val pitchCategory: String
    ) : VoiceUiState
    data class Error(val message: String) : VoiceUiState
}

sealed interface ReportUiState {
    object Idle : ReportUiState
    object Loading : ReportUiState
    data class Success(val report: DailyReport) : ReportUiState
    data class Error(val message: String) : ReportUiState
}

data class TodayStats(
    val draftsCount: Int,
    val sessionsCount: Int,
    val totalMistakes: Int
)
