package com.nvkhang96.trackme.viewmodels

import android.location.Location
import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.SphericalUtil
import com.nvkhang96.trackme.data.SessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class RecordViewModel @ViewModelInject constructor(
    private val sessionRepository: SessionRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _startLatLng = MutableLiveData<LatLng?>()
    val startLatLng: LiveData<LatLng?> = _startLatLng

    private val _endLatLng = MutableLiveData<LatLng?>()
    val endLatLng: LiveData<LatLng?> = _endLatLng

    private val _distance = MutableLiveData(0.0)
    val distance: LiveData<String> = Transformations.map(_distance) {
        String.format("%.2f", it / KILOMETERS_TO_METERS_MULTIPLIER)
    }

    private val _speed = MutableLiveData(0f)
    val speed: LiveData<String> = Transformations.map(_speed) {
        String.format("%.2f", it)
    }
    private val speedRecords = ArrayList<Float>()

    private var timer = Timer()

    private val _durationInMilliseconds = MutableLiveData(0L)
    val duration: LiveData<String> = Transformations.map(_durationInMilliseconds) {
        val millisecondsToHours = TimeUnit.MILLISECONDS.toHours(it)
        val millisecondsToMinutes = TimeUnit.MILLISECONDS.toMinutes(it)
        String.format(
            "%02d:%02d:%02d",
            millisecondsToHours,
            millisecondsToMinutes - TimeUnit.HOURS.toMinutes(millisecondsToHours),
            TimeUnit.MILLISECONDS.toSeconds(it) - TimeUnit.MINUTES.toSeconds(millisecondsToMinutes)
        )
    }

    var route = ArrayList<LatLng>()

    val pauseButtonVisible = Transformations.map(getTrackingState()) {
        it
    }
    val resumeButtonVisible = Transformations.map(getTrackingState()) {
        !it
    }
    val stopButtonVisible = Transformations.map(getTrackingState()) {
        !it
    }

    fun initializeTracking() {
        Log.d(TAG, "initializeTracking")
        _startLatLng.value = null
        _endLatLng.value = null
        route.clear()
        _distance.value = 0.0
        _speed.value = 0f
        speedRecords.clear()
        _durationInMilliseconds.value = 0
        clearTrackingState()
    }

    fun startTracking() {
        Log.d(TAG, "startTracking")
        savedStateHandle.set(TRACKING_LOCATION_SAVED_STATE_KEY, true)
        startTimer()
    }

    private fun startTimer() {
        timer.cancel()

        val timerTask = object : TimerTask() {
            override fun run() {
                if (isTracking()) {
                    _durationInMilliseconds.postValue(
                        (_durationInMilliseconds.value ?: 0) + TIMER_PERIOD_MS
                    )
                }
            }
        }
        timer = Timer()
        timer.scheduleAtFixedRate(timerTask, TIMER_PERIOD_MS, TIMER_PERIOD_MS)
    }

    private fun stopTimer() {
        timer.cancel()
    }

    fun pauseTracking() {
        savedStateHandle.set(TRACKING_LOCATION_SAVED_STATE_KEY, false)
        stopTimer()
    }

    private fun getAvgSpeed(): Float {
        if (speedRecords.isEmpty()) {
            return 0f
        }

        var sumSpeed = 0f
        for (speedRecord in speedRecords) {
            sumSpeed += speedRecord
        }
        return sumSpeed / speedRecords.size
    }

    fun stopTracking() {
        viewModelScope.launch(Dispatchers.IO) {
            sessionRepository.createSession(
                _averageSpeed = getAvgSpeed(),
                _distance = (_distance.value ?: 0.0) / KILOMETERS_TO_METERS_MULTIPLIER,
                _duration = duration.value,
                _route = PolyUtil.encode(route)
            )
            stopTimer()
        }
    }

    fun isTracking() = savedStateHandle.get<Boolean?>(TRACKING_LOCATION_SAVED_STATE_KEY) ?: true
    fun clearTrackingState() {
        savedStateHandle.set(TRACKING_LOCATION_SAVED_STATE_KEY, true)
    }

    private fun getTrackingState(): MutableLiveData<Boolean> {
        return savedStateHandle.getLiveData(TRACKING_LOCATION_SAVED_STATE_KEY, true)
    }

    private fun setStartLatLng(latLng: LatLng) {
        route.add(latLng)
        _startLatLng.value = latLng
    }

    private fun setEndLatLng(latLng: LatLng) {
        route.add(latLng)
        _endLatLng.value = latLng
    }

    fun receiveLocationUpdate(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)

        if (isStartLocation()) {
            setStartLatLng(latLng)
            return
        }

        val justMoveLength = SphericalUtil.computeDistanceBetween(route.last(), latLng)
        if (justMoveLength >= MINIMUM_ACCEPTABLE_LENGTH_IN_METERS) {
            setEndLatLng(latLng)
            increaseDistance(justMoveLength)
            updateSpeed(location.speed)
        }

        if (justMoveLength < MINIMUM_LENGTH_TO_ACCEPT_SPEED) {
            updateSpeed(0f)
        }
    }

    private fun isStartLocation() = route.isEmpty()

    companion object {
        private val TAG = RecordViewModel::class.java.simpleName
        private const val TRACKING_LOCATION_SAVED_STATE_KEY = "TRACKING_LOCATION_SAVED_STATE_KEY"
        private const val MINIMUM_ACCEPTABLE_LENGTH_IN_METERS = 10
        private const val MINIMUM_LENGTH_TO_ACCEPT_SPEED = 5
        private const val KILOMETERS_TO_METERS_MULTIPLIER = 1000
        private const val METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR_MULTIPLIER = 3.6f
        private const val TIMER_PERIOD_MS = 1000L
    }

    private fun increaseDistance(length: Double) {
        _distance.value = (_distance.value ?: 0.0) + length
    }

    private fun updateSpeed(newSpeed: Float) {
        val newSpeedInKilometerPerHour =
            newSpeed * METERS_PER_SECOND_TO_KILOMETERS_PER_HOUR_MULTIPLIER
        _speed.value = newSpeedInKilometerPerHour
        speedRecords.add(newSpeedInKilometerPerHour)
    }
}
