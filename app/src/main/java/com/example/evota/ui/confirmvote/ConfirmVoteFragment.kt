package com.example.evota.ui.confirmvote

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.example.evota.R
import com.example.evota.ui.BaseFragment
import com.example.evota.util.loadImage
import kotlinx.android.synthetic.main.confirm_vote_fragment.*

class ConfirmVoteFragment : BaseFragment(R.layout.confirm_vote_fragment) {

    private lateinit var viewModel: ConfirmVoteViewModel

    private val args: ConfirmVoteFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val (candidate1, candidate2) = args

        role1.text = candidate1.electionTitle
        partyLogo.loadImage(candidate1.party.logo)
        partyBrief.text = candidate1.party.code
        partyName.text = candidate1.party.name
        candidateImg.loadImage(candidate1.img)
        candidateName.text = candidate1.name

        role2.text = candidate2.electionTitle
        partyLogo2.loadImage(candidate2.party.logo)
        partyBrief2.text = candidate2.party.code
        partyName2.text = candidate2.party.name
        candidateImg2.loadImage(candidate2.img)
        candidateName2.text = candidate2.name

    }

}