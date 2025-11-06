package com.example.hingelevel

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import java.time.LocalDate

@Entity(tableName = "level_data")
data class LevelData(
    @PrimaryKey val date: Long, // Storing LocalDate as Long (epochDay)
    val level: Int,
    val dayAtLevel: Int,
    val goalLevel: Int? = null
)

@Dao
interface LevelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevel(levelData: LevelData)

    @Query("SELECT * FROM level_data WHERE date >= :startDate ORDER BY date DESC")
    suspend fun getLevelsFrom(startDate: Long): List<LevelData>

    @Query("SELECT * FROM level_data ORDER BY date DESC LIMIT 1")
    suspend fun getLatestLevel(): LevelData?

    @Query("DELETE FROM level_data WHERE date < :sevenDaysAgoEpoch AND date != :latestDateEpoch")
    suspend fun deleteOldEntries(sevenDaysAgoEpoch: Long, latestDateEpoch: Long)

    @Transaction
    suspend fun pruneOldData() {
        val latestLevel = getLatestLevel()
        // If there's no data at all, there's nothing to do.
        if (latestLevel == null) return

        val sevenDaysAgo = LocalDate.now().minusDays(7)
        val latestDate = LocalDate.ofEpochDay(latestLevel.date)

        // Only prune if the latest entry is within the last 7 days.
        // If the latest entry is older, we don't delete anything, preserving it as requested.
        if (latestDate.isAfter(sevenDaysAgo) || latestDate.isEqual(sevenDaysAgo)) {
            deleteOldEntries(sevenDaysAgo.toEpochDay(), latestLevel.date)
        }
    }
}

@Database(entities = [LevelData::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun levelDao(): LevelDao
}
