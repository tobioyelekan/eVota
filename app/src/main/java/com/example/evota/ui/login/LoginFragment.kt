package com.example.evota.ui.login

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.evota.R
import com.example.evota.ui.BaseFragment
import com.example.evota.util.EventObserver
import com.example.evota.util.text
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.login_fragment.*

@AndroidEntryPoint
class LoginFragment : BaseFragment(R.layout.login_fragment) {

    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        login.setOnClickListener {
            viewModel.validate(userInput.text(), passwordInput.text())
        }

        setupValidationObservers()
    }

    private fun setupValidationObservers() {
        viewModel.validatePassword.observe(viewLifecycleOwner, Observer {
            when {
                it.isEmpty() -> passwordInputLayout.isErrorEnabled = false
                else -> passwordInputLayout.error = it
            }
        })

        viewModel.validateEmail.observe(viewLifecycleOwner, Observer {
            when {
                it.isEmpty() -> userInputLayout.isErrorEnabled = false
                else -> userInputLayout.error = it
            }
        })

        viewModel.errorMessage.observe(viewLifecycleOwner, EventObserver {
            showMessage(it)
        })
    }


}