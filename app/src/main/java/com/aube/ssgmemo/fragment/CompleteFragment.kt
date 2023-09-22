package com.aube.ssgmemo.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.databinding.FragmentCompleteBinding
import com.aube.ssgmemo.etc.MyApplication

class CompleteFragment(var listener: CallbackListener) : DialogFragment() {
    private lateinit var binding: FragmentCompleteBinding
    private var darkmode = MyApplication.prefs.getString("darkmode", "0").toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        binding = FragmentCompleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle: Bundle? = arguments
        val memoIdx: Int? = bundle?.getInt("memoIdx")

        if (darkmode == 32 ) {
            binding.completeFLayout.setBackgroundResource(R.drawable.fragment_decoration2)
        }

        binding.dialogMemoCompleteNo.setOnClickListener {
            dismiss()
        }

        binding.dialogMemoCompleteYes.setOnClickListener {
            listener.completeMemo(memoIdx!!)
            dismiss()
        }
    }
}