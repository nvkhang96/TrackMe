package com.nvkhang96.trackme.viewmodels

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.nvkhang96.trackme.data.Session
import com.nvkhang96.trackme.data.SessionRepository

class HistoryViewModel @ViewModelInject internal constructor(
    sessionRepository: SessionRepository
) : ViewModel() {
    val sessions: LiveData<List<Session>> = sessionRepository.getSessions().asLiveData()
}
