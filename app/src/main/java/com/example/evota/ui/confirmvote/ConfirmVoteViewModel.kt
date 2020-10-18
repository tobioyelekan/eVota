package com.example.evota.ui.confirmvote

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.example.evota.repository.VoterRepo

class ConfirmVoteViewModel @ViewModelInject constructor(voterRepo: VoterRepo) : ViewModel() {

    private val _voterId = MutableLiveData<String>()

    fun getVoter(voterId: String) {
        _voterId.value = voterId
    }

    val voter = _voterId.switchMap {
        voterRepo.searchVoter(it)
    }
}