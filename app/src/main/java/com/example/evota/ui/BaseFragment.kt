package com.example.evota.ui

import android.widget.Toast
import androidx.fragment.app.Fragment
import timber.log.Timber
import java.net.UnknownHostException

abstract class BaseFragment(layoutId: Int) : Fragment(layoutId) {

    protected fun onError(error: String?, throwableError: Throwable?) {
        val errorMessage = if (throwableError is UnknownHostException) {
            "No internet connection"
        } else {
            error ?: "Something went wrong"
        }

        showMessage(errorMessage)

        Timber.d("ERROR_MSG $errorMessage")
    }

    protected fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}