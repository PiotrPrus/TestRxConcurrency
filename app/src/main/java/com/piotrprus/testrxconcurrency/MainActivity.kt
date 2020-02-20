package com.piotrprus.testrxconcurrency

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.piotrprus.testrxconcurrency.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        val viewModel: MainViewModel by viewModels()
        binding.viewModel = viewModel
        viewModel.tasksInProgress.observe(this, Observer {
            binding.progressBar.visibility = if(it) View.VISIBLE else View.INVISIBLE
        })
    }
}
