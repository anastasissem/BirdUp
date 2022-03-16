package com.example.birdup.ui.feedback

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.birdup.databinding.FragmentFeedbackBinding

class FeedbackFragment : Fragment() {

    private lateinit var feedbackViewModel: FeedbackViewModel
    private var _binding: FragmentFeedbackBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        feedbackViewModel =
            ViewModelProvider(this).get(FeedbackViewModel::class.java)

        _binding = FragmentFeedbackBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textFeedback
        feedbackViewModel.text.observe(viewLifecycleOwner, {
            textView.text = it
        })
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}