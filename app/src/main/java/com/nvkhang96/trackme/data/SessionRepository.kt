package com.nvkhang96.trackme.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao
) {
    suspend fun createSession(_averageSpeed: Float, _distance: Double, _duration: String?, _route: String) {
        val newSession = Session(
            averageSpeed = _averageSpeed,
            distance = _distance,
            duration = _duration ?: "00:00:00",
            route = _route
        )
        sessionDao.insertSession(newSession)
    }

    fun getSessions() = sessionDao.getSessions()
}
