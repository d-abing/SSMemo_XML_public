package com.aube.ssgmemo.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.aube.ssgmemo.R
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.databinding.FragmentCtgrDeleteBinding
import com.aube.ssgmemo.etc.MyApplication
import io.github.muddz.styleabletoast.StyleableToast

class CtgrDeleteFragment (var listener:CallbackListener) : DialogFragment() {
    private lateinit var binding: FragmentCtgrDeleteBinding
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
        binding = FragmentCtgrDeleteBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle: Bundle? = arguments
        val ctgrIdx: String? = bundle?.getString("ctgrIdx")
        var ctgrSelected = false // ctgr만 지울 것인가
        var memoSelected = false // 내부의 메모도 함께 지울 것 인가.

        if (darkmode == 32 ) {
            binding.ctgrDeleteFLayout.setBackgroundResource(R.drawable.fragment_decoration2)
            binding.deleteOnlyCtgr.setTextColor(Color.parseColor("#FFFFFF"))
            binding.deleteAlsoMemo.setTextColor(Color.parseColor("#FFFFFF"))
        }

        binding.deleteOnlyCtgr.setOnClickListener {
            ctgrSelected = if (ctgrSelected) {
                if (darkmode == 32 ) {
                    binding.deleteOnlyCtgr.setTextColor(Color.parseColor("#FFFFFF"))
                } else {
                    binding.deleteOnlyCtgr.setTextColor(Color.parseColor("#BDBBBB"))
                }
                false
            } else {
                binding.deleteOnlyCtgr.setTextColor(Color.parseColor("#EEC18A"))
                if (darkmode == 32 ) {
                    binding.deleteAlsoMemo.setTextColor(Color.parseColor("#FFFFFF"))
                } else {
                    binding.deleteAlsoMemo.setTextColor(Color.parseColor("#BDBBBB"))
                }
                memoSelected = false
                true
            }
        }

        binding.deleteAlsoMemo.setOnClickListener {
            memoSelected = if (memoSelected) {
                if (darkmode == 32 ) {
                    binding.deleteAlsoMemo.setTextColor(Color.parseColor("#FFFFFF"))
                } else {
                    binding.deleteAlsoMemo.setTextColor(Color.parseColor("#BDBBBB"))
                }
                false
            } else {
                binding.deleteAlsoMemo.setTextColor(Color.parseColor("#EEC18A"))
                if (darkmode == 32 ) {
                    binding.deleteOnlyCtgr.setTextColor(Color.parseColor("#FFFFFF"))
                } else {
                    binding.deleteOnlyCtgr.setTextColor(Color.parseColor("#BDBBBB"))
                }
                ctgrSelected = false
                true
            }
        }

        binding.dialogDeleteNo.setOnClickListener {
            dismiss()
        }

        binding.dialogDeleteYes.setOnClickListener {
            if (ctgrSelected || memoSelected) {
                if (ctgrSelected) {
                    listener.deleteCtgr(ctgrIdx!!)
                } else if (memoSelected) {
                    listener.moveCtgrList(ctgrIdx!!.toInt(), -1)
                }
                dismiss()
            }
        }
    }
}
