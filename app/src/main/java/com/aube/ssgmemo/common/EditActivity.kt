package com.aube.ssgmemo.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.KeyListener
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.aube.ssgmemo.Memo
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SpinnerModel
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.adapter.SpinnerAdapter
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.databinding.ActivityWriteBinding
import com.aube.ssgmemo.etc.MyApplication
import com.aube.ssgmemo.fragment.MemoDeleteFragment
import io.github.muddz.styleabletoast.StyleableToast

class EditActivity : AppCompatActivity(), CallbackListener {
    private lateinit var binding: ActivityWriteBinding
    private val helper = SqliteHelper(this, "ssgMemo", 1)

    private var fontSize = MyApplication.prefs.getString("fontSize", "")
    private var darkmode = MyApplication.prefs.getString("darkmode", "0")
    private var memofont = MyApplication.prefs.getString("memofont", "")

    // font Style
    private var isBold = false
    private var isItalic = false
    private var isUnderline = false

    // edit용 변수
    private var memoIdx = "1"
    private var memo = Memo(null, "", "", "", 1111111, 0, 0, 0)
    private var mCtgr: Int = 0
    private var readmode = false
    private var more = false
    private var textFontSize = 20


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 폰트 설정
        if (!memofont.equals("")) {
            val typeface = Typeface.createFromAsset(assets, "font/" + memofont + ".ttf")
            binding.writeTitle.typeface = typeface
            binding.writeContent.typeface = typeface
        }

        // 다크모드 설정
        if (darkmode.equals("32")) {
            binding.writeLayout.setBackgroundColor(Color.BLACK)
            binding.fontBar.setBackgroundColor(Color.DKGRAY)
            binding.adView.setBackgroundColor(Color.BLACK)
            binding.category.setBackgroundResource(R.drawable.bg_spinner2)
        }

        // edit용 설정 및 메모 정보 불러오기
        memoIdx = intent.getStringExtra("memoIdx") as String
        memo = helper.selectMemo(memoIdx)
        val contentAtt = memo.contentAttribute.split(",")
        val contentGravity = contentAtt[0].toInt()
        val contentSize = contentAtt[1].toFloat()
        val ctgr = memo.ctgr
        val status = memo.status

        binding.btnMode.visibility = View.VISIBLE
        binding.btnDelete.visibility = View.VISIBLE
        binding.saveMemo.setImageResource(R.drawable.more)
        binding.adView.visibility = View.VISIBLE
        binding.fontBar.visibility = View.GONE

        binding.writeTitle.setText(memo.title)
        binding.writeContent.setText(SpannableString.valueOf(Html.fromHtml(memo.content)))
        binding.writeContent.gravity = contentGravity
        binding.writeContent.textSize = contentSize

        // < 카테고리 >
        // ctgrList
        val ctgrList = ArrayList<SpinnerModel>()
        val ctgrMap = helper.selectCtgrMap()
        ctgrList.add(0, SpinnerModel(R.drawable.closed_box, "미분류"))
        for (i in ctgrMap.values.toMutableList()) {
            val spinnerModel = SpinnerModel(R.drawable.closed_box, i)
            ctgrList.add(spinnerModel)
        }

        // category spinner
        binding.category.adapter = SpinnerAdapter(this, R.layout.item_spinner, ctgrList)
        var selectedIndexCtgr = 0
        for (i in ctgrList) {
            if (i.name == ctgrMap[ctgr]) {
                selectedIndexCtgr = ctgrList.indexOf(i)
            }
        }
        binding.category.setSelection(selectedIndexCtgr)

        // < 삭제 >
        binding.btnDelete.setOnClickListener {
            fragmentOpen(memo.ctgr.toString(), memo.idx.toString(), false)
        }

        // < 더보기 >
        binding.saveMemo.setOnClickListener {
            if (more) {
                more = false
                ObjectAnimator.ofFloat(binding.btnCopy, "translationY", -96f).apply {
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            // 애니메이션이 시작될 때 호출되는 콜백 메서드
                            binding.btnCopy.visibility = View.INVISIBLE
                        }
                    })
                    start()
                }
                ObjectAnimator.ofFloat(binding.moreButton, "translationY", -96f).apply {
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            // 애니메이션이 종료될 때 호출되는 콜백 메서드
                            binding.moreButton.visibility = View.INVISIBLE
                        }
                    })
                    start()
                }

            } else {
                more = true
                ObjectAnimator.ofFloat(binding.moreButton, "translationY", 0f).apply {
                    addListener(object : AnimatorListenerAdapter() {

                        override fun onAnimationStart(animation: Animator) {
                            // 애니메이션이 시작될 때 호출되는 콜백 메서드
                            binding.moreButton.visibility = View.VISIBLE
                        }
                    })
                    start()
                }
                ObjectAnimator.ofFloat(binding.btnCopy, "translationY", 0f).apply {
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            // 애니메이션이 시작될 때 호출되는 콜백 메서드
                            binding.btnCopy.visibility = View.VISIBLE
                        }
                    })
                    start()
                }
            }
        }

        // < 공유 >
        binding.btnShare.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(
                    Intent.EXTRA_TEXT,
                    "${binding.writeTitle.text.toString() + "\n\n" + binding.writeContent.text.toString()}"
                )
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        // < 복사 >
        binding.btnCopy.setOnClickListener {
            copyToClipboard(binding.writeContent.text.toString())
            StyleableToast.makeText(this, "클립보드에 복사되었습니다", R.style.toast).show()
        }

        // dpi 값에 맞게 layoutParams 조절
        val dpi = resources.displayMetrics.densityDpi

        val rect = Rect()
        binding.writeLayout.getWindowVisibleDisplayFrame(rect)
        dpiLayoutParams(binding.writeTitle, (rect.width() * 0.9).toInt())
        dpiLayoutParams(binding.category, (rect.width() * 0.4).toInt())

        if (status == 2 || memo.ctgr == -1) { // < 완료 >, < 휴지통 >
            binding.category.isEnabled = false
            binding.btnMode.setImageResource(R.drawable.reset)
            binding.btnMode.setOnClickListener {
                moveCtgr(memo.idx, mCtgr, 1)
            }

            binding.writeTitle.isEnabled = false
            binding.writeContent.keyListener = null

            val layoutParams = binding.writeContent.layoutParams
            layoutParams.height =
                rect.bottom - binding.writeContent.y.toInt() - (0.8 * dpi + 165).toInt()
            layoutParams.width = (rect.width() * 0.9).toInt()
            binding.writeContent.layoutParams = layoutParams

            if (status == 2) {
                binding.writeTitle.setTextColor(Color.BLACK)
            }
        } else { // <일반 >
            binding.category.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val category = binding.category.getItemAtPosition(position) as SpinnerModel
                    if (category.name != "미분류") {
                        mCtgr = getKey(helper.selectCtgrMap(), category.name)
                    } else {
                        mCtgr = 0
                    }
                }

                fun <K, V> getKey(map: Map<K, V>, target: V): K {
                    return map.keys.first { target == map[it] }
                }
            }

            binding.btnMode.setOnClickListener {
                if (readmode) {
                    changeToModify()
                } else {
                    changeToRead()
                }
            }

            // < 폰트 사이즈 >
            val fontSizeList: List<String>
            var selectedIndexFontSize = 0

            // 설정 반영
            if (fontSize.equals("ON")) {
                fontSizeList = listOf("26", "28", "30", "32", "34", "36")
            } else {
                fontSizeList = listOf("20", "22", "24", "26", "28", "30")
            }

            // fontSize spinner
            binding.fontSize.adapter =
                ArrayAdapter(this, R.layout.spinner_layout, fontSizeList.toMutableList())
            binding.fontSize.setSelection(fontSizeList.indexOf(contentSize.toInt().toString()))
            binding.fontSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    changeFontSize(binding.fontSize.getItemAtPosition(position).toString())
                    textFontSize = binding.fontSize.getItemAtPosition(position).toString().toInt()
                    selectedIndexFontSize = position
                }
            }

            // fontSize spinner 옆 사이즈 조절 버튼
            binding.sizedown.setOnClickListener { // 사이즈 다운
                if (selectedIndexFontSize != 0) {
                    binding.fontSize.setSelection(selectedIndexFontSize - 1)
                }
            }

            binding.sizeup.setOnClickListener { // 사이즈 업
                if (selectedIndexFontSize != fontSizeList.size - 1) {
                    binding.fontSize.setSelection(selectedIndexFontSize + 1)
                }
            }

            // writeContent의 크기를 유동적으로 설정
            var layoutHeight = 0
            binding.writeLayout.viewTreeObserver.addOnGlobalLayoutListener {
                // 현재 레이아웃의 크기
                val rect = Rect()
                binding.writeLayout.getWindowVisibleDisplayFrame(rect)

                layoutHeight = if (layoutHeight < rect.bottom) rect.bottom else layoutHeight

                // EditText의 크기 조정
                val layoutParams = binding.writeContent.layoutParams
                layoutParams.height =
                    rect.bottom - binding.writeContent.y.toInt() - (0.25 * dpi + 165).toInt()
                layoutParams.width = (rect.width() * 0.9).toInt()
                binding.writeContent.layoutParams = layoutParams

                if (layoutHeight <= rect.bottom) {
                    binding.adView.visibility = View.VISIBLE
                    binding.fontBar.visibility = View.GONE
                } else {
                    binding.adView.visibility = View.GONE
                    binding.fontBar.visibility = View.VISIBLE
                }
            }

            // writeContent의 텍스트가 변경될 때마다 스크롤 변경
            binding.writeContent.doOnTextChanged { _, _, _, _ ->
                val cursorPosition = binding.writeContent.selectionStart
                val cursorLineIndex = binding.writeContent.layout.getLineForOffset(cursorPosition)
                val lastLineIndex =
                    binding.writeContent.layout.getLineForOffset(binding.writeContent.length())
                val fontHeight = (textFontSize * 4)
                val maxline = (binding.writeContent.height - 10) / fontHeight

                Log.d("test다", "$textFontSize, ${binding.writeContent.scrollY}")

                when (textFontSize) {
                    20 -> scrollChange(cursorLineIndex, lastLineIndex, maxline, 86, 82)
                    22 -> scrollChange(cursorLineIndex, lastLineIndex, maxline, 60, 88)
                    24 -> scrollChange(cursorLineIndex, lastLineIndex, maxline, 116, 94)
                    26 -> scrollChange(cursorLineIndex, lastLineIndex, maxline, 85, 102)
                    28 -> scrollChange(cursorLineIndex, lastLineIndex, maxline, 135, 108)
                    30 -> scrollChange(cursorLineIndex, lastLineIndex, maxline, 70, 114)
                    32 -> scrollChange(cursorLineIndex, lastLineIndex, maxline, 125, 122)
                    34 -> scrollChange(cursorLineIndex, lastLineIndex, maxline, 169, 128)
                    36 -> scrollChange(cursorLineIndex, lastLineIndex, maxline, 130, 136)
                }
            }

            // writeContent의 텍스트가 변경될 때마다 스타일 적용
            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val cursorPosition = binding.writeContent.selectionStart
                    if (isBold) {
                        s?.setSpan(
                            StyleSpan(Typeface.BOLD),
                            cursorPosition - 1,
                            cursorPosition,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    if (isItalic) {
                        s?.setSpan(
                            StyleSpan(Typeface.ITALIC),
                            cursorPosition - 1,
                            cursorPosition,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                    if (isUnderline) {
                        s?.setSpan(
                            UnderlineSpan(),
                            cursorPosition - 1,
                            cursorPosition,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
            }
            binding.writeContent.addTextChangedListener(textWatcher)

            // 텍스트를 지울 때 span 에러가 발생하지 않도록 처리
            binding.writeContent.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    isBold = false
                    isItalic = false
                    isUnderline = false
                    return@setOnKeyListener false
                }
                false
            }

            // font Style 버튼 클릭 이벤트
            binding.bold.setOnClickListener {
                fontStyleChange("bold")
            }

            binding.italic.setOnClickListener {
                fontStyleChange("italic")
            }

            binding.underline.setOnClickListener {
                fontStyleChange("underline")
            }

            binding.leftAlign.setOnClickListener {
                binding.writeContent.gravity = Gravity.LEFT
            }

            binding.centerAlign.setOnClickListener {
                binding.writeContent.gravity = Gravity.CENTER_HORIZONTAL
            }

            binding.rightAlign.setOnClickListener {
                binding.writeContent.gravity = Gravity.RIGHT
            }
        }
    }

    fun dpiLayoutParams(view: View, width: Int) {
        val tmp_layoutParams = view.layoutParams
        tmp_layoutParams.width = width
        view.layoutParams = tmp_layoutParams
    }

    fun Context.copyToClipboard(text: String) { // 클립보드에 복사
        val clipboardManager =
            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("label", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (memo.ctgr != -1) {
            var checkdiff = false
            var checkdiffC = false

            var mTitle = memo.title
            var mContent = memo.content
            val mAttribute = memo.contentAttribute.split(",")
            val mGravity = mAttribute.get(0).toInt()
            val mFontSize = mAttribute.get(1).toFloat()
            var mPriority = memo.priority

            val gravity = binding.writeContent.gravity
            val fontsize = textFontSize.toFloat()
            val mContentAttribute = gravity.toString() + "," + fontsize.toString()

            if (binding.writeTitle.text.toString() != mTitle) {
                mTitle = if (binding.writeTitle.text.toString() == "") {
                    "빈 제목"
                } else {
                    binding.writeTitle.text.toString()
                }
                checkdiff = true
            }
            if (Html.toHtml(binding.writeContent.text) != mContent) {
                mContent = Html.toHtml(binding.writeContent.text)
                checkdiff = true
            }
            // 카테고리가 변경되었을 때 우선순위 +1 부여
            if (mCtgr != memo.ctgr) {
                mPriority = helper.checkTopPriority(mCtgr) + 1
                checkdiffC = true
            }
            if (mGravity != gravity || mFontSize != fontsize) {
                checkdiff = true
            }

            // 제목, 내용, 카테고리 하나라도 변경되었으면 db업뎃
            if (checkdiff || checkdiffC) {
                val memo_after = Memo(
                    memo.idx,
                    mTitle,
                    mContent,
                    mContentAttribute,
                    System.currentTimeMillis(),
                    mCtgr,
                    mPriority,
                    memo.status
                )
                helper.updateMemo(memo_after)
            }

            if (checkdiffC) {
                helper.sortPriority(memo.ctgr)
            }
        }
    }

    fun scrollChange(
        cursorLineIndex: Int,
        lastLineIndex: Int,
        maxline: Int,
        base: Int,
        multi: Int
    ) {
        // 글 입력 시 스크롤 변경
        val x = cursorLineIndex - maxline
        val scrollY = base + multi * x

        if (cursorLineIndex > (maxline - 1) && cursorLineIndex == lastLineIndex) {
            val scrollAmount =
                binding.writeContent.layout.getLineBottom(binding.writeContent.lineCount - 1) - binding.writeContent.height + 65
            binding.writeContent.scrollTo(0, scrollAmount)
        } else if (cursorLineIndex > (maxline - 1) && !(binding.writeContent.scrollY >= scrollY && binding.writeContent.scrollY <= scrollY + multi * (maxline - 1))) {
            binding.writeContent.scrollTo(0, scrollY)
        } else if (cursorLineIndex <= (maxline - 1)) {
            binding.writeContent.scrollTo(0, 0)
        }
    }

    fun fontStyleChange(fontKind: String) {
        // B, I, U 폰트 스타일 변경
        val start = binding.writeContent.selectionStart
        val end = binding.writeContent.selectionEnd

        if (start == end) { // 드래그하지 않은 경우
            setNextFontStyle(fontKind)
        } else { // 드래그한 경우
            setSeletedFontStyle(fontKind, start, end)
        }
    }

    fun setNextFontStyle(fontKind: String) {
        // 드래그하지 않은 경우 폰트 스타일
        when (fontKind) {
            "bold" ->
                if (!isBold) {
                    isBold = true
                    setNextSelection()
                } else {
                    isBold = false
                }

            "italic" ->
                if (!isItalic) {
                    isItalic = true
                    setNextSelection()
                } else {
                    isItalic = false
                }

            "underline" ->
                if (!isUnderline) {
                    isUnderline = true
                    setNextSelection()
                } else {
                    isUnderline = false
                }
        }
    }

    fun setSeletedFontStyle(fontKind: String, start: Int, end: Int) {
        // 드래그한 경우 폰트 스타일
        val spans = binding.writeContent.text!!.getSpans(start, end, StyleSpan::class.java)
        val underlines = binding.writeContent.text!!.getSpans(start, end, UnderlineSpan::class.java)

        if (spans.isNotEmpty() && underlines.isNotEmpty()) { // B,I : O / U : O
            for (span in spans) {
                for (underline in underlines) {
                    when (fontKind) {
                        "bold" ->
                            checkStyle(span, start, end, Typeface.BOLD)

                        "italic" ->
                            checkStyle(span, start, end, Typeface.ITALIC)

                        "underline" ->
                            binding.writeContent.text!!.removeSpan(underline)
                    }
                }
            }
        } else if (spans.isNotEmpty()) { // B,I : O / U : X
            for (span in spans) {
                when (fontKind) {
                    "bold" ->
                        checkStyle(span, start, end, Typeface.BOLD)

                    "italic" ->
                        checkStyle(span, start, end, Typeface.ITALIC)

                    "underline" ->
                        binding.writeContent.text!!.setSpan(
                            UnderlineSpan(),
                            start,
                            end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                }
            }
        } else if (underlines.isNotEmpty()) { // B,I : X / U : O
            for (underline in underlines) {
                when (fontKind) {
                    "bold" ->
                        binding.writeContent.text!!.setSpan(
                            StyleSpan(Typeface.BOLD),
                            start,
                            end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                    "italic" ->
                        binding.writeContent.text!!.setSpan(
                            StyleSpan(Typeface.ITALIC),
                            start,
                            end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                    "underline" ->
                        binding.writeContent.text!!.removeSpan(underline)
                }
            }
        } else { // B,I : X / U : X
            when (fontKind) {
                "bold" ->
                    binding.writeContent.text!!.setSpan(
                        StyleSpan(Typeface.BOLD),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                "italic" ->
                    binding.writeContent.text!!.setSpan(
                        StyleSpan(Typeface.ITALIC),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                "underline" ->
                    binding.writeContent.text!!.setSpan(
                        UnderlineSpan(),
                        start,
                        end,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
            }
        }
    }

    fun checkStyle(
        span: StyleSpan,
        start: Int,
        end: Int,
        style: Int
    ) { // 이미 B,I 속성이 설정되어 있을 때 속성 확인 후 bold_italic 적용 여부 선택
        binding.writeContent.text!!.removeSpan(span)
        if (span.style == Typeface.BOLD_ITALIC) { // 현재 스타일이 bold_italic일때
            when (style) {
                Typeface.BOLD -> binding.writeContent.text!!.setSpan(
                    StyleSpan(Typeface.ITALIC),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                Typeface.ITALIC -> binding.writeContent.text!!.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } else if (span.style != style) { // 두 스타일 모두 적용
            binding.writeContent.text!!.setSpan(
                StyleSpan(Typeface.BOLD_ITALIC),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    fun setNextSelection() { // 드래그 하지 않은 경우 폰트 스타일을 바꾸기 위해서 공백문자열을 삽입해야 함
        val cursorPosition = binding.writeContent.selectionStart
        binding.writeContent.text?.insert(cursorPosition, " ")
        binding.writeContent.setSelection(cursorPosition, cursorPosition + 1)
    }

    fun changeFontSize(fontSize: String) {
        val fontSizeInDp = fontSize.toInt() // 원하는 텍스트 크기(dp)
        val scale = resources.displayMetrics.density
        val fontSizeInPixels = (fontSizeInDp * scale + 0.5f).toInt() // dp를 픽셀로 변환
        binding.writeContent.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizeInPixels.toFloat())
    }

    override fun fragmentOpen(
        ctgrIdx: String,
        memoIdx: String,
        isList: Boolean
    ) { // memoDeleteFragment 오픈
        val deleteFragment = MemoDeleteFragment(this)
        val bundle = Bundle()
        bundle.putString("ctgrIdx", ctgrIdx)
        bundle.putString("memoIdx", memoIdx)
        bundle.putBoolean("isList", isList)
        deleteFragment.arguments = bundle
        deleteFragment.show(supportFragmentManager, "memoDelete")
    }

    override fun moveCtgr(memoIdx: Int?, ctgrIdx: Int, status: Int) { // 카테고리 이동
        val memo: Memo = helper.selectMemo(memoIdx.toString())
        helper.updateMemoCtgr(memoIdx, memo.ctgr, ctgrIdx)
        helper.updateMemoStatus(memo, status)
        this.finish()
    }

    override fun deleteMemo(memoIdx: String) {
        val memo: Memo = helper.selectMemo(memoIdx)
        helper.deleteMemo(memo)
        this.finish()
    }

    var originalKeyListener: KeyListener? = null
    fun changeToModify() { // 수정모드로 변경
        readmode = false
        binding.btnMode.setImageResource(R.drawable.read)
        binding.writeContent.keyListener = originalKeyListener
    }

    fun changeToRead() { // 읽기모드로 변경
        readmode = true
        binding.btnMode.setImageResource(R.drawable.modify)
        originalKeyListener = binding.writeContent.keyListener
        binding.writeContent.clearFocus()
        binding.writeContent.keyListener = null
        softkeyboardHide()
    }

    fun softkeyboardHide() { // 키보드 숨기기
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.writeContent.windowToken, 0)
    }
}