package com.aube.ssgmemo.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.adapter.RecyclerSwipeAdapter
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.callback.ItemTouchHelperCallback
import com.aube.ssgmemo.databinding.ActivityViewMemoBinding
import com.aube.ssgmemo.etc.MemoStatus
import com.aube.ssgmemo.etc.ModeStatus
import com.aube.ssgmemo.etc.MyApplication
import com.aube.ssgmemo.fragment.CompleteFragment
import com.aube.ssgmemo.fragment.MemoDeleteFragment
import com.aube.ssgmemo.fragment.MemoMoveFragment
import io.github.muddz.styleabletoast.StyleableToast

class ViewMemoActivity : AppCompatActivity(), CallbackListener {
    private lateinit var binding: ActivityViewMemoBinding
    private val helper = SqliteHelper(this, "ssgMemo", 1)

    private var darkmode = MyApplication.prefs.getInt("darkmode", 16)

    private lateinit var itemTouchHelperCallback: ItemTouchHelperCallback
    private lateinit var viewMemoAdapter: RecyclerSwipeAdapter
    private var ctgrIdx: Int = 0
    private var isSelectMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDarkMode()
        initializeVariables()
        setupRecyclerView()
        setupButtons()
        //loadAds()
    }

    private fun setupDarkMode() {
        if (darkmode == 32) {
            binding.viewMemoLayout.setBackgroundColor(Color.BLACK)
            binding.adView.setBackgroundColor(Color.BLACK)
            binding.ctgrTitle.setTextColor(Color.WHITE)
        }
    }

    private fun initializeVariables() {
        ctgrIdx = intent.getIntExtra("idx", 0)
        binding.ctgrTitle.text = intent.getStringExtra("ctgrname")
        viewMemoAdapter = RecyclerSwipeAdapter(this).apply {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            helper = this@ViewMemoActivity.helper
            callbackListener = this@ViewMemoActivity
            itemList = helper.selectMemoList(ctgrIdx.toString())
        }
        itemTouchHelperCallback = ItemTouchHelperCallback(viewMemoAdapter).apply {
            setClamp(150f)
            if (ctgrIdx == MemoStatus.DELETED.code) {
                setMode(MemoStatus.DELETED.code)
            }
        }
        adjustVisibility()
    }

    private fun adjustVisibility() {
        if (viewMemoAdapter.itemList.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            binding.selectBtn.visibility = View.INVISIBLE
        } else {
            binding.emptyText.visibility = View.INVISIBLE
            binding.selectBtn.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        val dividerItemDecoration = DividerItemDecoration(
            binding.recyclerMemo.context,
            LinearLayoutManager(this).orientation
        )
        binding.recyclerMemo.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = this@ViewMemoActivity.viewMemoAdapter
            addItemDecoration(dividerItemDecoration)
            setOnTouchListener { _, _ ->
                itemTouchHelperCallback.removePreviousClamp(this)
                false
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerMemo)
    }

    private fun setupButtons() {
        binding.selectBtn.setOnClickListener { toggleMode() }
        binding.addMemo.setOnClickListener { navigateToWriteActivity() }
        binding.selectAll.setOnClickListener { toggleSelectAll() }
        binding.moveSelected.setOnClickListener { handleButton(::moveFragmentOpen) }
        binding.completeSelected.setOnClickListener { handleButton(::completeFragmentOpen) }
        binding.deleteSelected.setOnClickListener { handleButton(::deleteFragmentOpen) }
    }

    private fun toggleMode() {
        viewMemoAdapter.selectedList.clear()
        isSelectMode = !isSelectMode
        viewMemoAdapter.mode =
            if (isSelectMode) ModeStatus.SELECT.code else ModeStatus.COMPLETE.code
        changeListener(viewMemoAdapter.mode)
        animateBottom(viewMemoAdapter.mode)
        animateAllItems(viewMemoAdapter.mode)
        viewMemoAdapter.selectAll = false
    }

    private fun navigateToWriteActivity() {
        val ctgrName = intent.getStringExtra("ctgrname")
        val intent = Intent(this, WriteActivity::class.java)
        intent.putExtra("ctgrname", "$ctgrName")
        startActivity(intent)
    }

    private fun toggleSelectAll() {
        viewMemoAdapter.apply {
            selectAll = !selectAll
            for (memo in itemList) {
                memo.isSelected = selectAll
            }
            selectedList = if (selectAll) {
                itemList.toMutableList()
            } else {
                mutableListOf()
            }
            notifyDataSetChanged()
        }
    }

    private fun handleButton(openFragment: (Int?) -> Unit) {
        when {
            viewMemoAdapter.selectedList.isEmpty() -> {
                StyleableToast.makeText(this, "선택된 값이 없습니다", R.style.toast).show()
            }

            else -> {
                openFragment(ctgrIdx)
            }
        }
    }

    /*private fun loadAds() {
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
    }*/

    override fun onBackPressed() {
        if (isSelectMode) {
            toggleMode()
        } else {
            finish()
        }
    }

    override fun onRestart() {
        super.onRestart()
        refreshRecyclerView()
    }

    private fun refreshRecyclerView() {
        binding.recyclerMemo.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = this@ViewMemoActivity.viewMemoAdapter
            itemTouchHelperCallback.removePreviousClamp(this)
            adjustVisibility()
        }
        viewMemoAdapter.itemList = helper.selectMemoList(ctgrIdx.toString())
        viewMemoAdapter.notifyDataSetChanged()
    }

    override fun completeFragmentOpen(memoIdx: Int) {
        val completeFragment = CompleteFragment(this)
        val bundle = Bundle().apply {
            putInt("memoIdx", memoIdx)
        }
        completeFragment.arguments = bundle
        completeFragment.show(supportFragmentManager, "memoComplete")
    }

    private fun moveFragmentOpen(ctgrIdx: Int?) {
        val moveFragment = MemoMoveFragment(this)
        val bundle = Bundle().apply {
            putInt("ctgrIdx", ctgrIdx!!)
        }
        moveFragment.helper = helper
        moveFragment.arguments = bundle
        moveFragment.show(supportFragmentManager, "memoMove")
    }

    private fun completeFragmentOpen(ctgrIdx: Int?) {
        val completeFragment = CompleteFragment(this)
        completeFragment.show(supportFragmentManager, "memoMove")
    }

    private fun deleteFragmentOpen(ctgrIdx: Int?) {
        val deleteFragment = MemoDeleteFragment(this)
        val bundle = Bundle().apply {
            putInt("ctgrIdx", ctgrIdx!!)
        }
        deleteFragment.arguments = bundle
        deleteFragment.show(supportFragmentManager, "memoDelete")
    }

    override fun moveCtgrList(oldCtgrIdx: Int, newCtgrIdx: Int) {
        val sortedList = viewMemoAdapter.selectedList.sortedBy { it.priority }
        for (memo in sortedList) {
            helper.updateMemoCtgr(memo.idx, oldCtgrIdx, newCtgrIdx)
        }
        refreshRecyclerView()
    }

    override fun deleteMemoList() {
        for (selectedMemo in viewMemoAdapter.selectedList) {
            helper.deleteMemo(selectedMemo)
        }
        refreshRecyclerView()
    }

    override fun completeMemo(memoIdx: Int) {
        super.completeMemo(memoIdx)
        val memo = viewMemoAdapter.helper.selectMemo(memoIdx.toString())
        viewMemoAdapter.helper.updateMemoStatus(memo, MemoStatus.COMPLETED.code)
        refreshRecyclerView()
    }

    override fun completeMemoList() {
        for (selectedMemo in viewMemoAdapter.selectedList) {
            completeMemo(selectedMemo.idx!!)
        }
    }

    private fun changeListener(mode: Int) {
        for (i in 0 until binding.recyclerMemo.childCount) {
            val viewHolder =
                binding.recyclerMemo.getChildViewHolder(binding.recyclerMemo.getChildAt(i))
            val memoItem = viewHolder.itemView.findViewById<ConstraintLayout>(R.id.memoItem)
            val toggleButton = viewHolder.itemView.findViewById<RadioButton>(R.id.toggleButton)

            if (mode == ModeStatus.COMPLETE.code) {
                viewMemoAdapter.itemList[i].isSelected = false
                toggleButton.isChecked = false
                memoItem.setOnClickListener {
                    val intent = Intent(this, EditActivity::class.java)
                    intent.putExtra("memoIdx", "${viewMemoAdapter.itemList[i].idx}")
                    startActivity(intent)
                }
            } else if (mode == ModeStatus.SELECT.code) {
                memoItem.setOnClickListener {
                    toggleButton.isChecked = !viewMemoAdapter.itemList[i].isSelected
                    if (viewMemoAdapter.itemList[i].isSelected) {
                        viewMemoAdapter.selectedList.remove(viewMemoAdapter.itemList[i])
                    } else {
                        viewMemoAdapter.selectedList.add(viewMemoAdapter.itemList[i])
                    }
                    viewMemoAdapter.itemList[i].isSelected = !viewMemoAdapter.itemList[i].isSelected
                    viewMemoAdapter.selectAll =
                        viewMemoAdapter.selectedList.size == viewMemoAdapter.itemList.size
                }
            }
        }
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun animateBottom(mode: Int) {
        val translationY = if (mode == ModeStatus.COMPLETE.code) 250f else 0f
        ObjectAnimator.ofFloat(binding.selectLayout, "translationY", translationY).apply { start() }
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun animateAllItems(mode: Int) {
        for (i in 0 until binding.recyclerMemo.childCount) {
            val viewHolder =
                binding.recyclerMemo.getChildViewHolder(binding.recyclerMemo.getChildAt(i))
            val item = viewHolder.itemView.findViewById<ConstraintLayout>(R.id.memoItem)
            val completeButton = viewHolder.itemView.findViewById<ImageView>(R.id.btnComplete)
            val toggleButton = viewHolder.itemView.findViewById<RadioButton>(R.id.toggleButton)

            if (mode == ModeStatus.COMPLETE.code) {
                itemTouchHelperCallback.setMode(ModeStatus.COMPLETE.code)
                ObjectAnimator.ofFloat(item, "translationX", 0f).apply {
                    start()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            completeButton.visibility = View.VISIBLE
                            toggleButton.visibility = View.GONE
                        }
                    })
                }
            } else if (mode == ModeStatus.SELECT.code) {
                completeButton.visibility = View.GONE
                toggleButton.visibility = View.VISIBLE
                itemTouchHelperCallback.setMode(ModeStatus.SELECT.code)
                ObjectAnimator.ofFloat(item, "translationX", 150f).apply { start() }
            }
        }
    }
}
