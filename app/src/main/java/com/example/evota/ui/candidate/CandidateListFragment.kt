package com.example.evota.ui.candidate

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.evota.R

class CandidateListFragment : Fragment() {

    companion object {
        fun newInstance() = CandidateListFragment()
    }

    private lateinit var viewModel: CandidateListViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.candidate_list_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(CandidateListViewModel::class.java)
        // TODO: Use the ViewModel
    }

}