package com.example.evota.ui.confirmdetails

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.evota.R
import com.example.evota.data.helpers.Status
import com.example.evota.ui.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.confirm_details_fragment.*
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ConfirmDetailsFragment : BaseFragment(R.layout.confirm_details_fragment) {

    private val viewModel: ConfirmDetailsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObserver()

        confirm.setOnClickListener {
            findNavController().navigate(R.id.action_confirmFragment_to_verifyFragment)
        }
    }

    private fun setupObserver() {
        viewModel.deviceData.observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.LOADING -> {
                    content.visibility = View.GONE
                    loading.visibility = View.VISIBLE
                    confirm.isEnabled = false
                }
                Status.SUCCESS -> {
                    content.visibility = View.VISIBLE
                    loading.visibility = View.GONE
                    confirm.isEnabled = true

                    it.data?.let { details ->
                        lga.text = details.lga
                        ward.text = details.ward
                        pollingUnit.text = details.pollingUnit
                        state.text = details.state

                        val calendar = Calendar.getInstance()

                        val dateFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        val timeFormatter = SimpleDateFormat("HH:MM:SS", Locale.getDefault())

                        date.text = dateFormatter.format(calendar.time)
                        time.text = timeFormatter.format(calendar.time)
                    }
                }
                Status.ERROR -> {
                    loading.visibility = View.GONE
                    onError(it.message, it.throwable)
                }
            }
        })

        viewModel.electionData.observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.LOADING -> {
                }
                Status.SUCCESS -> {
                    it.data?.let { data ->
                        val first = data[0]
                        val second = data[1]

                        role1.text = first.title
                        role2.text = second.title
                    }
                }
                Status.ERROR -> {
                }
            }
        })
    }

}