package com.example.evota.repository

import androidx.lifecycle.LiveData
import com.example.evota.data.helpers.*
import com.example.evota.data.model.VoterData
import com.example.evota.data.model.VoterSearchResponse
import com.example.evota.data.remote.VoterService
import javax.inject.Inject

class VoterRepo @Inject constructor(
    private val service: VoterService,
    private val executors: AppExecutors
) {

    fun searchVoter(voterId: String): LiveData<Resource<VoterData>> {
        return object : NetworkOutBoundResource<VoterSearchResponse, VoterData>(executors) {
            override fun processResponse(response: ApiSuccessResponse<VoterSearchResponse>): VoterData {
                return response.body.results[0]
            }

            override fun createCall() = service.searchVoter(voterId)

        }.asLiveData()
    }

}