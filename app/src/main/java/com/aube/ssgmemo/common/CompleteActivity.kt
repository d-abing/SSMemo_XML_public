package com.aube.ssgmemo.common

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.adapter.CompleteMemoAdapter
import com.aube.ssgmemo.databinding.ActivityCompleteBinding

class CompleteActivity : BaseSearchActivity() {
    private lateinit var binding: ActivityCompleteBinding
    private val helper = SqliteHelper(this, "ssgMemo", 1)

    private val completeMemoAdapter = CompleteMemoAdapter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompleteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDarkMode()
        setupCompleteRecyclerView()
        setupSearchBar(binding.keyword) { loadData() }
        setupSpinners()
        setupFilterButtons()
        //setupAds(binding.adView)
    }

    private fun setupDarkMode() {
        super.setupDarkMode(binding.completeLayout)
        if (darkmode == 32) {
            binding.adView.setBackgroundColor(Color.BLACK)
            binding.searchBar.setBackgroundResource(R.drawable.graysearchbar2)
            binding.btnFilter.setImageResource(R.drawable.grayfilter2)
        }
    }

    private fun setupCompleteRecyclerView() {
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
            recyclerSearch = binding.recyclerComplete,
            emptyText = binding.emptyText,
        )
    }

    override fun onRestart() {
        super.onRestart()
        loadData()
    }

    private fun loadData() {
        val data = helper.selectCompleteList(keyword, where, orderby)
        completeMemoAdapter.listData.clear()
        completeMemoAdapter.listData.addAll(data)
        binding.emptyText.visibility = if (data.isEmpty()) View.VISIBLE else View.INVISIBLE
        completeMemoAdapter.notifyDataSetChanged()
    }
}

