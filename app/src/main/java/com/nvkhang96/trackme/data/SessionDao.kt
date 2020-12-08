package com.nvkhang96.trackme.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: Session): Long

    @Query("SELECT * FROM sessions ORDER BY sessionId DESC")
    fun getSessions(): Flow<List<Session>>
}
