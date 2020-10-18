package com.example.evota.repository

import androidx.lifecycle.LiveData
import com.example.evota.data.helpers.*
import com.example.evota.data.model.VoteSuccessResponse
import com.example.evota.data.model.VoterData
import com.example.evota.data.model.VoterSearchResponse
import com.example.evota.data.model.VotingData
import com.example.evota.data.remote.ElectionService
import com.example.evota.data.remote.VoterService
import javax.inject.Inject

class VoterRepo @Inject constructor(
    private val voterService: VoterService,
    private val electionService: ElectionService,
    private val executors: AppExecutors
) {

    fun searchVoter(voterId: String): LiveData<Resource<VoterData>> {
        return object : NetworkOutBoundResource<VoterSearchResponse, VoterData>(executors) {
            override fun processResponse(response: ApiSuccessResponse<VoterSearchResponse>): VoterData {
                return response.body.results[0]
            }

            override fun createCall() = voterService.searchVoter(voterId)

        }.asLiveData()
    }

    fun voteNow(voteData: List<VotingData>): LiveData<Resource<VoteSuccessResponse>> {
        return object :
            NetworkOutBoundResource<VoteSuccessResponse, VoteSuccessResponse>(executors) {
            override fun processResponse(response: ApiSuccessResponse<VoteSuccessResponse>): VoteSuccessResponse {
                return response.body
            }

            override fun createCall() = electionService.voteNow(voteData)

        }.asLiveData()
    }

}