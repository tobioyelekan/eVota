package com.example.evota.ui.candidate

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.evota.R
import com.example.evota.data.helpers.Status
import com.example.evota.ui.BaseFragment
import com.example.evota.util.EventObserver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.candidate_list_fragment.*

@AndroidEntryPoint
class CandidateListFragment : BaseFragment(R.layout.candidate_list_fragment) {

    private val viewModel: CandidateListViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        candidates1.adapter = CandidateAdapter(viewModel)
        candidates2.adapter = CandidateAdapter(viewModel)
        setupObserver()

        confirm.setOnClickListener {
            viewModel.getSelectedCandidates()
        }
    }

    private fun setupObserver() {
        viewModel.candidates.observe(viewLifecycleOwner, Observer {
            when (it.status) {
                Status.LOADING -> {
                    loading1.visibility = View.VISIBLE
                    loading2.visibility = View.VISIBLE
                    candidates1.visibility = View.GONE
                    candidates2.visibility = View.GONE
                }
                Status.SUCCESS -> {
                    loading1.visibility = View.GONE
                    loading2.visibility = View.GONE
                    candidates1.visibility = View.VISIBLE
                    candidates2.visibility = View.VISIBLE

                    it.data?.let { candidatesData ->
                        val candidatesData1 = candidatesData[0]
                        val candidatesData2 = candidatesData[1]

                        title1.text = candidatesData1[0].electionTitle
                        title2.text = candidatesData2[1].electionTitle

                        (candidates1.adapter as CandidateAdapter).submitList(candidatesData1)
                        (candidates2.adapter as CandidateAdapter).submitList(candidatesData2)
                    }
                }
                Status.ERROR -> {
                    loading1.visibility = View.GONE
                    loading2.visibility = View.GONE
                    candidates1.visibility = View.VISIBLE
                    candidates2.visibility = View.VISIBLE
                }
            }
        })

        viewModel.message.observe(viewLifecycleOwner, EventObserver {
            showMessage(it)
        })

        viewModel.navigate.observe(viewLifecycleOwner, EventObserver {
            val action = CandidateListFragmentDirections
                .actionCandidateListFragmentToConfirmVoteFragment(it.first, it.second)
            findNavController().navigate(action)

        })
    }

    override fun onStop() {
        viewModel.clear()
        super.onStop()
    }
}