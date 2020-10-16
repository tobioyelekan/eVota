package com.example.evota.di

import android.content.Context
import com.example.evota.BuildConfig
import com.example.evota.BuildConfig.BASE_URL
import com.example.evota.data.helpers.LiveDataCallAdapterFactory
import com.example.evota.data.helpers.TokenInterceptor
import com.example.evota.data.remote.LoginService
import com.example.evota.data.sharedpreference.EVotaSharedPreferences
import com.example.evota.data.sharedpreference.Preferences
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
class AppModule {
    @Provides
    @Singleton
    fun providesPreferences(@ApplicationContext context: Context): Preferences {
        return EVotaSharedPreferences(context)
    }


    @Provides
    @Singleton
    fun providesRetrofit(tokenInterceptor: TokenInterceptor): Retrofit {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(tokenInterceptor)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(interceptor)
        }

        val gson = GsonBuilder()
            .setLenient()
            .create()

        return Retrofit.Builder()
            .client(builder.build())
            .addConverterFactory(ScalarsConverterFactory.create())
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .baseUrl(BASE_URL)
            .build()
    }

    @Provides
    @Singleton
    fun providesLoginService(retrofit: Retrofit): LoginService {
        return retrofit.create(LoginService::class.java)
    }
}