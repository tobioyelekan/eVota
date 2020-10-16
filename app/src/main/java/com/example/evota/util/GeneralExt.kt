package com.example.evota.util

import com.example.evota.data.model.GeneralError

fun String?.parseError(): GeneralError {
    return GeneralError(this ?: "Something went wrong")
}