package com.example.evota.data.remote

import androidx.lifecycle.LiveData
import com.example.evota.data.helpers.ApiResponse
import com.example.evota.data.model.DeviceDetailsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface DeviceDetailsService {
    @GET("devices/{id}/")
    fun getDeviceDetails(@Path("id") deviceId: String): LiveData<ApiResponse<DeviceDetailsResponse>>
}