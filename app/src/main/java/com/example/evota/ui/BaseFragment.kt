package com.example.evota.ui

import android.widget.Toast
import androidx.fragment.app.Fragment
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.net.UnknownHostException

abstract class BaseFragment(layoutId: Int) : Fragment(layoutId) {

    protected fun onError(error: String?, throwableError: Throwable?): String {
        val errorMessage = if (throwableError is UnknownHostException) {
            "No internet connection"
        } else {
            var finalErrorMsg: String? = null

            if (error != null) {
                try {
                    val errorData = JSONObject(error)
                    if (errorData.has("detail")) {
                        finalErrorMsg = errorData.getString("detail")
                    }
                } catch (e: JSONException) {

                }
            }

            finalErrorMsg ?: "Something went wrong"
        }

        Timber.d("ERROR_MSG $errorMessage")

        return error!!
    }

    protected fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}