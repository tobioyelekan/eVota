package com.example.evota.ui.confirmvote

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.example.evota.data.model.VotingData
import com.example.evota.repository.VoterRepo

class ConfirmVoteViewModel @ViewModelInject constructor(voterRepo: VoterRepo) : ViewModel() {

    private val _voterId = MutableLiveData<String>()
    private val _voteData = MutableLiveData<List<VotingData>>()

    fun getVoter(voterId: String) {
        _voterId.value = voterId
    }

    fun voteNow(list: List<VotingData>) {
        _voteData.value = list
    }

    val voter = _voterId.switchMap {
        voterRepo.searchVoter(it)
    }

    val votingNow = _voteData.switchMap {
        voterRepo.voteNow(it)
    }
}