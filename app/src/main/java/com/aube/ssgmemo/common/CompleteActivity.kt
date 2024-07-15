package com.aube.ssgmemo.common

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.adapter.CompleteMemoAdapter
import com.aube.ssgmemo.databinding.ActivityCompleteBinding
import com.aube.ssgmemo.etc.MyApplication

class CompleteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCompleteBinding
    private val helper = SqliteHelper(this, "ssgMemo", 1)
    private var darkmode = MyApplication.prefs.getInt("darkmode", 16)

    private val completeMemoAdapter = CompleteMemoAdapter(this)
    private var where = "제목+내용"          // sql where 조건
    private var orderby = "최신순"          // sql orderby 조건
    private var keyword = ""               // sql where의 keyword

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompleteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDarkMode()
        setupRecyclerView()
        setupSearchBar()
        setupSpinners()
        setupFilterButton()
        //setupAds()
    }

    private fun setupDarkMode() {
        if (darkmode == 32) {
            binding.searchLayout.setBackgroundColor(Color.BLACK)
            binding.adView.setBackgroundColor(Color.BLACK)
            binding.searchBar.setBackgroundResource(R.drawable.graysearchbar2)
            binding.btnFilter.setImageResource(R.drawable.grayfilter2)
        }
    }

    private fun setupRecyclerView() {
        val dividerItemDecoration = DividerItemDecoration(
            binding.recyclerComplete.context,
            LinearLayoutManager(this).orientation
        )
        binding.recyclerComplete.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = completeMemoAdapter
            addItemDecoration(dividerItemDecoration)
        }
        loadData()
    }

    private fun setupSearchBar() {
        binding.keyword.doOnTextChanged { _, _, _, _ ->
            keyword = binding.keyword.text.toString()
            reloadData()
        }
    }

    private fun setupSpinners() {
        val conditionList1 = listOf("제목+내용", "제목", "내용")
        val conditionList2 = listOf("최신순", "오래된순")

        binding.conditionContain.apply {
            adapter = ArrayAdapter(context, R.layout.item_condition_spinner, conditionList1)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    where = conditionList1[position]
                    reloadData()
                }
            }
        }

        binding.conditionLatest.apply {
            adapter = ArrayAdapter(context, R.layout.item_condition_spinner, conditionList2)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    orderby = conditionList2[position]
                    reloadData()
                }
            }
        }
    }

    private fun setupFilterButton() {
        var isFilterVisible = false
        binding.btnFilter.setOnClickListener {
            isFilterVisible = !isFilterVisible
            val visibility = if (isFilterVisible) View.VISIBLE else View.GONE
            binding.conditionContain.visibility = visibility
            binding.conditionLatest.visibility = visibility
            val topMargin = if (isFilterVisible) 60F else 15F
            binding.recyclerComplete.updateTopMargin(topMargin)
            binding.emptyText.updateTopMargin(topMargin)
        }
    }

    /*private fun setupAds() {
        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }*/

    override fun onRestart() {
        super.onRestart()
        reloadData()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        return true
    }

    private fun loadData() {
        val data = helper.selectCompleteList(keyword, where, orderby)
        completeMemoAdapter.listData.clear()
        completeMemoAdapter.listData.addAll(data)
        binding.emptyText.visibility = if (data.isEmpty()) View.VISIBLE else View.INVISIBLE
        completeMemoAdapter.notifyDataSetChanged()
    }

    private fun reloadData() {
        completeMemoAdapter.listData.clear()
        loadData()
    }

    private fun View.updateTopMargin(top: Float) {
        layoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = context.dpToPx(top)
        }
    }

    inline fun <reified T : ViewGroup.LayoutParams> View.layoutParams(block: T.() -> Unit) {
        if (layoutParams is T) block(layoutParams as T)
    }

    fun Context.dpToPx(dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()
}
