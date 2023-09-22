package com.aube.ssgmemo.common

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.aube.ssgmemo.Ctgr
import com.aube.ssgmemo.Memo
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.adapter.RecyclerAdapter
import com.aube.ssgmemo.adapter.ViewPagerAdapter
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.databinding.ActivityClassifyBinding
import com.aube.ssgmemo.etc.MyApplication
import com.aube.ssgmemo.fragment.CtgrAddFragment
import com.aube.ssgmemo.fragment.MemoDeleteFragment
import io.github.muddz.styleabletoast.StyleableToast

class ClassifyActivity : AppCompatActivity(), CallbackListener {
    private lateinit var binding: ActivityClassifyBinding
    private val helper = SqliteHelper(this, "ssgMemo", 1)

    private var vibration = MyApplication.prefs.getString("vibration", "")
    private var darkmode = MyApplication.prefs.getString("darkmode", "0")

    private var pagerAdapter: ViewPagerAdapter? = null  // memoList 출력
    private var recyclerAdapter: RecyclerAdapter? = null// ctgrList 출력
    private var memoList: MutableList<Memo>? = null     // 미분류 memoList
    private var currentMemoIdx: Int? = null             // 현재 보고 있는 메모의 idx값
    private var tmp_position: Int = 0                   // viewpager의 현재 위치

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 다크모드 설정
        if (darkmode.equals("32")) {
            binding.classifyLayout.setBackgroundColor(Color.DKGRAY)
            binding.recyclerClassifyCtgr.setBackgroundColor(Color.BLACK)
            binding.btnDelete.setImageResource(R.drawable.delete2)
            binding.adView.setBackgroundColor(Color.BLACK)
        }

        // dpi 값에 맞게 layoutParams 조절
        val rect = Rect()
        binding.classifyLayout.getWindowVisibleDisplayFrame(rect)

        dpiLayoutParams(binding.recyclerClassifyCtgr, rect.width(), (rect.height() * 0.40).toInt())
        dpiLayoutParams(binding.viewpager, rect.width(), (rect.height() * 0.53).toInt())

        // < 메모 list >
        pagerAdapter = ViewPagerAdapter(this)
        memoList = helper.selectUnclassifiedMemoList()  // 분류되지 않은 memoList
        pagerAdapter!!.listData.addAll(memoList!!)      // pagerAdapter에 추가
        binding.viewpager.adapter = pagerAdapter        // viewpager에 pagerAdapter 등록
        if (memoList!!.size > 1) {                     // 미분류 메모가 1개 이상일 경우, 오른쪽 화살표 visibility visible
            binding.next.visibility = View.VISIBLE
        }

        binding.viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (memoList!!.isNotEmpty()) {
                    currentMemoIdx = memoList!![position].idx
                    tmp_position = position
                }

                if (memoList!!.size <= 1) {
                    binding.previous.visibility = View.GONE
                    binding.next.visibility = View.GONE
                } else {
                    if (tmp_position == 0) {
                        binding.previous.visibility = View.GONE
                        binding.next.visibility = View.VISIBLE
                    } else if (tmp_position < memoList!!.size - 1) {
                        binding.previous.visibility = View.VISIBLE
                        binding.next.visibility = View.VISIBLE
                    } else if (tmp_position == memoList!!.size - 1) {
                        binding.next.visibility = View.GONE
                        binding.previous.visibility = View.VISIBLE
                    }
                }
            }

        })

        if (memoList!!.isEmpty()) { // memoList가 비어있을 경우 "분류할 메모가 없습니다" 출력
            binding.viewpager.visibility = View.INVISIBLE
            binding.emptyText.visibility = View.VISIBLE
            binding.btnDelete.visibility = View.GONE
        }

        binding.btnDelete.setOnClickListener {
            fragmentOpen(
                helper.selectMemo(currentMemoIdx.toString()).ctgr,
                currentMemoIdx.toString()
            )
        }

        // < 카테고리 list >
        recyclerAdapter = RecyclerAdapter(this)
        recyclerAdapter!!.helper = helper
        if (memoList!!.isNotEmpty()) {
            recyclerAdapter!!.isExistMemoList = true
        }
        val btnCtgrAdd = Ctgr(null, "+")
        recyclerAdapter!!.listData.addAll(helper.selectCtgrList())
        recyclerAdapter!!.listData.add(btnCtgrAdd)
        binding.recyclerClassifyCtgr.adapter = recyclerAdapter
        binding.recyclerClassifyCtgr.layoutManager = GridLayoutManager(this, 4)

    }

    fun dpiLayoutParams(view: View, width: Int, height: Int) {
        val tmp_layoutParams = view.layoutParams
        tmp_layoutParams.width = width
        tmp_layoutParams.height = height
        view.layoutParams = tmp_layoutParams
    }

    override fun fragmentOpen(item: String, ctgrIdx: String?) {
        if (item == "+") {
            val ctgrAddFrgment = CtgrAddFragment(this)
            ctgrAddFrgment.show(supportFragmentManager, "CtgrAdd")
        }
    }

    override fun addCtgr(ctgrName: String) {
        val ctgr = Ctgr(null, ctgrName)
        val btnCtgrAdd = Ctgr(null, "+")

        if (ctgrName != "미분류" && ctgrName != "delete@#" && ctgrName != "+" && ctgrName != "휴지통") {
            if (!helper.checkDuplicationCtgr(ctgrName)) {
                helper.insertCtgr(ctgr)
                val ctgrList = helper.selectCtgrList() as MutableList<Any>
                ctgrList.add(btnCtgrAdd)
                recyclerAdapter!!.listData = ctgrList
                recyclerAdapter!!.notifyDataSetChanged()
            } else {
                val text = "이미 사용중 입니다"
                val toast = StyleableToast.makeText(applicationContext, text, R.style.toast)
                toast.show()
            }
        } else {
            val text = "사용할 수 없는 이름입니다"
            val toast = StyleableToast.makeText(applicationContext, text, R.style.toast)
            toast.show()
        }
    }

    override fun fragmentOpen(ctgrIdx: Int, memoIdx: String) {
        val memoDeleteFragment = MemoDeleteFragment(this)
        val bundle = Bundle()
        bundle.putString("memoIdx", memoIdx)
        bundle.putString("ctgrIdx", ctgrIdx.toString())
        memoDeleteFragment.arguments = bundle
        memoDeleteFragment.show(supportFragmentManager, "MemoDelete")
    }

    override fun moveCtgr(ctgrIdx: Int) {                                                     // RecyclerAdapter에서 호출되는 callback 함수
        if (vibration.equals("ON")) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(200, 50))
        }
        moveCtgr(currentMemoIdx, ctgrIdx, 1)
    }

    override fun moveCtgr(memoIdx: Int?, ctgrIdx: Int, status: Int) {
        helper.updateMemoCtgr(memoIdx, 0, ctgrIdx)
        pagerAdapter!!.listData.clear()
        memoList = helper.selectUnclassifiedMemoList()               // 분류, 삭제로 인해 변경된 memoList 가져오기
        pagerAdapter!!.listData.addAll(memoList!!)
        pagerAdapter!!.notifyDataSetChanged()

        if (memoList!!.isNotEmpty()) {                               // 분류, 삭제 후에도 memoList가 남아있으면
            if (memoList!!.size == tmp_position) {                  // 마지막 메모라면
                tmp_position = tmp_position - 1
                binding.viewpager.currentItem = tmp_position
            }
            currentMemoIdx = memoList!![tmp_position].idx
        } else {                                                  // 분류, 삭제 후 메모리스트가 남아있지 않을 경우
            binding.viewpager.visibility = View.INVISIBLE
            binding.emptyText.visibility = View.VISIBLE           // "분류할 메모가 없습니다" 출력
            binding.btnDelete.visibility = View.INVISIBLE
            memoList!!.clear()
            recyclerAdapter!!.isExistMemoList = false
            recyclerAdapter!!.notifyDataSetChanged()
        }

        if (memoList!!.size == tmp_position + 1) {
            binding.next.visibility = View.GONE
        }
    }
}