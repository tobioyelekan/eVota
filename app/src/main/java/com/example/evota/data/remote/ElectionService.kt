package com.example.evota.data.remote

import androidx.lifecycle.LiveData
import com.example.evota.data.helpers.ApiResponse
import com.example.evota.data.model.ElectionDataResponse
import retrofit2.http.GET

interface ElectionService {
    @GET("election/active/")
    fun getElectionData(): LiveData<ApiResponse<ElectionDataResponse>>
}