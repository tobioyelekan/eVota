package com.example.evota.ui.login

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.evota.R
import com.example.evota.data.helpers.Status
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
        setupLoginObserver()
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

    private fun setupLoginObserver() {
        viewModel.loginUser.observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.LOADING -> {
                    login.visibility = View.GONE
                    loading.visibility = View.VISIBLE
                }

                Status.SUCCESS -> {
                    login.visibility = View.VISIBLE
                    loading.visibility = View.GONE

                    findNavController().navigate(R.id.action_loginFragment_to_confirmFragment)
                }

                Status.ERROR -> {
                    login.visibility = View.VISIBLE
                    loading.visibility = View.GONE
                    onError(it.message, it.throwable)
                }
            }
        })
    }

}