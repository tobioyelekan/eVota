package com.example.evota.ui.printout

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.evota.R
import com.example.evota.data.sharedpreference.Preferences
import com.example.evota.ui.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.print_out_fragment.*
import javax.inject.Inject

@AndroidEntryPoint
class PrintOut : BaseFragment(R.layout.print_out_fragment) {

    private lateinit var viewModel: PrintOutViewModel

    @Inject
    lateinit var preferences: Preferences

    private val args: PrintOutArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        voterName.text = args.voterName
        voterId.text = args.voteId
        dateVoted.text = args.dateVoted
        timeVoted.text = args.timeVoted.substringBeforeLast(":")
        pollingUnit.text = preferences.getPollingUnit()

        print.setOnClickListener {
            findNavController().navigate(R.id.action_printOut_to_confirmFragment)
        }
    }
}