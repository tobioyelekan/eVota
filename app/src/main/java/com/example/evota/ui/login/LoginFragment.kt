package com.example.evota.ui.login

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.evota.R

class LoginFragment : Fragment() {

//    companion object {
//        fun newInstance() = LoginFragment()
//    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.login_fragment, container, false)
    }

}