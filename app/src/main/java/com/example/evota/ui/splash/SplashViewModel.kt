package com.example.evota.ui.splash

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.evota.data.sharedpreference.Preferences
import com.example.evota.util.Event

class SplashViewModel @ViewModelInject constructor(private val preferences: Preferences) :
    ViewModel() {
    // TODO: Implement the ViewModel

    private val _openLoginEvent = MutableLiveData<Event<Unit>>()
    val openLoginEvent: LiveData<Event<Unit>> = _openLoginEvent

    private val _openConfirmDetailsEvent = MutableLiveData<Event<Unit>>()
    val openConfirmDetailsEvent: LiveData<Event<Unit>> = _openConfirmDetailsEvent

    fun isUserLoggedIn() {
        when (preferences.isUserLoggedIn()) {
            true -> {
                _openConfirmDetailsEvent.value = Event(Unit)
            }
            false -> {
                _openLoginEvent.value = Event(Unit)
            }
        }
    }
}