package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drafts")
data class Draft(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalText: String,
    val correctedText: String,
    val category: String, // e.g., "Social Media", "Casual Text", "Professional/Financial"
    val explanation: String, // High-level explanation
    val mistakesJson: String, // JSON list of MistakeDetail
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "voice_sessions")
data class VoiceSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scenarioName: String, // e.g., "Answering a financial text", "Casual Greeting", "Phone call simulation"
    val userSpeechText: String, // Transcribed or simulated speech
    val analysisResult: String, // Gemini comprehensive speech coach feedback
    val averagePitchHz: Float, // Simulated or analyzed pitch
    val pitchCategory: String, // "Normal", "Slightly High", "Anxious", etc.
    val specificExercisesJson: String, // JSON list of recommended speech exercises
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_reports")
data class DailyReport(
    @PrimaryKey val dateStr: String, // Format: YYYY-MM-DD
    val summary: String, // Overall review of the day
    val mistakesCount: Int,
    val commonMistakeAreas: String, // Primary areas to work on
    val customizedExercisesJson: String, // Recommended customized exercises
    val timestamp: Long = System.currentTimeMillis()
)

data class MistakeDetail(
    val original: String,
    val corrected: String,
    val explanation: String,
    val errorType: String
)
