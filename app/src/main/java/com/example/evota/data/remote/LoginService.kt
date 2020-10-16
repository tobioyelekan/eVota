package com.example.evota.data.remote

import androidx.lifecycle.LiveData
import com.example.evota.data.helpers.ApiResponse
import com.example.evota.data.model.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface LoginService {
    @POST("auth/login/")
    fun login(@Body details: Map<String, String>): LiveData<ApiResponse<LoginResponse>>
}