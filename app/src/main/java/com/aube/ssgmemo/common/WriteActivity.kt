package com.aube.ssgmemo.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.Html
import android.text.Spannable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
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
import com.aube.ssgmemo.etc.BackKeyHandler
import com.aube.ssgmemo.etc.MyApplication


class WriteActivity : AppCompatActivity(), CallbackListener {
    private lateinit var binding: ActivityWriteBinding
    private val helper = SqliteHelper(this, "ssgMemo", 1)
    private val backKeyHandler = BackKeyHandler(this)

    private var vibration = MyApplication.prefs.getString("vibration", "")
    private var fontSize = MyApplication.prefs.getString("fontSize", "")
    private var darkmode = MyApplication.prefs.getString("darkmode", "0")
    private var memofont = MyApplication.prefs.getString("memofont", "")
    private var textFontSize = MyApplication.prefs.getString("textFontSize", "20").toInt()

    private var end = 0

    // 뒤로 가기 플래그
    private var backFlag = false

    // font Style
    private var isBold = false
    private var isItalic = false
    private var isUnderline = false

    @SuppressLint("MissingInflatedId", "WrongViewCast")
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

        // < 카테고리 >
        // ctgrList
        val ctgrList = ArrayList<SpinnerModel>()
        ctgrList.add(0, SpinnerModel(R.drawable.closed_box, "미분류"))
        for (i in helper.selectCtgrMap().values.toMutableList()) {
            val spinnerModel = SpinnerModel(R.drawable.closed_box, i)
            ctgrList.add(spinnerModel)
        }

        // category spinner
        var mCtgr = 0 // 선택된 카테고리를 파악할 변수
        binding.category.adapter = SpinnerAdapter(this, R.layout.item_spinner, ctgrList)
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

        // < 폰트 사이즈 >
        var fontSizeList: List<String>                                              // 글씨 설정에 따라 메모 폰트 사이즈 리스트가 달라짐

        // 설정 반영
        if (fontSize.equals("ON")) {
            fontSizeList = listOf("26", "28", "30", "32", "34", "36")
        } else {
            fontSizeList = listOf("20", "22", "24", "26", "28", "30")
        }

        var selectedIndex =
            fontSizeList.indexOf(textFontSize.toString())           // fontSize spinner에서 선택된 index

        // fontSize spinner
        binding.fontSize.adapter =
            ArrayAdapter(this, R.layout.spinner_layout, fontSizeList.toMutableList())
        binding.fontSize.setSelection(selectedIndex)
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
                selectedIndex = position
            }
        }

        // fontSize spinner 옆 사이즈 조절 버튼
        binding.sizedown.setOnClickListener { // 사이즈 다운
            if (selectedIndex != 0) {
                binding.fontSize.setSelection(selectedIndex - 1)
            }
        }

        binding.sizeup.setOnClickListener { // 사이즈 업
            if (selectedIndex != fontSizeList.size - 1) {
                binding.fontSize.setSelection(selectedIndex + 1)
            }
        }

        // < 저장 >
        binding.saveMemo.setOnClickListener {
            if (binding.writeContent.text.toString().isNotEmpty()) {
                val memo: Memo
                // 제목, 내용, 내용 속성(정렬, 폰트사이즈), 생성일시, 카테고리, 우선순위, 상태
                val mTitle =
                    if (binding.writeTitle.text.toString() == "") "빈 제목" else binding.writeTitle.text.toString()
                // edittext에서 자동으로 생성되는 underline 제거
                if (end == binding.writeContent.length()) isUnderline = true
                if (!isUnderline) { // 가장 끝부분의 underline이 의도하지 않은 underline일때
                    val underlines = binding.writeContent.text!!.getSpans(
                        binding.writeContent.length() - 1,
                        binding.writeContent.length(),
                        UnderlineSpan::class.java
                    )
                    for (underline in underlines) {
                        binding.writeContent.text!!.removeSpan(underline)
                    }
                }
                val mContent = Html.toHtml(binding.writeContent.text)
                val mContentAttribute =
                    binding.writeContent.gravity.toString() + "," + textFontSize.toString()
                val mDate = System.currentTimeMillis()
                var mPriority = helper.checkTopPriority(mCtgr) + 1
                var mStatus = 1


                // Database에 insert
                memo = Memo(
                    null,
                    mTitle,
                    mContent,
                    mContentAttribute,
                    mDate,
                    mCtgr,
                    mPriority,
                    mStatus
                )
                helper.insertMemo(memo)

                // 진동
                if (vibration.equals("ON")) {
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.vibrate(VibrationEffect.createOneShot(200, 50))
                }

                // 색 있는 이미지로 변경, 0.5초 후 다시 원래 이미지로 변경
                binding.saveMemo.setImageResource(R.drawable.save2)
                val handler = android.os.Handler()
                handler.postDelayed({ binding.saveMemo.setImageResource(R.drawable.save1) }, 200)

                // 변수 초기화 & edittext 비우기
                isBold = false
                isItalic = false
                isUnderline = false
                binding.writeTitle.setText("")
                binding.writeContent.setText("")
            }
        }

        // dpi 값에 맞게 layoutParams 조절
        val dpi = resources.displayMetrics.densityDpi

        val rect = Rect()
        binding.writeLayout.getWindowVisibleDisplayFrame(rect)
        val layoutParams2 = binding.writeTitle.layoutParams
        layoutParams2.width = (rect.width() * 0.9).toInt()
        binding.writeTitle.layoutParams = layoutParams2

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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
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

                // 뒤로 가기 플래그 조절
                backFlag = !binding.writeContent.text.toString().trim()
                    .equals("") && binding.writeContent.text!!.isNotEmpty()
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

        if (start == end) { // 드래그 하지 않은 경우
            setNextFontStyle(fontKind)
        } else { // 드래그 한 경우
            setSeletedFontStyle(fontKind, start, end)
        }
    }

    fun setNextFontStyle(fontKind: String) {
        // 드래그 하지 않은 경우 폰트 스타일
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
        // 드래그 한 경우 폰트 스타일
        val spans = binding.writeContent.text!!.getSpans(start, end, StyleSpan::class.java)
        val underlines = binding.writeContent.text!!.getSpans(start, end, UnderlineSpan::class.java)

        when (fontKind) {
            "underline" ->
                if (this.end < end) { // 가장 뒤쪽에 있는 end를 클래스의 end값으로 초기화
                    this.end = end
                }
        }

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

    override fun onBackPressed() {
        MyApplication.prefs.setString("textFontSize", "$textFontSize")
        if (backFlag) {
            backKeyHandler.onBackPressed() // 텍스트가 있는 경우 [저장하지 않은 메모는 사라집니다] 출력
        } else {
            super.onBackPressed()
        }
    }
}