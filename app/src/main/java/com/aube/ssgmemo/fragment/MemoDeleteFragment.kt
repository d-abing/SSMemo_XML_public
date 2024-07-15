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
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.databinding.FragmentMemoDeleteBinding
import com.aube.ssgmemo.etc.MemoStatus
import com.aube.ssgmemo.etc.MyApplication

class MemoDeleteFragment(var listener: CallbackListener) : DialogFragment() {
    private lateinit var binding: FragmentMemoDeleteBinding
    private var darkmode = MyApplication.prefs.getInt("darkmode", 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        binding = FragmentMemoDeleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle: Bundle? = arguments
        val ctgrIdx: Int? = bundle?.getInt("ctgrIdx")

        if (darkmode == 32) {
            binding.memoDeleteFLayout.setBackgroundResource(R.drawable.fragment_decoration2)
        }

        if (ctgrIdx == MemoStatus.DELETED.code) {
            binding.deleteMemoMsg.text = "휴지통 메모는 삭제되면\n복원할 수 없습니다"
        }

        binding.dialogMemoDeleteNo.setOnClickListener {
            dismiss()
        }

        binding.dialogMemoDeleteYes.setOnClickListener {
            if (ctgrIdx == MemoStatus.DELETED.code) {
                listener.deleteMemoList()
                dismiss()
            } else {
                listener.moveCtgrList(ctgrIdx!!, MemoStatus.DELETED.code)
                dismiss()
            }
        }
    }
}