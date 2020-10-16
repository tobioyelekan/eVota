package com.example.evota.ui.login

import androidx.core.util.PatternsCompat
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.evota.util.Event

class LoginViewModel @ViewModelInject constructor() : ViewModel() {

    private val _validateEmail = MutableLiveData<String>()
    val validateEmail: LiveData<String> = _validateEmail

    private val _validatePassword = MutableLiveData<String>()
    val validatePassword: LiveData<String> = _validatePassword

    private val _errorMessage = MutableLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> = _errorMessage

    fun validate(email: String, password: String) {
        var error = false

        when {
            email.isEmpty() -> {
                _validateEmail.value = "email cannot be empty"
                error = true
            }
            !PatternsCompat.EMAIL_ADDRESS.matcher(email.trim()).matches() -> {
                _validateEmail.value = "invalid email"
                error = true
            }
            else -> _validateEmail.value = ""
        }

        when {
            password.isEmpty() -> {
                _validatePassword.value = "password cannot be empty"
                error = true
            }
            password.length < 4 -> {
                _validatePassword.value = "password cannot be less than 4 characters"
                error = true
            }
            else -> _validatePassword.value = ""
        }

        if (!error) {
//            login(email, password)
            _errorMessage.value = Event("hurray")
        }
    }

}