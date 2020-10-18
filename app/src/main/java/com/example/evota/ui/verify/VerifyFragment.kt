package com.example.evota.ui.verify

import android.os.Bundle
import android.view.View
import com.example.evota.R
import com.example.evota.ui.BaseFragment
import kotlinx.android.synthetic.main.verify_fragment.*

class VerifyFragment : BaseFragment(R.layout.verify_fragment) {

    private lateinit var viewModel: VerifyViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fingerprint.setOnClickListener {

        }
    }
}