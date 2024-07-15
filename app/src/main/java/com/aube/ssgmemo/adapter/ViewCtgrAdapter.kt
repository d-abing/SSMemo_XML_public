package com.aube.ssgmemo.adapter

import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aube.ssgmemo.Ctgr
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.common.ViewMemoActivity
import com.aube.ssgmemo.databinding.RecyclerViewCtgrBinding
import com.aube.ssgmemo.etc.BackPressEditText
import com.aube.ssgmemo.etc.MyApplication
import io.github.muddz.styleabletoast.StyleableToast

class ViewCtgrAdapter(private val context: Context) :
    RecyclerView.Adapter<ViewCtgrAdapter.ViewHolder>() {

    var listData = mutableListOf<Ctgr>()
    var helper: SqliteHelper? = null

    private var darkmode = MyApplication.prefs.getInt("darkmode", 16)
    private var vibration = MyApplication.prefs.getString("vibration", "OFF")

    var vibrator: Vibrator? = null

    inner class ViewHolder(private val binding: RecyclerViewCtgrBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(ctgr: Ctgr) {

            helper?.let { helper ->

                when (ctgr.name) {
                    "+" -> {
                        binding.memoCount.visibility = View.INVISIBLE
                        binding.ctgrBtn.setBackgroundResource(if (darkmode == 32) R.drawable.ctgrback6 else R.drawable.ctgrback2)
                    }

                    "미분류" -> {
                        binding.memoCount.visibility = View.VISIBLE
                        binding.ctgrBtn.setBackgroundResource(if (darkmode == 32) R.drawable.ctgrback8 else R.drawable.ctgrback4)
                    }

                    "휴지통" -> {
                        binding.memoCount.visibility = View.VISIBLE
                        binding.ctgrBtn.setBackgroundResource(if (darkmode == 32) R.drawable.ctgrback7 else R.drawable.ctgrback3)
                    }

                    else -> {
                        binding.memoCount.visibility = View.VISIBLE
                        binding.ctgrBtn.setBackgroundResource(if (darkmode == 32) R.drawable.ctgrback5 else R.drawable.ctgrback1)
                    }
                }

                binding.ctgrEdit.setText(ctgr.name)
                binding.ctgrName.text = ctgr.name
                binding.deleteBtn.visibility = View.INVISIBLE
                binding.ctgrEdit.visibility = View.INVISIBLE
                binding.ctgrName.visibility = View.VISIBLE
                binding.memoCount.text = helper.checkMemoListSize(ctgr.idx).toString()

                if (ctgr.name != "미분류" && ctgr.name != "+" && ctgr.name != "휴지통") {
                    itemView.setOnLongClickListener {
                        if (vibration == "ON") vibrator?.vibrate(
                            VibrationEffect.createOneShot(
                                200,
                                50
                            )
                        )
                        binding.deleteBtn.visibility = View.VISIBLE
                        binding.ctgrEdit.visibility = View.VISIBLE
                        binding.ctgrName.visibility = View.INVISIBLE
                        binding.ctgrEdit.requestFocus()
                        binding.ctgrEdit.setSelection(binding.ctgrEdit.length())
                        (context as CallbackListener).openKeyBoard(binding.ctgrEdit)
                        true
                    }
                }

                binding.deleteBtn.setOnClickListener {
                    if (helper.checkMemoListSize(ctgr.idx) > 0) {
                        (context as CallbackListener).fragmentOpen(
                            "delete@#",
                            ctgr.idx.toString()
                        )
                    } else {
                        helper.deleteCtgr(ctgr.idx.toString())
                        listData.remove(ctgr)
                        notifyDataSetChanged()
                    }
                    binding.deleteBtn.visibility = View.INVISIBLE
                    binding.ctgrEdit.visibility = View.INVISIBLE
                    binding.ctgrName.visibility = View.VISIBLE
                }

                binding.ctgrEdit.setOnBackPressListener(object :
                    BackPressEditText.OnBackPressListener {
                    override fun onBackPress() {
                        modifyCtgrName(binding.ctgrName, binding.ctgrEdit, ctgr)
                    }
                })

                binding.ctgrEdit.setOnEditorActionListener { _, i, keyEvent ->
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        modifyCtgrName(binding.ctgrName, binding.ctgrEdit, ctgr)
                        (context as CallbackListener).closeKeyBoard()
                        true
                    } else {
                        false
                    }
                }

                if (ctgr.name == "+") {
                    itemView.setOnClickListener {
                        (context as CallbackListener).fragmentOpen(
                            ctgr.name, null
                        )
                        notifyDataSetChanged()
                    }
                } else {
                    itemView.setOnClickListener {
                        val intent = Intent(context, ViewMemoActivity::class.java)
                        intent.putExtra("idx", ctgr.idx)
                        intent.putExtra("ctgrname", ctgr.name)
                        context.startActivity(intent)
                    }
                }
            }
        }

        private fun modifyCtgrName(textView: TextView, editText: BackPressEditText, ctgr: Ctgr) {
            val ctgrName = editText.text.toString().trim()
            if (ctgrName.isNotEmpty() && !listOf(
                    "미분류",
                    "delete@#",
                    "+",
                    "휴지통"
                ).contains(ctgrName)
            ) {
                if (!helper!!.checkDuplicationCtgr(ctgrName)) {
                    helper?.updateCtgrName(ctgr.idx.toString(), editText.text.toString())
                    textView.text = editText.text
                    ctgr.name = editText.text.toString()
                    listData[adapterPosition] = ctgr
                    notifyDataSetChanged()
                } else if (ctgrName == textView.text) {
                    notifyDataSetChanged()
                } else {
                    showToast("이미 사용중 입니다")
                }
            } else {
                showToast("사용할 수 없는 이름입니다")
            }
            binding.ctgrEdit.clearFocus()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            RecyclerViewCtgrBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listData[position])
    }

    override fun getItemCount(): Int = listData.size

    private fun showToast(message: String) {
        StyleableToast.makeText(context, message, R.style.toast).show()
    }
}
