package com.aube.ssgmemo.fragment

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aube.ssgmemo.R
import com.aube.ssgmemo.common.MainActivity
import com.aube.ssgmemo.databinding.FragmentSettingBinding
import com.aube.ssgmemo.etc.MyApplication
import java.util.regex.Pattern


class SettingFragment : Fragment(), MainActivity.OnBackPressedListener {
    private lateinit var mainActivity: MainActivity
    private lateinit var binding: FragmentSettingBinding

    private val preferences = MyApplication.prefs
    private var vibration = preferences.getString("vibration", "OFF")
    private var darkmode = preferences.getInt("darkmode", 16)
    private var largeFont = preferences.getString("largeFont", "OFF")
    private var memoFont = preferences.getString("memofont", "")

    private val memoFontMap =
        mapOf(
            "기본" to "",
            "강원교육모두체" to "kangwon",
            "KBIZ한마음명조체" to "kbiz",
            "고운바탕" to "gowun",
            "교보손글씨 2020 박도연" to "kyobo",
            "마포꽃섬" to "mapo",
            "오뮤 다예쁨체" to "omyu"
        )

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            mainActivity = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingBinding.inflate(inflater, container, false)
        binding.root.setOnTouchListener { _, _ -> true }
        setupUI()
        setupListeners()
        applySettings()

        return binding.root
    }

    private fun setupUI() {
        binding.btnSetting.setOnClickListener {
            onBackPressed()
        }

        Linkify.addLinks(
            binding.textView,
            Pattern.compile("개인정보처리방침"),
            "https://aubeco2.blogspot.com/2024/01/blog-post.html",
            null
        ) { _, _ -> "" }
    }

    private fun setupListeners() {
        binding.switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("vibration", if (isChecked) "ON" else "OFF")
            vibration = if (isChecked) "ON" else "OFF"

            if (vibration == "ON") {
                val vibrator =
                    requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(200, 50))
            }
        }

        binding.switchFontSize.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("largeFont", if (isChecked) "ON" else "OFF")
            updatePreference("fontSize", if (isChecked) 26 else 20)
            largeFont = if (isChecked) "ON" else "OFF"
        }

        binding.switchDarkmode.setOnCheckedChangeListener { _, isChecked ->
            updatePreference("darkmode", if (isChecked) 32 else 16)
            darkmode = if (isChecked) 32 else 16
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }


        binding.memofont.adapter =
            ArrayAdapter(
                requireContext(),
                R.layout.item_condition_spinner,
                memoFontMap.keys.toList()
            )
        binding.memofont.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                memoFontMap[memoFontMap.keys.toList()[position]]?.let { updateFont(it) }
            }
        }
    }

    private fun applySettings() {
        binding.switchVibrate.isChecked = vibration == "ON"
        binding.switchFontSize.isChecked = largeFont == "ON"
        binding.switchDarkmode.isChecked = darkmode == Configuration.UI_MODE_NIGHT_YES

        if (darkmode == Configuration.UI_MODE_NIGHT_YES) {
            binding.settingLayout.setBackgroundColor(Color.DKGRAY)
            val drawable =
                ContextCompat.getDrawable(requireContext(), R.drawable.baseline_settings_24)
            drawable?.setTint(ContextCompat.getColor(requireContext(), R.color.lightgray))
            binding.btnSetting.setImageDrawable(drawable)
        }
        binding.memofont.setSelection(memoFontMap.values.indexOf(memoFont))
    }

    private fun updatePreference(key: String, value: Any) {
        if (value is String) {
            MyApplication.prefs.setString(key, value)
        } else if (value is Int) {
            MyApplication.prefs.setInt(key, value)
        }
    }

    private fun updateFont(memoFont: String) {
        updatePreference("memofont", memoFont)

        val typeface = if (memoFont.isNotEmpty()) {
            Typeface.createFromAsset(mainActivity.assets, "font/$memoFont.ttf")
        } else {
            Typeface.DEFAULT
        }
        binding.fontexam.typeface = typeface
    }

    override fun onBackPressed() {
        requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
    }
}