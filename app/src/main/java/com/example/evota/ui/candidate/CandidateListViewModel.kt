package com.example.evota.ui.candidate

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.example.evota.data.model.Candidate
import com.example.evota.repository.CandidateRepo
import com.example.evota.util.Event

class CandidateListViewModel @ViewModelInject constructor(candidateRepo: CandidateRepo) :
    ViewModel() {

    private val _candidates = MutableLiveData<Unit>()

    private val _message = MutableLiveData<Event<String>>()
    val message: LiveData<Event<String>> = _message

    private val _navigate = MutableLiveData<Event<Pair<Candidate, Candidate>>>()
    val navigate: LiveData<Event<Pair<Candidate, Candidate>>> = _navigate

    val selectedCandidates = HashMap<String, Int>()

    init {
        _candidates.value = Unit
    }

    val candidates = _candidates.switchMap {
        candidateRepo.getCandidates()
    }

    fun candidateSelected(position: Int, electionId: String) {
        selectedCandidates[electionId] = position
    }

    fun getSelectedCandidates() {
        if (selectedCandidates.size < 2) {
            _message.value = Event("please select a candidate for each of the election")
        } else {
            val electionIds = selectedCandidates.keys.toList()
            val candidateIndexOne = selectedCandidates[electionIds[0]]!!
            val candidateIndexTwo = selectedCandidates[electionIds[1]]!!
            val candidateOne = candidates.value!!.data?.get(0)!![candidateIndexOne]
            val candidateTwo = candidates.value!!.data?.get(1)!![candidateIndexTwo]
            _navigate.value = Event(Pair(candidateOne, candidateTwo))
        }
    }

    fun clear() {
        selectedCandidates.clear()
    }
}