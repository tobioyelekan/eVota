package com.example.evota.repository

import androidx.lifecycle.LiveData
import com.auth0.android.jwt.Claim
import com.auth0.android.jwt.JWT
import com.example.evota.data.helpers.*
import com.example.evota.data.model.LoginResponse
import com.example.evota.data.remote.LoginService
import com.example.evota.data.sharedpreference.Preferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepo @Inject constructor(
    private val loginService: LoginService,
    private val executors: AppExecutors,
    private val preferences: Preferences
) {

    fun login(details: Map<String, String>): LiveData<Resource<Unit>> {
        return object : NetworkOutBoundResource<LoginResponse, Unit>(executors) {
            override fun processResponse(response: ApiSuccessResponse<LoginResponse>) {
                val token = response.body.access
                val jwt = JWT(token)
                saveUser(token, jwt.claims)
            }

            override fun createCall() = loginService.login(details)

        }.asLiveData()
    }

    fun saveUser(token: String, data: Map<String, Claim>) {
        Timber.tag("USER_ID").d(data["user_id"]?.asString()!!)
        preferences.setToken(token)
        preferences.setUserId((data["user_id"]?.asString()!!))
        preferences.setEmail((data["email"]?.asString()!!))
        preferences.setPhone((data["phone"]?.asString()!!))
        preferences.setImg((data["image"]?.asString()!!))
        preferences.setName((data["fullname"]?.asString()!!))
    }

}