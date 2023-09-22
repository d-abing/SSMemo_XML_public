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
import com.aube.ssgmemo.Memo
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.databinding.ActivityMainBinding
import com.aube.ssgmemo.etc.MyApplication
import com.aube.ssgmemo.fragment.SettingFragment


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    var vibration = MyApplication.prefs.getString("vibration", "")
    var fontSize = MyApplication.prefs.getString("fontSize", "")
    var darkmode: Int = MyApplication.prefs.getString("darkmode", "0").toInt()
    var firstCreate = MyApplication.prefs.getString("firstCreate", "o")
    var memofont = MyApplication.prefs.getString("memofont", "")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (firstCreate.equals("o")) {
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
            MyApplication.prefs.setString("firstCreate", "x")
        }

        // 설정 fragment
        binding.btnSetting1.setOnClickListener {
            supportFragmentManager.beginTransaction().add(R.id.frameLayout, SettingFragment())
                .commit()
        }

        if (darkmode == Configuration.UI_MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            binding.mainLayout.setBackgroundColor(Color.DKGRAY)
            binding.adView.setBackgroundColor(Color.DKGRAY)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // memomo 이동 좌표
        var startX = 0f
        var startY = 0f
        var xx = 0f
        var yy = 0f
        var coordinatorAdjust = 100f

        // memomo 이동
        binding.memomo.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    xx = v.x
                    yy = v.y
                }

                MotionEvent.ACTION_MOVE -> {
                    val movedX: Float = event.x - startX
                    val movedY: Float = event.y - startY

                    v.x = v.x + movedX
                    v.y = v.y + movedY
                }

                MotionEvent.ACTION_UP -> {
                    //TextView의 상대적 좌표
                    val write: ArrayList<Float> = getRange(binding.write)
                    val classify: ArrayList<Float> = getRange(binding.classify)
                    val gather: ArrayList<Float> = getRange(binding.gather)
                    val complete: ArrayList<Float> = getRange(binding.throwout)
                    goMenu<WriteActivity>(
                        v,
                        v.x + v.width / 2,
                        v.y + v.height / 2,
                        write[2] + coordinatorAdjust,
                        write[0] - coordinatorAdjust,
                        write[3] + coordinatorAdjust,
                        write[1] - coordinatorAdjust,
                        WriteActivity::class.java
                    )
                    goMenu<ClassifyActivity>(
                        v,
                        v.x + v.width / 2,
                        v.y + v.height / 2,
                        classify[2],
                        classify[0],
                        classify[3],
                        classify[1],
                        ClassifyActivity::class.java
                    )
                    goMenu<ViewCtgrActivity>(
                        v,
                        v.x + v.width / 2,
                        v.y + v.height / 2,
                        gather[2],
                        gather[0],
                        gather[3],
                        gather[1],
                        ViewCtgrActivity::class.java
                    )
                    goMenu<CompleteActivity>(
                        v,
                        v.x + v.width / 2,
                        v.y + v.height / 2,
                        complete[2],
                        complete[0],
                        complete[3],
                        complete[1],
                        CompleteActivity::class.java
                    )
                    v.x = xx
                    v.y = yy
                }
            }
            true
        }
        binding.write.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            startActivity(intent)
        }
        binding.gather.setOnClickListener {
            val intent = Intent(this, ViewCtgrActivity::class.java)
            startActivity(intent)
        }
        binding.classify.setOnClickListener {
            val intent = Intent(this, ClassifyActivity::class.java)
            startActivity(intent)
        }
        binding.throwout.setOnClickListener {
            val intent = Intent(this, CompleteActivity::class.java)
            startActivity(intent)
        }
    }

    // 각 Activity로 이동
    fun <T> goMenu(
        v: View, x: Float, y: Float,
        range1: Float, range2: Float, range3: Float, range4: Float,
        targetActivity: Class<T>
    ) {

        if (x < range1 && x > range2 && y < range3 && y > range4) {
            if (vibration.equals("ON")) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(200, 50))
            }

            val intent = Intent(this, targetActivity)
            startActivity(intent)
        }
    }

    fun getRange(view: View): ArrayList<Float> {
        var result: ArrayList<Float> = ArrayList()
        result.add(view.x)
        result.add(view.y)
        result.add(view.x + view.width)
        result.add(view.y + view.height)
        return result
    }

    // 설정 fragment 뒤로가기 시 닫기 구현
    interface onBackPressedListener {
        fun onBackPressed()
    }

    override fun onBackPressed() {
        if (supportFragmentManager.fragments.isNotEmpty()) {
            (supportFragmentManager.fragments[0] as onBackPressedListener).onBackPressed()
            return
        } else {
            super.onBackPressed()
        }
    }
}