package com.example.evota.repository

import androidx.lifecycle.LiveData
import com.example.evota.data.helpers.*
import com.example.evota.data.model.Candidate
import com.example.evota.data.model.CandidateDataResponse
import com.example.evota.data.remote.ElectionService

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CandidateRepo @Inject constructor(
    private val electionService: ElectionService,
    private val appExecutors: AppExecutors
) {
    fun getCandidates(): LiveData<Resource<List<List<Candidate>>>> {
        return object :
            NetworkOutBoundResource<CandidateDataResponse, List<List<Candidate>>>(appExecutors) {
            override fun processResponse(response: ApiSuccessResponse<CandidateDataResponse>): List<List<Candidate>> {
                return response.body.data.map { candidateData ->
                    candidateData.candidates.map { candidate ->
                        Candidate(
                            electionTitle = candidateData.title,
                            electionId = candidateData.id,
                            id = candidate.id,
                            name = candidate.name,
                            img = candidate.img,
                            party = candidate.party
                        )
                    }
                }
            }

            override fun createCall() = electionService.getCandidates()

        }.asLiveData()
    }
}