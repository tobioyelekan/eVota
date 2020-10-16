package com.example.evota.ui.confirmvote

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.evota.R

class ConfirmVoteFragment : Fragment() {

    companion object {
        fun newInstance() = ConfirmVoteFragment()
    }

    private lateinit var viewModel: ConfirmVoteViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.confirm_vote_fragment, container, false)
    }

}