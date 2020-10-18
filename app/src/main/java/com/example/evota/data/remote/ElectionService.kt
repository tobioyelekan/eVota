package com.example.evota.data.remote

import androidx.lifecycle.LiveData
import com.example.evota.data.helpers.ApiResponse
import com.example.evota.data.model.CandidateDataResponse
import com.example.evota.data.model.ElectionDataResponse
import com.example.evota.data.model.VoteSuccessResponse
import com.example.evota.data.model.VotingData
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ElectionService {
    @GET("election/active/")
    fun getElectionData(): LiveData<ApiResponse<ElectionDataResponse>>

    @GET("election/active/")
    fun getCandidates(): LiveData<ApiResponse<CandidateDataResponse>>

    @POST("election/vote-now/")
    fun voteNow(@Body voteData: List<VotingData>): LiveData<ApiResponse<VoteSuccessResponse>>
}