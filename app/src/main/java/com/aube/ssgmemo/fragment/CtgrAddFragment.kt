package com.aube.ssgmemo.fragment

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import com.aube.ssgmemo.R
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.databinding.FragmentCtgrAddBinding
import com.aube.ssgmemo.etc.MyApplication

class CtgrAddFragment(val listener: CallbackListener) : DialogFragment(){
    private lateinit var binding: FragmentCtgrAddBinding
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
        binding = FragmentCtgrAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (darkmode == 32 ) {
            binding.ctgrAddFLayout.setBackgroundResource(R.drawable.fragment_decoration2)
        }

        binding.ctgrname.postDelayed({
            binding.ctgrname.requestFocus()
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.ctgrname, InputMethodManager.SHOW_IMPLICIT)
        }, 100)

        binding.dialogTvNo.setOnClickListener {
            dismiss()
        }
        binding.dialogTvYes.setOnClickListener {
            if (binding.ctgrname.text.trim().isNotEmpty()) {
                listener.addCtgr(binding.ctgrname.text.toString())
                dismiss()
            }
        }
    }

}