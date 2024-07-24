package com.aube.ssgmemo.common

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import com.aube.ssgmemo.Memo
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SpinnerModel
import com.aube.ssgmemo.databinding.ActivityWriteBinding
import com.aube.ssgmemo.etc.BackKeyHandler
import com.aube.ssgmemo.etc.MemoStatus
import com.aube.ssgmemo.etc.MyApplication

class WriteActivity : BaseWriteActivity() {
    private var backFlag = false
    private val backKeyHandler = BackKeyHandler(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        //initializeAds()
    }

    override fun setupListeners() {
        super.setupListeners()
        binding.saveMemo.setOnClickListener { saveMemo() }
    }

    override fun createTextWatcher() = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            applyTextStyles(s)
            adjustBackFlag()
        }
    }

    private fun adjustBackFlag() {
        backFlag = binding.writeContent.text?.trim().isNullOrEmpty().not()
    }

    private fun saveMemo() {
        if (binding.writeContent.text!!.isNotEmpty()) {
            val mTitle =
                if (binding.writeTitle.text.isEmpty()) "빈 제목" else binding.writeTitle.text.toString()
            removeUnintendedUnderline()

            val mContent = Html.toHtml(binding.writeContent.text)
            val mContentAttribute = "${binding.writeContent.gravity},$fontSize"
            val mDate = System.currentTimeMillis()
            val mCtgr = (binding.category.selectedItem as SpinnerModel).let {
                if (it.name != "미분류") getKey(helper.selectCtgrMap(), it.name) else 0
            }
            val mPriority = helper.checkTopPriority(mCtgr) + 1
            val mStatus = MemoStatus.NORMAL.code

            val memo =
                Memo(null, mTitle, mContent, mContentAttribute, mDate, mCtgr, mPriority, mStatus)
            helper.insertMemo(memo)

            triggerVibration()
            animateSaveButton()
            resetFields()
        }
    }

    private fun removeUnintendedUnderline() {
        if (!isUnderline) {
            binding.writeContent.text?.getSpans(
                binding.writeContent.length() - 1,
                binding.writeContent.length(),
                UnderlineSpan::class.java
            )?.forEach {
                binding.writeContent.text?.removeSpan(it)
            }
        }
    }

    private fun <K, V> getKey(map: Map<K, V>, target: V): K {
        return map.keys.first { target == map[it] }
    }

    private fun triggerVibration() {
        if (vibration == "ON") {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(200, 50))
        }
    }


    private fun animateSaveButton() {
        binding.saveMemo.setImageResource(R.drawable.save2)
        android.os.Handler()
            .postDelayed({ binding.saveMemo.setImageResource(R.drawable.save1) }, 200)
    }

    private fun resetFields() {
        isBold = false
        isItalic = false
        isUnderline = false
        binding.writeTitle.setText("")
        binding.writeContent.setText("")
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (backFlag) {
            backKeyHandler.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }

    override fun onStop() {
        super.onStop()
        MyApplication.prefs.setInt("fontSize", fontSize)
    }
}
