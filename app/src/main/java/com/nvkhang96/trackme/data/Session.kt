package com.nvkhang96.trackme.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) var sessionId: Long = 0,
    var distance: Double,
    var averageSpeed: Float,
    var duration: String,
    var route: String
)
