package com.example.hingelevel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LevelHistoryUiState(
    val lastSevenDays: Map<LocalDate, LevelData?> = emptyMap(),
    val latestLevel: LevelData? = null
)

class LevelViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "level-database"
    ).addMigrations(MIGRATION_1_2).build()

    private val levelDao = db.levelDao()

    private val _uiState = MutableStateFlow(LevelHistoryUiState())
    val uiState: StateFlow<LevelHistoryUiState> = _uiState.asStateFlow()

    init {
        loadLevelHistory()
    }

    private fun loadLevelHistory() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val sevenDaysAgo = today.minusDays(6)
            val historyData = levelDao.getLevelsFrom(sevenDaysAgo.toEpochDay())
            val latestLevelData = levelDao.getLatestLevel()

            val dateMap = (0..6).associate {
                val date = today.minusDays(it.toLong())
                val dataForDate = historyData.find { ld -> LocalDate.ofEpochDay(ld.date) == date }
                date to dataForDate
            }

            _uiState.update {
                it.copy(lastSevenDays = dateMap, latestLevel = latestLevelData)
            }
        }
    }

    fun recordLevel(level: Int, dayAtLevel: Int, goalLevel: Int?) {
        viewModelScope.launch {
            val newEntry = LevelData(
                date = LocalDate.now().toEpochDay(),
                level = level,
                dayAtLevel = dayAtLevel,
                goalLevel = goalLevel
            )
            levelDao.insertLevel(newEntry)

            // Prune old data after inserting the new entry.
            levelDao.pruneOldData()

            loadLevelHistory() // Refresh data after recording
        }
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE level_data ADD COLUMN goalLevel INTEGER")
            }
        }
    }
}
