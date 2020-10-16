package com.example.evota.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.evota.data.model.GeneralError
import com.google.android.material.textfield.TextInputEditText

fun String?.parseError(): GeneralError {
    return GeneralError(this ?: "Something went wrong")
}

fun TextInputEditText.text(): String {
    return this.editableText.toString()
}

fun ImageView.loadImage(url: String) {
    if (url.isNotEmpty()) {
        Glide.with(context)
            .load(url)
            .into(this)
    }
}