package com.example.evota.ui.candidate

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.evota.R
import com.example.evota.data.model.Candidate
import com.example.evota.util.loadImage
import kotlinx.android.synthetic.main.candidate_item.view.*
import kotlinx.android.synthetic.main.candidate_item.view.candidateImg
import kotlinx.android.synthetic.main.candidate_item.view.partyBrief
import kotlinx.android.synthetic.main.candidate_item.view.partyLogo
import kotlinx.android.synthetic.main.candidate_item.view.partyName

class CandidateAdapter(private val viewModel: CandidateListViewModel) :
    ListAdapter<Candidate, CandidateAdapter.ViewHolder>(CandidateDiffCallback()) {

    var lastCheckedButton: CheckBox? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(inflater.inflate(R.layout.candidate_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(viewModel, getItem(position))

        with(holder.itemView) {
            checkCandidate.setOnCheckedChangeListener { compoundButton, select ->
                val checkedRb = compoundButton?.findViewById<CheckBox>(compoundButton.id)

                if (lastCheckedButton != null) {
                    lastCheckedButton!!.isChecked = false
                }

                if (select) {
                    lastCheckedButton = checkedRb
                    viewModel.candidateSelected(position, getItem(position).electionId)
                } else {
                    if (lastCheckedButton == checkedRb) lastCheckedButton = null
                    viewModel.candidateUnSelected(getItem(position).electionId)
                }
            }

            setOnClickListener {
                checkCandidate.isChecked = !checkCandidate.isChecked
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(viewModel: CandidateListViewModel, candidate: Candidate) {
            with(itemView) {
                partyLogo.loadImage(candidate.party.logo)
                partyBrief.text = candidate.party.code
                partyName.text = candidate.party.name
                candidateImg.loadImage(candidate.img)
                val fullname = candidate.name.split(" ")
                candidateFirstName.text = fullname[0]
                candidateLastName.text = fullname[1]
            }
        }
    }
}

class CandidateDiffCallback : DiffUtil.ItemCallback<Candidate>() {
    override fun areItemsTheSame(oldItem: Candidate, newItem: Candidate): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Candidate, newItem: Candidate): Boolean {
        return oldItem == newItem
    }
}