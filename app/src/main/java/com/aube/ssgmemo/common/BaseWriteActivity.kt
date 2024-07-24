package com.aube.ssgmemo.common

import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.ActionMode
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.widget.doOnTextChanged
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SpinnerModel
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.adapter.SpinnerAdapter
import com.aube.ssgmemo.databinding.ActivityWriteBinding
import com.aube.ssgmemo.etc.MyApplication

abstract class BaseWriteActivity : AppCompatActivity() {
    protected lateinit var binding: ActivityWriteBinding
    protected val helper = SqliteHelper(this, "ssgMemo", 1)

    protected var vibration = MyApplication.prefs.getString("vibration", "OFF")
    protected var largeFont = MyApplication.prefs.getString("largeFont", "OFF")
    protected var darkmode = MyApplication.prefs.getInt("darkmode", 16)
    private var memofont = MyApplication.prefs.getString("memofont", "")
    protected var fontSize = MyApplication.prefs.getInt("fontSize", 20)

    protected var isBold = false
    protected var isItalic = false
    protected var isUnderline = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        // initializeAds()
    }

    protected open fun setupUI() {
        applyDarkMode()
        applyFontSettings()
        setupCategorySpinner()
        applyAlignment()
        setupFontSizeSpinner()
        adjustVisibility()
    }

    protected fun applyDarkMode() {
        if (darkmode == 32) {
            binding.writeLayout.setBackgroundColor(Color.BLACK)
            binding.fontBar.setBackgroundColor(Color.DKGRAY)
            binding.adView.setBackgroundColor(Color.BLACK)
            binding.category.setBackgroundResource(R.drawable.bg_spinner2)
        }
    }

    protected fun applyFontSettings() {
        if (memofont.isNotEmpty()) {
            val typeface = Typeface.createFromAsset(assets, "font/$memofont.ttf")
            binding.writeTitle.typeface = typeface
            binding.writeContent.typeface = typeface
        }
    }


    protected fun setupCategorySpinner() {
        val ctgrList = mutableListOf(
            SpinnerModel(R.drawable.closed_box, "미분류")
        ).apply {
            addAll(helper.selectCtgrMap().values.map {
                SpinnerModel(R.drawable.closed_box, it)
            })
        }

        binding.category.adapter =
            SpinnerAdapter(this, R.layout.item_category_spinner, ctgrList)

        val ctgrname = intent.getStringExtra("ctgrname") ?: "미분류"

        if (ctgrname != "미분류") {
            val selectedIndex = ctgrList.indexOfFirst { it.name == ctgrname }
            binding.category.setSelection(selectedIndex)
        }
    }

    protected fun applyAlignment() {
        val gravity = binding.writeContent.gravity
        when (gravity) {
            Gravity.START or Gravity.TOP -> setupAlignmentButtonsColor(Gravity.START)
            Gravity.CENTER_HORIZONTAL or Gravity.TOP -> setupAlignmentButtonsColor(Gravity.CENTER_HORIZONTAL)
            Gravity.END or Gravity.TOP -> setupAlignmentButtonsColor(Gravity.END)
        }
    }

    protected open fun setupFontSizeSpinner() {
        val fontSizeList = if (largeFont == "ON") {
            listOf("26", "28", "30", "32", "34", "36")
        } else {
            listOf("20", "22", "24", "26", "28", "30")
        }

        var selectedIndex = fontSizeList.indexOf(fontSize.toString())

        binding.fontSize.adapter =
            ArrayAdapter(this, R.layout.item_condition_spinner, fontSizeList)
        binding.fontSize.setSelection(selectedIndex)
        binding.fontSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                changeFontSize(fontSizeList[position])
                fontSize = fontSizeList[position].toInt()
                selectedIndex = position
            }
        }

        binding.sizedown.setOnClickListener {
            if (selectedIndex > 0) binding.fontSize.setSelection(
                selectedIndex - 1
            )
        }
        binding.sizeup.setOnClickListener {
            if (selectedIndex < fontSizeList.size - 1) binding.fontSize.setSelection(
                selectedIndex + 1
            )
        }
    }

    protected fun changeFontSize(fontSize: String) {
        val fontSizeInPixels = dpToPx(fontSize.toInt())
        binding.writeContent.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizeInPixels.toFloat())
    }

    private fun dpToPx(dp: Int): Int {
        val scale = resources.displayMetrics.density
        return (dp * scale + 0.5f).toInt()
    }

    protected fun adjustVisibility() {
        val rect = Rect()
        var layoutHeight = 0

        binding.writeLayout.viewTreeObserver.addOnGlobalLayoutListener {
            binding.writeLayout.getWindowVisibleDisplayFrame(rect)
            layoutHeight = maxOf(layoutHeight, rect.bottom)

            if (rect.bottom < layoutHeight) {
                // 키보드가 올라간 경우
                binding.adView.visibility = View.GONE
                binding.fontBar.visibility = View.VISIBLE
                updateConstraints(R.id.fontBar)
            } else {
                // 키보드가 내려간 경우
                binding.adView.visibility = View.VISIBLE
                binding.fontBar.visibility = View.GONE
                updateConstraints(R.id.adView)
            }
        }
    }

    private fun updateConstraints(targetId: Int) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.writeLayout)
        constraintSet.connect(R.id.writeContent, ConstraintSet.BOTTOM, targetId, ConstraintSet.TOP)
        constraintSet.applyTo(binding.writeLayout)
    }

    /*protected fun initializeAds() {
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }*/

    protected open fun setupListeners() {
        binding.writeContent.doOnTextChanged { _, _, _, _ -> handleTextChanged() }
        binding.writeContent.addTextChangedListener(createTextWatcher())
        binding.writeContent.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = true
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                updateButtonStyles()
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false
            override fun onDestroyActionMode(mode: ActionMode?) {
                applyButtonStyles()
            }
        }
        setupFontStyleButtons()
        setupAlignmentButtons()
    }

    private fun handleTextChanged() {
        val cursorPosition = binding.writeContent.selectionStart
        val cursorLineIndex = binding.writeContent.layout.getLineForOffset(cursorPosition)
        val lastLineIndex =
            binding.writeContent.layout.getLineForOffset(binding.writeContent.length())
        val fontHeight = dpToPx(fontSize + 10)
        val maxLine = (binding.writeContent.height - dpToPx(20)) / fontHeight

        scrollChange(cursorLineIndex, lastLineIndex, maxLine)
    }

    private fun scrollChange(cursorLineIndex: Int, lastLineIndex: Int, maxLine: Int) {
        if (cursorLineIndex > (maxLine - 1) && cursorLineIndex == lastLineIndex) {
            val scrollAmount =
                binding.writeContent.layout.getLineBottom(binding.writeContent.lineCount - 1) - binding.writeContent.height + dpToPx(
                    fontSize
                )
            binding.writeContent.scrollTo(0, scrollAmount)
        } else if (cursorLineIndex > (maxLine - 1) && cursorLineIndex < lastLineIndex) {
            val scrollAmount =
                binding.writeContent.layout.getLineBottom(cursorLineIndex) - binding.writeContent.height + dpToPx(
                    fontSize
                )
            binding.writeContent.scrollTo(0, scrollAmount)
        } else if (cursorLineIndex <= (maxLine - 1)) {
            binding.writeContent.scrollTo(0, 0)
        }
    }

    private fun setupAlignmentButtons() {
        binding.leftAlign.setOnClickListener {
            binding.writeContent.gravity = Gravity.START
            setupAlignmentButtonsColor(Gravity.START)
        }
        binding.centerAlign.setOnClickListener {
            binding.writeContent.gravity = Gravity.CENTER_HORIZONTAL
            setupAlignmentButtonsColor(Gravity.CENTER_HORIZONTAL)
        }
        binding.rightAlign.setOnClickListener {
            binding.writeContent.gravity = Gravity.END
            setupAlignmentButtonsColor(Gravity.END)
        }
    }

    private fun setupAlignmentButtonsColor(gravity: Int) {
        binding.leftAlign.setBackgroundResource(if (gravity == Gravity.START) R.drawable.text_style_selected_background else R.drawable.text_style_background)
        binding.centerAlign.setBackgroundResource(if (gravity == Gravity.CENTER_HORIZONTAL) R.drawable.text_style_selected_background else R.drawable.text_style_background)
        binding.rightAlign.setBackgroundResource(if (gravity == Gravity.END) R.drawable.text_style_selected_background else R.drawable.text_style_background)
    }

    protected open fun createTextWatcher() = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            applyTextStyles(s)
        }
    }

    protected fun applyTextStyles(s: Editable?) {
        val cursorPosition = binding.writeContent.selectionStart
        if (cursorPosition != 0) {
            if (isBold) applyTextStyle(s, Style.BOLD, cursorPosition - 1, cursorPosition)
            if (isItalic) applyTextStyle(s, Style.ITALIC, cursorPosition - 1, cursorPosition)
            if (isUnderline) applyTextStyle(s, Style.UNDERLINE, cursorPosition - 1, cursorPosition)
        }
    }

    private fun applyTextStyle(s: Editable?, style: Style, start: Int, end: Int) {
        s?.setSpan(
            getSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun getSpan(style: Style): Any {
        return when (style) {
            Style.BOLD -> StyleSpan(Typeface.BOLD)
            Style.ITALIC -> StyleSpan(Typeface.ITALIC)
            Style.UNDERLINE -> UnderlineSpan()
        }
    }

    private fun updateButtonStyles() {
        val start = binding.writeContent.selectionStart
        val end = binding.writeContent.selectionEnd
        val s = binding.writeContent.text

        if (start != end) {
            updateButtonStyle(s, start, end, Style.BOLD, binding.bold)
            updateButtonStyle(s, start, end, Style.ITALIC, binding.italic)
            updateButtonStyle(s, start, end, Style.UNDERLINE, binding.underline)
        }
    }

    private fun updateButtonStyle(
        s: Editable,
        start: Int,
        end: Int,
        style: Style,
        button: ImageView
    ) {
        var allStyled = true
        for (i in start until end) {
            val characterSpans = when (style) {
                Style.BOLD -> s.getSpans(i, i + 1, StyleSpan::class.java)
                    .filter { it.style == Typeface.BOLD }

                Style.ITALIC -> s.getSpans(i, i + 1, StyleSpan::class.java)
                    .filter { it.style == Typeface.ITALIC }

                Style.UNDERLINE -> s.getSpans(i, i + 1, UnderlineSpan::class.java).toList()
            }
            if (characterSpans.isEmpty()) {
                allStyled = false
                break
            }
        }

        if (allStyled) {
            button.setBackgroundResource(R.drawable.text_style_selected_background)
        } else {
            button.setBackgroundResource(R.drawable.text_style_background)
        }
    }

    private fun applyButtonStyles() {
        binding.bold.setBackgroundResource(if (isBold) R.drawable.text_style_selected_background else R.drawable.text_style_background)
        binding.italic.setBackgroundResource(if (isItalic) R.drawable.text_style_selected_background else R.drawable.text_style_background)
        binding.underline.setBackgroundResource(if (isUnderline) R.drawable.text_style_selected_background else R.drawable.text_style_background)
    }

    private fun setupFontStyleButtons() {
        binding.bold.setOnClickListener { fontStyleChange(Style.BOLD) }
        binding.italic.setOnClickListener { fontStyleChange(Style.ITALIC) }
        binding.underline.setOnClickListener { fontStyleChange(Style.UNDERLINE) }
    }

    private fun fontStyleChange(style: Style) {
        val start = binding.writeContent.selectionStart
        val end = binding.writeContent.selectionEnd
        val text = binding.writeContent.text.substring(start, end)

        if (start == end && text.trim().isEmpty()) {
            toggleFontStyle(style)
        } else {
            applyFontStyleToSelection(binding.writeContent.text, style, start, end)
        }
    }

    private fun toggleFontStyle(style: Style) {
        when (style) {
            Style.BOLD -> isBold = !isBold
            Style.ITALIC -> isItalic = !isItalic
            Style.UNDERLINE -> isUnderline = !isUnderline
        }

        applyButtonStyles()
    }

    private fun applyFontStyleToSelection(s: Editable?, style: Style, start: Int, end: Int) {
        var allStyled = true
        val spansToRemove = mutableSetOf<Any>()

        when (style) {
            Style.BOLD -> {
                val spans = s!!.getSpans(start, end, StyleSpan::class.java)
                for (i in start until end) {
                    val spansAtPos = spans.filter {
                        it.style == Typeface.BOLD && s.getSpanStart(it) <= i && s.getSpanEnd(it) >= i
                    }
                    if (spansAtPos.isEmpty()) {
                        allStyled = false
                        break
                    } else {
                        spansToRemove.addAll(spansAtPos)
                    }
                }
            }

            Style.ITALIC -> {
                val spans = s!!.getSpans(start, end, StyleSpan::class.java)
                for (i in start until end) {
                    val spansAtPos = spans.filter {
                        it.style == Typeface.ITALIC && s.getSpanStart(it) <= i && s.getSpanEnd(it) >= i
                    }
                    if (spansAtPos.isEmpty()) {
                        allStyled = false
                        break
                    } else {
                        spansToRemove.addAll(spansAtPos)
                    }
                }
            }

            Style.UNDERLINE -> {
                val spans = s!!.getSpans(start, end, UnderlineSpan::class.java)
                for (i in start until end) {
                    val spansAtPos =
                        spans.filter { s.getSpanStart(it) <= i && s.getSpanEnd(it) >= i }
                    if (spansAtPos.isEmpty()) {
                        allStyled = false
                        break
                    } else {
                        spansToRemove.addAll(spansAtPos)
                    }
                }
            }
        }

        // 스타일 적용 해제
        if (allStyled) {
            spansToRemove.forEach { span ->
                val spanStart = s.getSpanStart(span)
                val spanEnd = s.getSpanEnd(span)

                if (spanStart <= start) {
                    s.setSpan(span, spanStart, start, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                if (spanEnd > end) {
                    s.setSpan(span, end, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                s.removeSpan(span)
            }
        } else {
            // 스타일 적용
            when (style) {
                Style.BOLD -> s.setSpan(
                    StyleSpan(Typeface.BOLD),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                Style.ITALIC -> s.setSpan(
                    StyleSpan(Typeface.ITALIC),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

                Style.UNDERLINE -> s.setSpan(
                    UnderlineSpan(),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }
}

enum class Style {
    BOLD,
    ITALIC,
    UNDERLINE
}
