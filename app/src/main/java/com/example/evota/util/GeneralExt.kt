package com.example.evota.util

import android.text.Editable
import com.example.evota.data.model.GeneralError
import com.google.android.material.textfield.TextInputEditText

fun String?.parseError(): GeneralError {
    return GeneralError(this ?: "Something went wrong")
}

fun TextInputEditText.text() : String{
    return this.editableText.toString()
}