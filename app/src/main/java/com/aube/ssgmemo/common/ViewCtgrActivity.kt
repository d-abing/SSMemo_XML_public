package com.aube.ssgmemo.common

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Vibrator
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import com.aube.ssgmemo.Ctgr
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.adapter.SearchMemoAdapter
import com.aube.ssgmemo.adapter.ViewCtgrAdapter
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.databinding.ActivityViewCtgrBinding
import com.aube.ssgmemo.etc.MemoStatus
import com.aube.ssgmemo.etc.MyApplication
import com.aube.ssgmemo.fragment.CtgrAddFragment
import com.aube.ssgmemo.fragment.CtgrDeleteFragment
import io.github.muddz.styleabletoast.StyleableToast

class ViewCtgrActivity : AppCompatActivity(), CallbackListener {
    private lateinit var binding: ActivityViewCtgrBinding
    private val helper = SqliteHelper(this, "ssgMemo", 1)

    private var darkmode = MyApplication.prefs.getInt("darkmode", 16)

    private lateinit var ctgrAdapter: ViewCtgrAdapter

    private var keyword = ""
    private var where = "제목+내용"
    private var orderby = "최신순"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewCtgrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        //initializeAds()
    }

    private fun setupUI() {
        applyDarkMode()
        setupCategoryRecyclerView()
        setupSearchConditions()
    }

    private fun applyDarkMode() {
        if (darkmode == 32) {
            binding.viewCtgrLayout.setBackgroundColor(Color.BLACK)
            binding.adView.setBackgroundColor(Color.BLACK)
            binding.searchBar.setBackgroundResource(R.drawable.graysearchbar2)
            binding.btnFilter.setImageResource(R.drawable.grayfilter2)
        }
    }

    private fun setupCategoryRecyclerView() {
        ctgrAdapter = ViewCtgrAdapter(this)
        ctgrAdapter.helper = helper
        ctgrAdapter.vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        setDefaultCtgr()
        binding.recyclerViewCtgr.apply {
            adapter = ctgrAdapter
            layoutManager =
                GridLayoutManager(this@ViewCtgrActivity, 2)
        }
    }

    private fun setupSearchConditions() {
        setupSpinner(binding.conditionContain, listOf("제목+내용", "제목", "내용")) {
            where = it
            refreshSearchResults()
        }

        setupSpinner(binding.conditionLatest, listOf("최신순", "오래된순")) {
            orderby = it
            refreshSearchResults()
        }

        binding.keyword.doOnTextChanged { _, _, _, _ ->
            keyword = binding.keyword.text.toString()
            refreshSearchResults()
        }
    }

    private fun setupSpinner(
        spinner: AdapterView<*>,
        items: List<String>,
        onItemSelected: (String) -> Unit
    ) {
        spinner.adapter = ArrayAdapter(this, R.layout.item_condition_spinner, items)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                onItemSelected(items[position])
            }
        }
    }

    private fun refreshSearchResults() {
        val searchMemoAdapter = SearchMemoAdapter(this)
        showDataList(searchMemoAdapter, keyword, where, orderby)
        binding.recyclerSearch.adapter = searchMemoAdapter
    }

    private fun showDataList(
        recyclerAdapter: SearchMemoAdapter,
        keyword: String,
        where: String,
        orderby: String
    ) {
        val data = helper.selectSearchList(keyword, where, orderby)
        recyclerAdapter.listData.clear()
        recyclerAdapter.listData.addAll(data)
        if (data.isEmpty()) {
            binding.recyclerSearch.visibility = View.INVISIBLE
            binding.emptyText.visibility = View.VISIBLE
        } else {
            binding.recyclerSearch.visibility = View.VISIBLE
            binding.emptyText.visibility = View.INVISIBLE
        }
        recyclerAdapter.notifyDataSetChanged()
    }

    private fun setupListeners() {
        binding.btnFilter.setOnClickListener {
            toggleFilterOptions()
        }

        binding.viewCtgrLayout.viewTreeObserver.addOnGlobalLayoutListener {
            adjustRecyclerViewVisibility()
        }
    }

    private fun toggleFilterOptions() {
        val isVisible = binding.conditionLatest.visibility == View.VISIBLE
        val visibility = if (isVisible) View.GONE else View.VISIBLE
        binding.conditionLatest.visibility = visibility
        binding.conditionContain.visibility = visibility
        val marginTop = if (isVisible) 15F else 60F
        updateMargins(marginTop)
    }

    private fun updateMargins(marginTop: Float) {
        binding.recyclerViewCtgr.updateMargin(top = marginTop)
        binding.recyclerSearch.updateMargin(top = marginTop)
        binding.emptyText.updateMargin(top = marginTop)
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

    /*private fun initializeAds() {
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }*/

    private fun setDefaultCtgr() {
        val ctgrList = mutableListOf(
            Ctgr(0, "미분류")
        ).apply {
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

    private fun showToast(message: String) {
        StyleableToast.makeText(applicationContext, message, R.style.toast).show()
    }

    override fun deleteCtgr(ctgrIdx: String) {
        helper.deleteCtgr(ctgrIdx)
        setDefaultCtgr()
    }

    override fun moveCtgrList(oldCtgrIdx: Int, newCtgrIdx: Int) {
        helper.selectMemoList(oldCtgrIdx.toString())
            .sortedBy { it.priority }
            .forEach { helper.updateMemoCtgr(it.idx, oldCtgrIdx, newCtgrIdx) }
        deleteCtgr(oldCtgrIdx.toString())
    }

    override fun openKeyBoard(view: View) {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(view, 0)
    }

    override fun closeKeyBoard() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }


    private fun View.updateMargin(
        left: Float? = null,
        top: Float? = null,
        right: Float? = null,
        bottom: Float? = null
    ) {
        layoutParams<ViewGroup.MarginLayoutParams> {
            left?.let { leftMargin = context.dpToPx(it) }
            top?.let { topMargin = context.dpToPx(it) }
            right?.let { rightMargin = context.dpToPx(it) }
            bottom?.let { bottomMargin = context.dpToPx(it) }
        }
    }

    private inline fun <reified T : ViewGroup.LayoutParams> View.layoutParams(block: T.() -> Unit) {
        if (layoutParams is T) block(layoutParams as T)
    }

    private fun Context.dpToPx(dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
}
