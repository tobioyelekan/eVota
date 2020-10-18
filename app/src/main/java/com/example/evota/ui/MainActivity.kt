package com.example.evota.ui

import android.os.Bundle
import android.os.Process
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.Navigation
import com.example.evota.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onBackPressed() {
        this.moveTaskToBack(true)
        this.finish()
        Process.killProcess(Process.myPid())
    }
}