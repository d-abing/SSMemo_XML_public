package com.aube.ssgmemo.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.Spannable
import android.text.Spanned
import android.text.TextWatcher
import android.text.method.KeyListener
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
    protected var memofont = MyApplication.prefs.getString("memofont", "")
    protected var fontSize = MyApplication.prefs.getInt("fontSize", 20)


    // font Style
    protected var isBold = false
    protected var isItalic = false
    protected var isUnderline = false

    protected var originalKeyListener: KeyListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        //initializeAds()
    }

    protected open fun setupUI() {
        applyFontSettings()
        applyDarkMode()
        setupCategorySpinner()
        applyAlignment()
        setupFontSizeSpinner()
        adjustVisibility()
    }

    protected fun applyFontSettings() {
        if (memofont.isNotEmpty()) {
            val typeface = Typeface.createFromAsset(assets, "font/$memofont.ttf")
            binding.writeTitle.typeface = typeface
            binding.writeContent.typeface = typeface
        }
    }

    protected fun applyDarkMode() {
        if (darkmode == 32) {
            binding.writeLayout.setBackgroundColor(Color.BLACK)
            binding.fontBar.setBackgroundColor(Color.DKGRAY)
            binding.adView.setBackgroundColor(Color.BLACK)
            binding.category.setBackgroundResource(R.drawable.bg_spinner2)
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
            Gravity.START or Gravity.TOP -> setupLeftAlignButtonColor()
            Gravity.CENTER_HORIZONTAL or Gravity.TOP -> setupCenterAlignButtonColor()
            Gravity.END or Gravity.TOP -> setupRightAlignButtonColor()
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
        val fontSizeInDp = fontSize.toInt()
        val scale = resources.displayMetrics.density
        val fontSizeInPixels = (fontSizeInDp * scale + 0.5f).toInt()
        binding.writeContent.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSizeInPixels.toFloat())
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

    protected open fun setupListeners() {
        binding.writeContent.doOnTextChanged { _, _, _, _ -> handleTextChanged() }
        binding.writeContent.addTextChangedListener(createTextWatcher())
        setupFontStyleButtons()
        setupAlignmentButtons()
    }

    /*protected fun initializeAds() {
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }*/

    protected fun handleTextChanged() {
        val cursorPosition = binding.writeContent.selectionStart
        val cursorLineIndex = binding.writeContent.layout.getLineForOffset(cursorPosition)
        val lastLineIndex =
            binding.writeContent.layout.getLineForOffset(binding.writeContent.length())
        val fontHeight = fontSize * 4
        val maxline = (binding.writeContent.height - 10) / fontHeight

        scrollChange(cursorLineIndex, lastLineIndex, maxline)
    }

    private fun scrollChange(cursorLineIndex: Int, lastLineIndex: Int, maxline: Int) {
        val scrollValues = mapOf(
            20 to Pair(86, 82),
            22 to Pair(60, 88),
            24 to Pair(116, 94),
            26 to Pair(85, 102),
            28 to Pair(135, 108),
            30 to Pair(70, 114),
            32 to Pair(125, 122),
            34 to Pair(169, 128),
            36 to Pair(130, 136)
        )

        val (base, multi) = scrollValues[fontSize] ?: Pair(0, 0)
        val x = cursorLineIndex - maxline
        val scrollY = base + multi * x

        if (cursorLineIndex > (maxline - 1) && cursorLineIndex == lastLineIndex) {
            val scrollAmount =
                binding.writeContent.layout.getLineBottom(binding.writeContent.lineCount - 1) - binding.writeContent.height + 65
            binding.writeContent.scrollTo(0, scrollAmount)
        } else if (cursorLineIndex > (maxline - 1) && binding.writeContent.scrollY !in scrollY..(scrollY + multi * (maxline - 1))) {
            binding.writeContent.scrollTo(0, scrollY)
        } else if (cursorLineIndex <= (maxline - 1)) {
            binding.writeContent.scrollTo(0, 0)
        }
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
        if (isBold) s?.setSpan(
            StyleSpan(Typeface.BOLD),
            cursorPosition - 1,
            cursorPosition,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (isItalic) s?.setSpan(
            StyleSpan(Typeface.ITALIC),
            cursorPosition - 1,
            cursorPosition,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (isUnderline) s?.setSpan(
            UnderlineSpan(),
            cursorPosition - 1,
            cursorPosition,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun setupFontStyleButtons() {
        binding.bold.setOnClickListener { fontStyleChange("bold") }
        binding.italic.setOnClickListener { fontStyleChange("italic") }
        binding.underline.setOnClickListener { fontStyleChange("underline") }
    }

    private fun setupAlignmentButtons() {
        binding.leftAlign.setOnClickListener {
            binding.writeContent.gravity = Gravity.START
            setupLeftAlignButtonColor()
        }
        binding.centerAlign.setOnClickListener {
            binding.writeContent.gravity = Gravity.CENTER_HORIZONTAL
            setupCenterAlignButtonColor()
        }
        binding.rightAlign.setOnClickListener {
            binding.writeContent.gravity = Gravity.END
            setupRightAlignButtonColor()
        }
    }

    private fun setupLeftAlignButtonColor() {
        binding.leftAlign.setBackgroundResource(R.drawable.text_style_selected_background)
        binding.centerAlign.setBackgroundResource(R.drawable.text_style_background)
        binding.rightAlign.setBackgroundResource(R.drawable.text_style_background)
    }

    private fun setupCenterAlignButtonColor() {
        binding.centerAlign.setBackgroundResource(R.drawable.text_style_selected_background)
        binding.leftAlign.setBackgroundResource(R.drawable.text_style_background)
        binding.rightAlign.setBackgroundResource(R.drawable.text_style_background)
    }

    private fun setupRightAlignButtonColor() {
        binding.rightAlign.setBackgroundResource(R.drawable.text_style_selected_background)
        binding.leftAlign.setBackgroundResource(R.drawable.text_style_background)
        binding.centerAlign.setBackgroundResource(R.drawable.text_style_background)
    }


    private fun fontStyleChange(style: String) {
        val start = binding.writeContent.selectionStart
        val end = binding.writeContent.selectionEnd
        if (start == end) {
            toggleFontStyle(style)
        } else {
            applyFontStyleToSelection(style, start, end)
        }
    }

    private fun toggleFontStyle(style: String) {
        when (style) {
            "bold" -> isBold = !isBold
            "italic" -> isItalic = !isItalic
            "underline" -> isUnderline = !isUnderline
        }

        if (isBold) {
            binding.bold.setBackgroundResource(R.drawable.text_style_selected_background)
        } else {
            binding.bold.setBackgroundResource(R.drawable.text_style_background)
        }

        if (isItalic) {
            binding.italic.setBackgroundResource(R.drawable.text_style_selected_background)
        } else {
            binding.italic.setBackgroundResource(R.drawable.text_style_background)
        }

        if (isUnderline) {
            binding.underline.setBackgroundResource(R.drawable.text_style_selected_background)
        } else {
            binding.underline.setBackgroundResource(R.drawable.text_style_background)
        }

        if (isBold || isItalic || isUnderline) {
            insertSpaceToApplyStyle()
        }
    }

    private fun insertSpaceToApplyStyle() {
        val cursorPosition = binding.writeContent.selectionStart
        binding.writeContent.text?.insert(cursorPosition, " ")
        binding.writeContent.setSelection(cursorPosition, cursorPosition + 1)
    }

    private fun applyFontStyleToSelection(style: String, start: Int, end: Int) {
        when (style) {
            "bold" -> binding.writeContent.text?.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            "italic" -> binding.writeContent.text?.setSpan(
                StyleSpan(Typeface.ITALIC),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            "underline" -> binding.writeContent.text?.setSpan(
                UnderlineSpan(),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    protected fun softkeyboardHide() { // 키보드 숨기기
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.writeContent.windowToken, 0)
    }

    protected fun triggerVibration() {
        if (vibration == "ON") {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(200, 50))
        }
    }

    protected fun copyToClipboard(text: String) { // 클립보드에 복사
        val clipboardManager =
            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("label", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    protected fun animateSaveButton() {
        binding.saveMemo.setImageResource(R.drawable.save2)
        android.os.Handler()
            .postDelayed({ binding.saveMemo.setImageResource(R.drawable.save1) }, 200)
    }

    protected fun resetFields() {
        isBold = false
        isItalic = false
        isUnderline = false
        binding.writeTitle.setText("")
        binding.writeContent.setText("")
    }

    protected fun <K, V> getKey(map: Map<K, V>, target: V): K {
        return map.keys.first { target == map[it] }
    }
}
