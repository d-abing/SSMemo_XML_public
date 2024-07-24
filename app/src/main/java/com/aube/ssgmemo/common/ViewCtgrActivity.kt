package com.aube.ssgmemo.common

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.GridLayoutManager
import com.aube.ssgmemo.Ctgr
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.adapter.SearchMemoAdapter
import com.aube.ssgmemo.adapter.ViewCtgrAdapter
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.databinding.ActivityViewCtgrBinding
import com.aube.ssgmemo.etc.MemoStatus
import com.aube.ssgmemo.fragment.CtgrAddFragment
import com.aube.ssgmemo.fragment.CtgrDeleteFragment

class ViewCtgrActivity : BaseSearchActivity(), CallbackListener {
    private lateinit var binding: ActivityViewCtgrBinding
    private val helper = SqliteHelper(this, "ssgMemo", 1)

    private lateinit var ctgrAdapter: ViewCtgrAdapter
    private val searchMemoAdapter = SearchMemoAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewCtgrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupFilterButtons()
        setupListeners()
        //setupAds(binding.adView)
    }

    private fun setupUI() {
        applyDarkMode()
        setupCategoryRecyclerView()
        setupSearchBar(binding.keyword) { loadData() }
        setupSpinners()
    }

    private fun applyDarkMode() {
        super.setupDarkMode(binding.viewCtgrLayout)
        if (darkmode == 32) {
            binding.adView.setBackgroundColor(Color.BLACK)
            binding.searchBar.setBackgroundResource(R.drawable.graysearchbar2)
            binding.btnFilter.setImageResource(R.drawable.grayfilter2)
        }
    }

    private fun setupCategoryRecyclerView() {
        ctgrAdapter = ViewCtgrAdapter(this).apply {
            helper = this@ViewCtgrActivity.helper
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        setDefaultCtgr()
        binding.recyclerViewCtgr.apply {
            adapter = ctgrAdapter
            layoutManager = GridLayoutManager(this@ViewCtgrActivity, 2)
        }
    }

    private fun setupSpinners() {
        val conditionList1 = listOf("제목+내용", "제목", "내용")
        val conditionList2 = listOf("최신순", "오래된순")

        setupSpinners(binding.conditionContain, conditionList1) {
            where = it
            loadData()
        }

        setupSpinners(binding.conditionLatest, conditionList2) {
            orderby = it
            loadData()
        }

    }

    private fun setupFilterButtons() {
        setupFilterButton(
            filterButton = binding.btnFilter,
            conditionContain = binding.conditionContain,
            conditionLatest = binding.conditionLatest,
            recyclerViewCtgr = binding.recyclerViewCtgr,
            recyclerSearch = binding.recyclerSearch,
            emptyText = binding.emptyText,
        )
    }

    private fun setupListeners() {
        binding.viewCtgrLayout.viewTreeObserver.addOnGlobalLayoutListener {
            adjustRecyclerViewVisibility()
        }
    }

    private fun adjustRecyclerViewVisibility() {
        if (binding.keyword.text!!.isNotEmpty()) {
            binding.recyclerSearch.visibility = View.VISIBLE
            binding.recyclerViewCtgr.visibility = View.INVISIBLE
        } else {
            binding.recyclerViewCtgr.visibility = View.VISIBLE
            binding.recyclerSearch.visibility = View.INVISIBLE
            binding.emptyText.visibility = View.INVISIBLE
        }
    }

    private fun setDefaultCtgr() {
        val ctgrList = mutableListOf(Ctgr(0, "미분류")).apply {
            addAll(helper.selectCtgrList())
            add(Ctgr(null, "+"))
            add(Ctgr(MemoStatus.DELETED.code, "휴지통"))
        }

        ctgrAdapter.listData = ctgrList
        ctgrAdapter.notifyDataSetChanged()
    }

    override fun onRestart() {
        super.onRestart()
        setDefaultCtgr()
    }

    private fun loadData() {
        val data = helper.selectSearchList(keyword, where, orderby)
        searchMemoAdapter.listData.clear()
        searchMemoAdapter.listData.addAll(data)
        if (data.isEmpty()) {
            binding.recyclerSearch.visibility = View.INVISIBLE
            binding.emptyText.visibility = View.VISIBLE
        } else {
            binding.recyclerSearch.visibility = View.VISIBLE
            binding.emptyText.visibility = View.INVISIBLE
        }
        searchMemoAdapter.notifyDataSetChanged()
        binding.recyclerSearch.adapter = searchMemoAdapter
    }


    override fun fragmentOpen(item: String, ctgrIdx: String?) {
        when (item) {
            "+" -> CtgrAddFragment(this).show(supportFragmentManager, "CtgrAdd")
            "delete@#" -> {
                val ctgrDeleteFragment = CtgrDeleteFragment(this).apply {
                    arguments = Bundle().apply {
                        putString("ctgrIdx", ctgrIdx)
                    }
                }
                ctgrDeleteFragment.show(supportFragmentManager, "DeleteFragment")
            }
        }
    }

    override fun addCtgr(ctgrName: String) {
        if (ctgrName.isValidCtgrName()) {
            if (!helper.checkDuplicationCtgr(ctgrName)) {
                helper.insertCtgr(Ctgr(null, ctgrName))
                setDefaultCtgr()
            } else {
                showToast("이미 사용중 입니다")
            }
        } else {
            showToast("사용할 수 없는 이름입니다")
        }
    }

    private fun String.isValidCtgrName() = !listOf("미분류", "delete@#", "+", "휴지통").contains(this)

    override fun deleteCtgr(ctgrIdx: String) {
        helper.deleteCtgr(ctgrIdx)
        setDefaultCtgr()
    }

    override fun moveCtgrList(oldCtgrIdx: Int, newCtgrIdx: Int) {
        helper.selectMemoList(oldCtgrIdx.toString()).sortedBy { it.priority }.forEach {
            helper.updateMemoCtgr(it.idx, oldCtgrIdx, newCtgrIdx)
        }
        deleteCtgr(oldCtgrIdx.toString())
    }

    override fun openKeyBoard(view: View) {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
            view,
            0
        )
    }

    override fun closeKeyBoard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            currentFocus?.windowToken,
            0
        )
    }
}
