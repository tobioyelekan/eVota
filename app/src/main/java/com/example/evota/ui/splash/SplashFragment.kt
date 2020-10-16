package com.example.evota.ui.splash

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.evota.R
import com.example.evota.ui.BaseFragment
import com.example.evota.util.EventObserver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : BaseFragment(R.layout.splash_fragment) {

    private val splashViewModel: SplashViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        Handler().postDelayed({
            splashViewModel.isUserLoggedIn()
        }, 2000)
    }

    private fun setupObservers() {
        splashViewModel.openLoginEvent.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(R.id.action_splashFragment_to_loginFragment)
        })

        splashViewModel.openConfirmDetailsEvent.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(R.id.action_splashFragment_to_confirmFragment)
        })
    }

}