package com.example.evota.data.helpers


import com.example.evota.data.sharedpreference.Preferences
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

@Singleton
class TokenInterceptor @Inject constructor(private val preferences: Preferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()

        if (preferences.isUserLoggedIn()) {
            request.addHeader("authorization", "Bearer ${preferences.getToken()}")
        }

        return chain.proceed(request.build())
    }
}
