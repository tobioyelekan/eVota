package com.example.evota.ui.printout

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.evota.R

class PrintOut : Fragment() {

    companion object {
        fun newInstance() = PrintOut()
    }

    private lateinit var viewModel: PrintOutViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.print_out_fragment, container, false)
    }

}