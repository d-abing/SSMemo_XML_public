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
import com.aube.ssgmemo.databinding.FragmentMemoDeleteBinding
import com.aube.ssgmemo.etc.MyApplication

class MemoDeleteFragment(var listener: CallbackListener) : DialogFragment() {
    private lateinit var binding: FragmentMemoDeleteBinding
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
        binding = FragmentMemoDeleteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle:Bundle? = arguments
        val memoIdx: String? = bundle?.getString("memoIdx") // 해당 메모의 idx 리스트라면 = 첫번째 idx 값만을 가져옴
        val ctgrIdx:String? = bundle?.getString("ctgrIdx") // 해당 메모의 ctgr 미분류 = 0
        val isList: Boolean? = bundle?.getBoolean("isList") // 리스트인지 아닌지.

        if (darkmode == 32 ) {
            binding.memoDeleteFLayout.setBackgroundResource(R.drawable.fragment_decoration2)
        }

        if(ctgrIdx!!.toInt() == -1) {
            binding.deleteMemoMsg.text = "휴지통 메모는 삭제되면 복원할 수 없습니다"
        }

        binding.dialogMemoDeleteNo.setOnClickListener {
            dismiss()
        }

        binding.dialogMemoDeleteYes.setOnClickListener {
            if (ctgrIdx.toInt() == -1 ) {
                if (isList!!) {
                    // 선택된 메모가 리스트라면 리스트 전체 삭제
                    listener.deleteMemoList()
                    dismiss()

                } else {
                    // 리스트가 아니라면 해당 메모만 삭제
                    listener.deleteMemo(memoIdx!!)
                    dismiss()
                }
            } else {
                if (isList!!) {
                    // 선택된 메모가 리스트라면 리스트 전체 삭제
                    listener.moveCtgrList(ctgrIdx.toInt(), -1)
                    dismiss()

                } else {
                    // 리스트가 아니라면 해당 메모만 삭제
                    listener.moveCtgr(memoIdx!!.toInt(), -1, 1)
                    dismiss()
                }
            }
        }
    }
}