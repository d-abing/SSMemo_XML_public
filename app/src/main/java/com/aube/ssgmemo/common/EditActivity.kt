package com.aube.ssgmemo.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import com.aube.ssgmemo.Memo
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SpinnerModel
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.databinding.ActivityWriteBinding
import com.aube.ssgmemo.etc.MemoStatus
import com.aube.ssgmemo.fragment.MemoDeleteFragment
import io.github.muddz.styleabletoast.StyleableToast

class EditActivity : BaseWriteActivity(), CallbackListener {

    private var memoIdx = "1"
    private var memo = Memo(null, "", "", "", 1111111, 0, 0, 0)
    private var mCtgr: Int = 0
    private var readmode = false
    private var more = false

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
        binding.saveMemo.setOnClickListener { toggleMoreOptions() }
        binding.btnDelete.setOnClickListener {
            deleteFragmentOpen(memo.ctgr)
        }
        binding.btnShare.setOnClickListener { shareMemo() }
        binding.btnCopy.setOnClickListener {
            copyToClipboard(binding.writeContent.text.toString())
            StyleableToast.makeText(this, "클립보드에 복사되었습니다", R.style.toast).show()
        }
        setupWriteContentTouchListener()
    }

    private fun setupWriteContentTouchListener() {
        binding.writeContent.setOnTouchListener(object : View.OnTouchListener {
            var startY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startY = event.y
                        binding.writeContent.parent.requestDisallowInterceptTouchEvent(true)
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val y = event.y
                        if (Math.abs(y - startY) > 10) {
                            binding.writeContent.clearFocus()
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        binding.writeContent.parent.requestDisallowInterceptTouchEvent(false)
                        if (Math.abs(event.y - startY) <= 10) {
                            v.performClick()
                        }
                    }
                }
                return false
            }
        })
    }

    override fun setupUI() {
        applyFontSettings()
        applyDarkMode()
        setupMemo()
        setupCategorySpinner()
        applyAlignment()
        setupFontSizeSpinner()
        setupFontSizeSelection()
        adjustVisibility()
    }

    private fun setupFontSizeSelection() {
        val fontSizeList = if (largeFont == "ON") {
            listOf("26", "28", "30", "32", "34", "36")
        } else {
            listOf("20", "22", "24", "26", "28", "30")
        }

        binding.fontSize.setSelection(
            fontSizeList.indexOf(
                memo.contentAttribute.split(",")[1].toFloat().toInt().toString()
            )
        )
    }

    private fun setupMemo() {
        memoIdx = intent.getStringExtra("memoIdx") ?: "1"
        memo = helper.selectMemo(memoIdx)
        val contentAtt = memo.contentAttribute.split(",")
        val contentGravity = contentAtt[0].toInt()
        val contentSize = contentAtt[1].toFloat()

        binding.btnMode.visibility = View.VISIBLE
        binding.btnDelete.visibility = View.VISIBLE
        binding.saveMemo.setImageResource(R.drawable.baseline_more_vert_24)

        binding.writeTitle.setText(memo.title)
        binding.writeContent.setText(SpannableString.valueOf(Html.fromHtml(memo.content)))
        binding.writeContent.gravity = contentGravity
        binding.writeContent.textSize = contentSize

        setupMemoStatus(memo)
    }

    private fun setupMemoStatus(memo: Memo) {
        val status = memo.status
        if (status == MemoStatus.COMPLETED.code || memo.ctgr == MemoStatus.DELETED.code) {
            disableEditingForCompletedOrDeletedMemo(status)
        } else {
            enableEditing()
        }
    }

    private fun disableEditingForCompletedOrDeletedMemo(status: Int) {
        binding.category.isEnabled = false
        binding.btnMode.setImageResource(R.drawable.baseline_replay_24)
        binding.btnMode.setOnClickListener {
            moveCtgr(memo.idx, mCtgr, 1)
        }
        binding.writeContent.keyListener = null
        binding.writeTitle.isEnabled = false
        if (status == MemoStatus.COMPLETED.code) {
            if (darkmode == 32) {
                binding.writeTitle.setTextColor(Color.WHITE)
            } else {
                binding.writeTitle.setTextColor(Color.BLACK)
            }
        }

    }

    private fun enableEditing() {
        binding.category.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val category = binding.category.getItemAtPosition(position) as SpinnerModel
                mCtgr =
                    if (category.name != "미분류") getKey(helper.selectCtgrMap(), category.name) else 0
            }
        }

        binding.btnMode.setOnClickListener {
            if (readmode) changeToModify() else changeToRead()
        }
    }

    private fun getKey(map: Map<Int, String>, target: String): Int {
        return map.keys.first { target == map[it] }
    }

    private fun toggleMoreOptions() {
        more = !more
        if (more) {
            showMoreOptions()
        } else {
            hideMoreOptions()
        }
    }

    private fun showMoreOptions() {
        animateViewVisibility(binding.moreButton, 0f, View.VISIBLE)
        animateViewVisibility(binding.btnCopy, 0f, View.VISIBLE)
    }

    private fun hideMoreOptions() {
        animateViewVisibility(binding.moreButton, -96f, View.INVISIBLE)
        animateViewVisibility(binding.btnCopy, -96f, View.INVISIBLE)
    }

    private fun animateViewVisibility(view: View, translationY: Float, visibility: Int) {
        ObjectAnimator.ofFloat(view, "translationY", translationY).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    view.visibility = visibility
                }

                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = visibility
                }
            })
            start()
        }
    }

    private fun shareMemo() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                "${binding.writeTitle.text}\n\n${binding.writeContent.text}"
            )
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, null))
    }

    override fun onBackPressed() {
        updateMemoIfChanged()
        super.onBackPressed()
    }

    private fun updateMemoIfChanged() {
        if (memo.status == MemoStatus.NORMAL.code && memo.ctgr != MemoStatus.DELETED.code) {

            var isContentChanged = false
            var isCategoryChanged = false

            val updatedTitle = getUpdatedTitle()
            val updatedContent = getUpdatedContent()
            val updatedContentAttribute = getUpdatedContentAttribute()
            val updatedPriority = getUpdatedPriority()

            if (updatedTitle != memo.title) {
                isContentChanged = true
            }
            if (updatedContent != memo.content) {
                isContentChanged = true
            }
            if (updatedContentAttribute != memo.contentAttribute) {
                isContentChanged = true
            }
            if (mCtgr != memo.ctgr) {
                isCategoryChanged = true
            }

            if (isContentChanged || isCategoryChanged) {
                val updatedMemo = memo.copy(
                    title = updatedTitle,
                    content = updatedContent,
                    contentAttribute = updatedContentAttribute,
                    priority = updatedPriority,
                    datetime = System.currentTimeMillis(),
                    ctgr = mCtgr
                )
                helper.updateMemo(updatedMemo)
            }

            if (isCategoryChanged) {
                helper.sortPriority(memo.ctgr)
            }
        }
    }

    private fun getUpdatedTitle(): String {
        val title = binding.writeTitle.text.toString()
        return if (title.isEmpty()) "빈 제목" else title
    }

    private fun getUpdatedContent(): String {
        return Html.toHtml(binding.writeContent.text)
    }

    private fun getUpdatedContentAttribute(): String {
        val gravity = binding.writeContent.gravity
        val fontSize = fontSize.toFloat()
        return "$gravity,$fontSize"
    }

    private fun getUpdatedPriority(): Int {
        return if (mCtgr != memo.ctgr) {
            helper.checkTopPriority(mCtgr) + 1
        } else {
            memo.priority
        }
    }

    private fun deleteFragmentOpen(ctgrIdx: Int) {
        val deleteFragment = MemoDeleteFragment(this)
        val bundle = Bundle()
        bundle.putInt("ctgrIdx", ctgrIdx)
        deleteFragment.arguments = bundle
        deleteFragment.show(supportFragmentManager, "memoDelete")
    }

    override fun moveCtgr(memoIdx: Int?, ctgrIdx: Int, status: Int) {
        val memo: Memo = helper.selectMemo(memoIdx.toString())
        helper.updateMemoCtgr(memoIdx, memo.ctgr, ctgrIdx)
        helper.updateMemoStatus(memo, status)
        this.finish()
    }

    override fun moveCtgrList(oldCtgrIdx: Int, newCtgrIdx: Int) {
        helper.updateMemoCtgr(memoIdx.toInt(), memo.ctgr, newCtgrIdx)
        this.finish()
    }

    override fun deleteMemoList() {
        val memo: Memo = helper.selectMemo(memoIdx)
        helper.deleteMemo(memo)
        this.finish()
    }

    fun changeToModify() {
        readmode = false
        binding.btnMode.setImageResource(R.drawable.baseline_mode_edit_24)
        binding.writeContent.keyListener = originalKeyListener
    }

    fun changeToRead() {
        readmode = true
        binding.btnMode.setImageResource(R.drawable.baseline_chrome_reader_mode_24)
        originalKeyListener = binding.writeContent.keyListener
        binding.writeContent.clearFocus()
        binding.writeContent.keyListener = null
        softkeyboardHide()
    }
}
