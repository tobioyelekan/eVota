package com.example.evota.ui.confirm

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.evota.R

class ConfirmFragment : Fragment() {

//    companion object {
//        fun newInstance() = ConfirmFragment()
//    }

    private lateinit var viewModel: ConfirmViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.confirm_fragment, container, false)
    }

}