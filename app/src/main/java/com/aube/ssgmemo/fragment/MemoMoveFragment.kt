package com.aube.ssgmemo.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import androidx.fragment.app.DialogFragment
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SpinnerModel
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.adapter.SpinnerAdapter
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.databinding.FragmentMemoMoveBinding
import com.aube.ssgmemo.etc.MemoStatus
import com.aube.ssgmemo.etc.MyApplication

class MemoMoveFragment(var listener: CallbackListener) : DialogFragment() {
    private lateinit var binding: FragmentMemoMoveBinding
    var helper: SqliteHelper? = null
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
        binding = FragmentMemoMoveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle: Bundle? = arguments
        val ctgrIdx: Int? = bundle?.getInt("ctgrIdx")

        if (darkmode == 32) {
            binding.memoMoveFLayout.setBackgroundResource(R.drawable.fragment_decoration2)
        }

        if (ctgrIdx == MemoStatus.DELETED.code) {
            binding.moveMemoMsg.text = "복원할 카테고리를 선택해 주세요"
        }

        binding.dialogMemoMoveNo.setOnClickListener {
            dismiss()
        }

        fun <K, V> getKey(map: Map<K, V>, target: V): K {
            return map.keys.first { target == map[it] }
        }

        var ctgr = 0
        val ctgrList = ArrayList<SpinnerModel>()
        ctgrList.add(0, SpinnerModel(R.drawable.closed_box, "미분류"))
        for (i in helper!!.selectCtgrMap().values.toMutableList()) {
            val spinnerModel = SpinnerModel(R.drawable.closed_box, i)
            ctgrList.add(spinnerModel)
        }


        var currentIndex = 0
        for (i in ctgrList) {
            var currentCtgrName =
                if (helper!!.selectCtgrName(ctgrIdx.toString()) != null) helper!!.selectCtgrName(
                    ctgrIdx.toString()
                ) else {
                    "미분류"
                }
            if (i.name.equals(currentCtgrName)) {
                currentIndex = ctgrList.indexOf(i)
            }
        }

        ctgrList.removeAt(currentIndex)
        if (ctgrIdx == MemoStatus.DELETED.code) {
            ctgrList.add(0, SpinnerModel(R.drawable.closed_box, "미분류"))
        }

        if (ctgrList.isNotEmpty()) {
            binding.category2.adapter =
                SpinnerAdapter(requireContext(), R.layout.item_category_spinner, ctgrList)
        } else {
            binding.moveMemoMsg.text = "이동할 카테고리가 없습니다"
            binding.category2.visibility = View.GONE
        }

        binding.category2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val category = binding.category2.getItemAtPosition(position) as SpinnerModel
                if (category.name != "미분류") {
                    ctgr = getKey(helper!!.selectCtgrMap(), category.name)
                } else {
                    ctgr = 0
                }
            }
        }

        binding.dialogMemoMoveYes.setOnClickListener {
            listener.moveCtgrList(ctgrIdx!!, ctgr)
            dismiss()
        }
    }
}