package com.aube.ssgmemo.common

import ViewPagerAdapter
import android.content.Context
import android.graphics.Color
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
import com.aube.ssgmemo.adapter.ClassifyCtgrAdapter
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.databinding.ActivityClassifyBinding
import com.aube.ssgmemo.etc.MyApplication
import com.aube.ssgmemo.fragment.CtgrAddFragment
import com.aube.ssgmemo.fragment.MemoDeleteFragment
import io.github.muddz.styleabletoast.StyleableToast

class ClassifyActivity : AppCompatActivity(), CallbackListener {
    private lateinit var binding: ActivityClassifyBinding
    private val helper = SqliteHelper(this, "ssgMemo", 1)

    private val vibration by lazy { MyApplication.prefs.getString("vibration", "") }
    private val darkmode by lazy { MyApplication.prefs.getInt("darkmode", 0) }

    private lateinit var pagerAdapter: ViewPagerAdapter
    private lateinit var classifyCtgrAdapter: ClassifyCtgrAdapter
    private var memoList: MutableList<Memo> = mutableListOf()
    private var currentMemoIdx: Int? = null
    private var currentPosition: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClassifyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDarkMode()
        setupViewPager()
        setupCategoryRecyclerView()
        //setupAds()
        setupDeleteButton()
    }

    private fun setupDarkMode() {
        if (darkmode == 32) {
            binding.classifyLayout.setBackgroundColor(Color.DKGRAY)
            binding.recyclerClassifyCtgr.setBackgroundColor(Color.BLACK)
            binding.btnDelete.setImageResource(R.drawable.delete2)
            binding.adView.setBackgroundColor(Color.BLACK)
        }
    }

    private fun setupViewPager() {
        pagerAdapter = ViewPagerAdapter(this)
        memoList = helper.selectUnclassifiedMemoList()
        pagerAdapter.submitList(memoList)
        binding.viewpager.adapter = pagerAdapter

        binding.viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                handlePageSelected(position)
            }
        })

        updateNavigationButtons()
        setupNavigationButton()
        updateEmptyTextVisibility()
    }

    private fun handlePageSelected(position: Int) {
        if (memoList.isNotEmpty()) {
            currentMemoIdx = memoList[position].idx
            currentPosition = position
        }
        updateNavigationButtons()
    }

    private fun setupNavigationButton() {
        binding.previous.setOnClickListener {
            if (currentPosition > 0) {
                binding.viewpager.currentItem = currentPosition - 1
            }
        }
        binding.next.setOnClickListener {
            if (currentPosition < memoList.size - 1) {
                binding.viewpager.currentItem = currentPosition + 1
            }
        }
    }

    private fun updateNavigationButtons() {
        if (memoList.size <= 1) {
            binding.previous.visibility = View.GONE
            binding.next.visibility = View.GONE
        } else {
            if (currentPosition == 0) {
                binding.previous.visibility = View.GONE
                binding.next.visibility = View.VISIBLE
            } else if (currentPosition < memoList.size - 1) {
                binding.previous.visibility = View.VISIBLE
                binding.next.visibility = View.VISIBLE
            } else if (currentPosition == memoList.size - 1) {
                binding.next.visibility = View.GONE
                binding.previous.visibility = View.VISIBLE
            }
        }
    }

    private fun updateEmptyTextVisibility() {
        if (memoList.isEmpty()) {
            binding.viewpager.visibility = View.INVISIBLE
            binding.emptyText.visibility = View.VISIBLE
            binding.btnDelete.visibility = View.GONE
        } else {
            binding.viewpager.visibility = View.VISIBLE
            binding.emptyText.visibility = View.INVISIBLE
            binding.btnDelete.visibility = View.VISIBLE
        }
    }

    private fun setupCategoryRecyclerView() {
        classifyCtgrAdapter = ClassifyCtgrAdapter(this).apply {
            isExistMemoList = memoList.isNotEmpty()
        }
        val btnCtgrAdd = Ctgr(null, "+")
        classifyCtgrAdapter.submitList(helper.selectCtgrList() + btnCtgrAdd)
        binding.recyclerClassifyCtgr.apply {
            adapter = classifyCtgrAdapter
            layoutManager = GridLayoutManager(this@ClassifyActivity, 4)
        }
    }

    /*private fun setupAds() {
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }*/

    private fun setupDeleteButton() {
        binding.btnDelete.setOnClickListener {
            deleteFragmentOpen(0)
        }
    }

    override fun fragmentOpen(item: String, ctgrIdx: String?) {
        if (item == "+") {
            CtgrAddFragment(this).show(supportFragmentManager, "CtgrAdd")
        }
    }

    override fun addCtgr(ctgrName: String) {
        val ctgr = Ctgr(null, ctgrName)
        val btnCtgrAdd = Ctgr(null, "+")
        if (isValidCtgrName(ctgrName)) {
            if (!helper.checkDuplicationCtgr(ctgrName)) {
                helper.insertCtgr(ctgr)
                val ctgrList = helper.selectCtgrList() + btnCtgrAdd
                classifyCtgrAdapter.submitList(ctgrList)
            } else {
                showToast("이미 사용중 입니다")
            }
        } else {
            showToast("사용할 수 없는 이름입니다")
        }
    }

    private fun isValidCtgrName(name: String) = name !in listOf("미분류", "delete@#", "+", "휴지통")

    private fun showToast(message: String) {
        StyleableToast.makeText(applicationContext, message, R.style.toast).show()
    }

    private fun deleteFragmentOpen(ctgrIdx: Int) {
        val memoDeleteFragment = MemoDeleteFragment(this).apply {
            arguments = Bundle().apply {
                putInt("ctgrIdx", ctgrIdx)
            }
        }
        memoDeleteFragment.show(supportFragmentManager, "MemoDelete")
    }

    override fun moveCtgr(ctgrIdx: Int) {
        if (vibration == "ON") {
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(
                VibrationEffect.createOneShot(
                    200,
                    50
                )
            )
        }
        helper.updateMemoCtgr(currentMemoIdx, 0, ctgrIdx)
        refreshMemoList()
    }

    override fun moveCtgrList(oldCtgrIdx: Int, newCtgrIdx: Int) {
        helper.updateMemoCtgr(currentMemoIdx, 0, newCtgrIdx)
        refreshMemoList()
    }

    private fun refreshMemoList() {
        memoList = helper.selectUnclassifiedMemoList()
        pagerAdapter.submitList(memoList)
        updateNavigationButtons()
        updateEmptyTextVisibility()
        classifyCtgrAdapter.isExistMemoList = memoList.isNotEmpty()
        classifyCtgrAdapter.notifyDataSetChanged()
        binding.viewpager.currentItem = currentPosition
        currentMemoIdx = memoList.getOrNull(currentPosition)?.idx
    }
}
