package com.example.evota.ui.confirmdetails

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.example.evota.repository.ConfirmDetailRepo

class ConfirmDetailsViewModel @ViewModelInject constructor(repo: ConfirmDetailRepo) : ViewModel() {

    private val _deviceId = MutableLiveData<String>()

    init {
        _deviceId.value = "3e0079cc-d100-4c1f-8874-fe6092d88f13"
    }

    val deviceData = _deviceId.switchMap {
        repo.getDeviceData(it)
    }

    val electionData = _deviceId.switchMap {
        repo.getElectionData()
    }
}