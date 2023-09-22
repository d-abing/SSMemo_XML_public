package com.aube.ssgmemo.fragment

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.util.Linkify
import android.text.util.Linkify.TransformFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.aube.ssgmemo.R
import com.aube.ssgmemo.common.MainActivity
import com.aube.ssgmemo.databinding.FragmentSettingBinding
import com.aube.ssgmemo.etc.MyApplication
import java.util.regex.Pattern


class SettingFragment() : Fragment(),  MainActivity.onBackPressedListener {
    lateinit var mainActivity: MainActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity)	mainActivity = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentSettingBinding.inflate(inflater, container, false)
        binding.btnSetting2.setOnClickListener {
            onBackPressed()
        }

        val dpi = resources.displayMetrics.densityDpi
        val layoutParams = binding.setting.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.topMargin = (-1.75 * dpi + 1085).toInt()
        binding.setting.layoutParams = layoutParams

        // 앱 설정 확인 후 switch에 적용
        if(mainActivity.vibration.equals("ON")) { // 진동
            binding.switchVibrate.isChecked = true
        }
        if(mainActivity.fontSize.equals("ON")) { // 큰 글씨
            binding.switchFontSize.isChecked = true
        }
        if(mainActivity.darkmode == Configuration.UI_MODE_NIGHT_YES) { // 다크모드
            binding.settingLayout.setBackgroundColor(Color.DKGRAY)
            binding.switchDarkmode.isChecked = true
        }


        // switch ChangeListener
        binding.switchVibrate.setOnCheckedChangeListener { _, ischecked -> // 진동
            if (ischecked) {
                MyApplication.prefs.setString("vibration", "ON")
                mainActivity.vibration = "ON"

            } else {
                MyApplication.prefs.setString("vibration", "OFF")
                mainActivity.vibration = "OFF"
            }
        }
        binding.switchFontSize.setOnCheckedChangeListener { _, ischecked -> // 큰 글씨
            if(ischecked) {
                MyApplication.prefs.setString("fontSize", "ON")
                MyApplication.prefs.setString("textFontSize", "26")
                mainActivity.fontSize = "ON"
            } else {
                MyApplication.prefs.setString("fontSize", "OFF")
                MyApplication.prefs.setString("textFontSize", "20")
                mainActivity.fontSize = "OFF"
            }
        }

        binding.switchDarkmode.setOnCheckedChangeListener { _, ischecked -> // 다크모드
            if(ischecked) {
                MyApplication.prefs.setString("darkmode", "32")
                mainActivity.darkmode = 32
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

            } else {
                MyApplication.prefs.setString("darkmode", "16")
                mainActivity.darkmode = 16
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        val memofont = listOf("기본", "강원교육모두체", "KBIZ한마음명조체", "고운바탕", "교보손글씨 2020 박도연", "마포꽃섬", "오뮤 다예쁨체")
        binding.memofont.adapter = ArrayAdapter(requireContext(), R.layout.spinner_layout, memofont.toMutableList())
        binding.memofont.setSelection(memofont.indexOf(mainActivity.memofont))
        binding.memofont.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                var fontname = ""
                 when (position) {
                     0 -> fontname = ""
                     1 -> fontname = "kangwon"
                     2 -> fontname = "kbiz"
                     3 -> fontname = "gowun"
                     4 -> fontname = "kyobo"
                     5 -> fontname = "mapo"
                     6 -> fontname = "omyu"
                }
                MyApplication.prefs.setString("memofont", fontname)
                if (!fontname.equals("")) {
                    val typeface = Typeface.createFromAsset(mainActivity.assets, "font/" + fontname + ".ttf")
                    binding.fontexam.typeface = typeface
                } else {
                    binding.fontexam.typeface = Typeface.DEFAULT
                }
                mainActivity.memofont = binding.memofont.getItemAtPosition(position).toString()
            }
        }

        val linktest = TransformFilter { match, url -> "" }
        val pattern: Pattern = Pattern.compile("개인정보처리방침")
        Linkify.addLinks(binding.textView, pattern, "https://aubeco.tistory.com/1", null, linktest)


        return binding.root
    }

    // 뒤로 가기
    override fun onBackPressed() {
        requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
    }
}