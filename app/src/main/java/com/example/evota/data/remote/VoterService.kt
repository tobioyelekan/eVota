package com.example.evota.data.remote

import androidx.lifecycle.LiveData
import com.example.evota.data.helpers.ApiResponse
import com.example.evota.data.model.VoterSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface VoterService {
    @GET("voters/")
    fun searchVoter(@Query("search") voterId: String): LiveData<ApiResponse<VoterSearchResponse>>
}