/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.evota.data.helpers

import com.example.evota.data.model.GeneralError
import com.example.evota.util.parseError
import okhttp3.Headers
import retrofit2.HttpException
import retrofit2.Response

/**
 * Common class used by API responses.
 * @param <T> the type of the response object
</T> */
@Suppress("unused") // T is used in extending classes
sealed class ApiResponse<T> {
    companion object {
        fun <T> create(error: Throwable): ApiErrorResponse<T> {
            val generalError = if (error is HttpException) {
                error.response()?.errorBody()?.string().parseError()
            } else {
                val errorMsg = error.message ?: error.localizedMessage
                GeneralError(errorMsg)
            }

            return ApiErrorResponse(error, generalError)
        }

        fun <T> create(response: Response<T>): ApiResponse<T> {
            return if (response.isSuccessful) {
                if (response.body() == null) {
                    val res = Unit as T
                    ApiSuccessResponse(res, response.headers())
                } else {
                    ApiSuccessResponse(response.body()!!, response.headers())
                }
            } else {
                val errorBody = response.errorBody()?.string()

                val errorMsg = if (errorBody.isNullOrEmpty()) {
                    null
                } else {
                    errorBody
                }

                ApiErrorResponse(Throwable(errorMsg), errorBody.parseError())
            }
        }
    }
}

data class ApiSuccessResponse<T>(val body: T, val headers: Headers) : ApiResponse<T>()

data class ApiErrorResponse<T>(val throwable: Throwable, val error: GeneralError?) :
    ApiResponse<T>()
