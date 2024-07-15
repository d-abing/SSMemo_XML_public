package com.aube.ssgmemo.common

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.aube.ssgmemo.Memo
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.databinding.ActivityMainBinding
import com.aube.ssgmemo.etc.MyApplication
import com.aube.ssgmemo.fragment.SettingFragment


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private val preferences = MyApplication.prefs
    private var vibration = preferences.getString("vibration", "OFF")
    private var darkmode = preferences.getInt("darkmode", 16)
    private var firstCreate = preferences.getString("firstCreate", "o")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        if (firstCreate == "o") {
            initializeMemo()
        }
        initializeUI()
        initializeListeners()
        //initializeAds()
    }

    private fun initializeMemo() {
        val helper = SqliteHelper(this, "ssgMemo", 1)
        val memo = Memo(
            null,
            "생각정리 메모장 쓱싹메모\uD83E\uDD14",
            "<p dir=\"ltr\">&#50417;&#49913;-&#129529; &#49373;&#44033;&#51012; &#51221;&#47532;&#54616;&#44256; &#49910;&#51012; &#46412; &#49324;&#50857;&#54616;&#49464;&#50836;<br>\n" +
                    "&#9999;&#65039; [&#50416;&#44592;] &#50640;&#49436; &#49373;&#44033;&#51012; &#51088;&#50976;&#47213;&#44172; &#54364;&#54788;&#54616;&#49464;&#50836;<br>\n" +
                    "&#128230; [&#48516;&#47448;] &#50640;&#49436; &#54868;&#49332;&#54364; &#48169;&#54693;&#51004;&#47196; &#54868;&#47732;&#51012; &#50424;&#50612; &#48516;&#47448;&#54616;&#44256; &#49910;&#51008; &#47700;&#47784;&#47484; &#44256;&#47476;&#44256; &#52852;&#53580;&#44256;&#47532; &#49345;&#51088;&#47484; &#45580;&#47084; &#44036;&#54200;&#54616;&#44172; &#48516;&#47448;&#54624; &#49688; &#51080;&#49845;&#45768;&#45796;<br>\n" +
                    "&#128209; [&#48372;&#44592;] &#50640;&#49436; &#48516;&#47448;&#46108; &#47700;&#47784;&#46308;&#51012; &#54869;&#51064;&#54616;&#44256; &#47700;&#47784;&#47484; &#44985; &#45580;&#47084; &#49692;&#49436;&#47484; &#48320;&#44221;&#54624; &#49688; &#51080;&#49845;&#45768;&#45796;. &#50756;&#47308; &#47700;&#47784;&#47196; &#46321;&#47197;&#54616;&#44256; &#49910;&#45796;&#47732; &#47785;&#47197;&#50640;&#49436; &#47700;&#47784;&#47484; &#50724;&#47480;&#51901;&#51004;&#47196; &#48128;&#50612; &#50756;&#47308; &#48260;&#53948;&#51012; &#45580;&#47084;&#51469;&#45768;&#45796;.<br>\n" +
                    "&#9971;&#65039; [&#50756;&#47308;] &#50640; &#51221;&#47532;&#46108; &#49373;&#44033;&#46308;&#51012; &#47784;&#50500; &#48372;&#44256; &#49910;&#51012; &#46412;&#47560;&#45796; &#54869;&#51064;&#54644; &#48372;&#49464;&#50836;</p>",
            "8388659,20",
            System.currentTimeMillis(),
            0,
            1,
            1
        )
        helper.insertMemo(memo)
        preferences.setString("firstCreate", "x")
    }

    private fun initializeUI() {
        setDarkMode(darkmode == Configuration.UI_MODE_NIGHT_YES)

        binding.btnSetting.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .add(R.id.frameLayout, SettingFragment())
                .commit()
        }
    }

    private fun setDarkMode(isDarkMode: Boolean) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            binding.mainLayout.setBackgroundColor(Color.DKGRAY)
            binding.adView.setBackgroundColor(Color.DKGRAY)
            val drawable = ContextCompat.getDrawable(this, R.drawable.baseline_settings_24)
            drawable?.setTint(ContextCompat.getColor(this, R.color.lightgray))
            binding.btnSetting.setImageDrawable(drawable)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun initializeListeners() {
        binding.memomo.setOnTouchListener(DragTouchListener())
        binding.write.setOnClickListener { navigateToActivity(WriteActivity::class.java) }
        binding.view.setOnClickListener { navigateToActivity(ViewCtgrActivity::class.java) }
        binding.classify.setOnClickListener { navigateToActivity(ClassifyActivity::class.java) }
        binding.complete.setOnClickListener { navigateToActivity(CompleteActivity::class.java) }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    /*private fun initializeAds() {
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }*/

    private inner class DragTouchListener : View.OnTouchListener {
        // 터치가 시작될 때의 X, Y 좌표
        private var startX = 0f
        private var startY = 0f

        // 터치가 시작될 때의 View의 X, Y 좌표
        private var xx = 0f
        private var yy = 0f

        override fun onTouch(
            v: View,
            event: MotionEvent
        ): Boolean { // v: 터치 이벤트가 발생한 View, event: 터치 이벤트
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { // 터치 시작
                    startX = event.x
                    startY = event.y
                    xx = v.x
                    yy = v.y
                }

                MotionEvent.ACTION_MOVE -> { // 터치 이동
                    val movedX = event.x - startX
                    val movedY = event.y - startY
                    v.x += movedX
                    v.y += movedY
                }

                MotionEvent.ACTION_UP -> { // 터치 끝
                    handleDragEnd(v)
                    v.x = xx
                    v.y = yy
                }
            }
            return true
        }

        private fun handleDragEnd(v: View) {
            // 각 메뉴의 View 범위
            val writeRange = getViewRange(binding.write)
            val classifyRange = getViewRange(binding.classify)
            val viewRange = getViewRange(binding.view)
            val completeRange = getViewRange(binding.complete)

            navigateOnDrag(v, writeRange, WriteActivity::class.java)
            navigateOnDrag(v, classifyRange, ClassifyActivity::class.java)
            navigateOnDrag(v, viewRange, ViewCtgrActivity::class.java)
            navigateOnDrag(v, completeRange, CompleteActivity::class.java)
        }

        private fun getViewRange(view: View): List<Float> {
            return listOf(view.x, view.y, view.x + view.width, view.y + view.height)
        }

        private fun <T> navigateOnDrag(v: View, range: List<Float>, targetActivity: Class<T>) {
            val (startX, startY, endX, endY) = range
            val viewLeft = v.x
            val viewTop = v.y
            val viewRight = v.x + v.width
            val viewBottom = v.y + v.height

            // Check if any edge of the view intersects with the specified range
            if (isViewInRange(
                    viewLeft,
                    viewTop,
                    viewRight,
                    viewBottom,
                    startX,
                    startY,
                    endX,
                    endY
                )
            ) {
                if (vibration == "ON") {
                    val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.vibrate(VibrationEffect.createOneShot(200, 50))
                }

                navigateToActivity(targetActivity)
            }
        }

        private fun isViewInRange(
            viewLeft: Float, viewTop: Float, viewRight: Float, viewBottom: Float,
            rangeLeft: Float, rangeTop: Float, rangeRight: Float, rangeBottom: Float
        ): Boolean {
            return !(viewRight < rangeLeft || viewLeft > rangeRight || viewBottom < rangeTop || viewTop > rangeBottom)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val fragment = supportFragmentManager.fragments.firstOrNull()
        if (fragment is OnBackPressedListener) {
            fragment.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }

    interface OnBackPressedListener {
        fun onBackPressed()
    }
}