package com.aube.ssgmemo.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.widget.ImageButton
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.aube.ssgmemo.Memo
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.adapter.RecyclerSwipeAdapter
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.callback.ItemTouchHelperCallback
import com.aube.ssgmemo.databinding.ActivityViewMemoBinding
import com.aube.ssgmemo.etc.MyApplication
import com.aube.ssgmemo.fragment.CompleteFragment
import com.aube.ssgmemo.fragment.MemoDeleteFragment
import com.aube.ssgmemo.fragment.MemoMoveFragment
import io.github.muddz.styleabletoast.StyleableToast

class ViewMemoActivity : AppCompatActivity(), CallbackListener {
    private lateinit var binding: ActivityViewMemoBinding
    private val helper = SqliteHelper(this, "ssgMemo", 1)

    private var darkmode = MyApplication.prefs.getString("darkmode", "0")

    private lateinit var itemTouchHelperCallback: ItemTouchHelperCallback
    private lateinit var adapter: RecyclerSwipeAdapter
    private lateinit var title: String
    private var modeChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 다크모드 설정
        if (darkmode.equals("32")) {
            binding.viewMemoLayout.setBackgroundColor(Color.BLACK)
            binding.adView.setBackgroundColor(Color.BLACK)
            binding.addMemo.setImageResource(R.drawable.add1)
        }

        // dpi 값에 맞게 layoutParams 조절
        val dpi = resources.displayMetrics.densityDpi

        val rect = Rect()
        binding.viewMemoLayout.getWindowVisibleDisplayFrame(rect)

        dpiLayoutParams(binding.recyclerContent1, (rect.width() * 0.9).toInt())
        dpiLayoutParams(binding.selectLayout, rect.width())


        // 메모 제목 초기화
        title = intent.getStringExtra("idx").toString()

        // 변수선언
        val ctgrName = intent.getStringExtra("ctgrname")
        val memoList = helper.selectMemoList(title)
        val dividerItemDecoration = DividerItemDecoration(
            binding.recyclerContent1.context,
            LinearLayoutManager(this).orientation
        )
        val display = this.applicationContext?.resources?.displayMetrics
        val deviceHeight = display?.heightPixels
        val layoutParams1 = binding.recyclerContent1.layoutParams


        // 어뎁터 초기화
        adapter = RecyclerSwipeAdapter(this)
        adapter.vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        adapter.helper = helper
        adapter.callbackListener = this
        adapter.itemList = helper.selectMemoList(title)

        // 리사이클러뷰 어뎁터 붙이기
        binding.recyclerContent1.adapter = adapter

        // 터치 콜백리스터
        itemTouchHelperCallback = ItemTouchHelperCallback(adapter)
        itemTouchHelperCallback.setClamp(150f)

        // 터치 콜백리스너 리사이클러뷰에 붙이기
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerContent1)
        binding.recyclerContent1.addItemDecoration(dividerItemDecoration)
        binding.ctgrTitle.text = ctgrName

        // 레이아웃 초기화
        layoutParams1.height = deviceHeight?.times(0.82)!!.toInt()
        binding.recyclerContent1.layoutParams = layoutParams1

        if (adapter.itemList.isEmpty()) {
            binding.msgText.visibility = View.VISIBLE
            binding.selectBtn.visibility = View.INVISIBLE
        }
        if (ctgrName == "휴지통") {
            itemTouchHelperCallback.setMode(1)
        }

        // 클램프 지정 초기화
        binding.recyclerContent1.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = adapter
            setOnTouchListener { _, _ ->
                itemTouchHelperCallback.removePreviousClamp(this)
                false
            }
        }

        // 선택 버튼 리스너
        binding.selectBtn.setOnClickListener {
            adapter.selectedList.clear()
            if (!modeChange) {
                btnChange(adapter.mode)
                animateBottom(adapter.mode)
                animateAllItems(adapter.mode)
                adapter.mode = 1
                adapter.selectAll = false
                modeChange = true
            } else {
                btnChange(adapter.mode)
                animateBottom(adapter.mode)
                animateAllItems(adapter.mode)
                adapter.mode = 0
                adapter.selectAll = false
                modeChange = false
            }
        }

        binding.addMemo.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            intent.putExtra("ctgrname", "$ctgrName")
            startActivity(intent)
        }

        // 전체선택 버튼 리스너
        binding.selectAll.setOnClickListener {
            adapter.selectAll = !adapter.selectAll
            if (adapter.selectAll) {
                for (i in 0 until binding.recyclerContent1.childCount) {
                    val viewHolder = binding.recyclerContent1.getChildViewHolder(
                        binding.recyclerContent1.getChildAt(i)
                    )
                    val toggleButton =
                        viewHolder.itemView.findViewById<RadioButton>(R.id.toggleButton)
                    toggleButton.isChecked = true
                    adapter.itemList[i].sel = true
                }
                adapter.selectedList = memoList
            } else {
                for (i in 0 until binding.recyclerContent1.childCount) {
                    val viewHolder = binding.recyclerContent1.getChildViewHolder(
                        binding.recyclerContent1.getChildAt(i)
                    )
                    val toggleButton =
                        viewHolder.itemView.findViewById<RadioButton>(R.id.toggleButton)
                    toggleButton.isChecked = false
                    adapter.itemList[i].sel = false
                }
                adapter.selectedList.clear()
            }
        }

        // 삭제 버튼 리스너
        binding.deleteSelected.setOnClickListener {
            if (adapter.selectedList.size == 1) {
                val memoIdx = adapter.selectedList[0].idx.toString()
                fragmentOpen(title, memoIdx, false)
            } else if (adapter.selectedList.isEmpty()) {
                StyleableToast.makeText(this, "선택된 값이 없습니다", R.style.toast).show()
            } else {
                fragmentOpen(title, adapter.selectedList[0].idx.toString(), true)
            }
        }

        // 이동 버튼 리스너
        binding.moveSelected.setOnClickListener {
            if (adapter.selectedList.size == 1) {
                val memoIdx = adapter.selectedList[0].idx.toString()
                fragmentOpen(title, memoIdx, false, 1)
            } else if (adapter.selectedList.isEmpty()) {
                StyleableToast.makeText(this, "선택된 값이 없습니다", R.style.toast).show()
            } else {
                fragmentOpen(title, adapter.selectedList[0].ctgr.toString(), true, 1)
            }
        }

        if (ctgrName.equals("휴지통")) {
            binding.moveSelected.text = "복원"
            binding.addMemo.visibility = View.GONE
        }
    }

    fun dpiLayoutParams(view: View, width: Int) {
        val tmp_layoutParams = view.layoutParams
        tmp_layoutParams.width = width
        view.layoutParams = tmp_layoutParams
    }

    override fun onBackPressed() {
        if (modeChange) {
            btnChange(adapter.mode)
            animateBottom(adapter.mode)
            animateAllItems(adapter.mode)
            adapter.mode = 0
            adapter.selectAll = false
            modeChange = false
        } else {
            finish()
        }
    }

    override fun onRestart() {
        super.onRestart()
        // 메모 삭제시 clamp 되어있음
        // 메모 들어갔다가 나오면 삭제 클릭 리스너 클릭 불가
        binding.recyclerContent1.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = adapter
            itemTouchHelperCallback.removePreviousClamp(this)
        }
        adapter.itemList = helper.selectMemoList(title)
        adapter.notifyDataSetChanged()
    }

    // 프레그먼트 오프너 삭제 시
    override fun fragmentOpen(ctgrIdx: String, memoIdx: String, isList: Boolean) {
        super.fragmentOpen(ctgrIdx, memoIdx, isList)
        val deleteFragment = MemoDeleteFragment(this)
        val bundle = Bundle()
        bundle.putString("ctgrIdx", ctgrIdx)
        bundle.putString("memoIdx", memoIdx)
        bundle.putBoolean("isList", isList)
        deleteFragment.arguments = bundle
        deleteFragment.show(supportFragmentManager, "memoDelete")
    }

    // 프레그먼트 오프너 완료시
    override fun fragmentOpen(memoIdx: Int) {
        super.fragmentOpen(memoIdx)
        val completeFragment = CompleteFragment(this)
        val bundle = Bundle()
        bundle.putInt("memoIdx", memoIdx)
        completeFragment.arguments = bundle
        completeFragment.show(supportFragmentManager, "memoComplete")
    }

    // 프레그먼트 오프너 이동 시
    fun fragmentOpen(ctgrIdx: String, memoIdx: String, isList: Boolean, move: Int) {
        super.fragmentOpen(ctgrIdx, memoIdx, isList)
        val moveFragment = MemoMoveFragment(this)
        val bundle = Bundle()
        moveFragment.helper = helper
        bundle.putString("ctgrIdx", ctgrIdx)
        bundle.putString("memoIdx", memoIdx)
        bundle.putBoolean("isList", isList)
        moveFragment.arguments = bundle
        moveFragment.show(supportFragmentManager, "memoMove")
    }

    // 메모 삭제
    override fun deleteMemo(memoIdx: String) {
        super.deleteMemo(memoIdx)
        val memo: Memo = helper.selectMemo(memoIdx)
        helper.deleteMemo(memo)
        adapter.itemList = helper.selectMemoList(memo.ctgr.toString())
        if (adapter.itemList.isEmpty()) {
            binding.msgText.visibility = View.VISIBLE
        }
        binding.recyclerContent1.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = adapter
            itemTouchHelperCallback.removePreviousClamp(this)
        }
        adapter.notifyDataSetChanged()
    }

    // 메모 삭제 리스트인 경우
    override fun deleteMemoList() {
        for (selectedList in adapter.selectedList) {
            helper.deleteMemo(selectedList)
        }
        adapter.itemList = helper.selectMemoList(title)
        if (adapter.itemList.isEmpty()) {
            binding.msgText.visibility = View.VISIBLE
        }
        adapter.notifyDataSetChanged()
    }

    // 메모 ctgr 삭제
    override fun deleteCtgr(memoIdx: String) {
        super.deleteCtgr(memoIdx)
        val memo: Memo = helper.selectMemo(memoIdx)
        helper.deleteMemoCtgr(memoIdx)
        adapter.itemList = helper.selectMemoList(memo.ctgr.toString())
        if (adapter.itemList.isEmpty()) {
            binding.msgText.visibility = View.VISIBLE
        }
        binding.recyclerContent1.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = adapter
            itemTouchHelperCallback.removePreviousClamp(this)
        }
        adapter.notifyDataSetChanged()
    }

    // 메모 ctgr 삭제 리스트인 경우
    override fun deleteCtgrList() {
        super.deleteCtgrList()
        var sortedList = adapter.selectedList.sortedBy { it.priority }
        val ctgrIdx = adapter.selectedList.get(0).ctgr
        for (list in sortedList) {
            helper.deleteMemoCtgr(list.idx.toString())
        }
        helper.sortPriority(ctgrIdx)
        if (helper.selectMemoList(title).isEmpty()) {
            binding.msgText.visibility = View.VISIBLE
        }
        adapter.itemList = helper.selectMemoList(title)
        binding.recyclerContent1.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = adapter
            itemTouchHelperCallback.removePreviousClamp(this)
        }
        adapter.notifyDataSetChanged()
    }

    // 메모 ctgr 이동
    override fun moveCtgr(memoIdx: Int?, ctgrIdx: Int, status: Int) {
        super.moveCtgr(memoIdx, ctgrIdx, status)
        val memo: Memo = helper.selectMemo(memoIdx.toString())
        helper.updateMemoCtgr(memoIdx, memo.ctgr, ctgrIdx)
        adapter.itemList = helper.selectMemoList(memo.ctgr.toString())
        if (adapter.itemList.isEmpty()) {
            binding.msgText.visibility = View.VISIBLE
        }
        binding.recyclerContent1.apply {
            layoutManager = LinearLayoutManager(applicationContext)
            adapter = adapter
            itemTouchHelperCallback.removePreviousClamp(this)
        }
        adapter.notifyDataSetChanged()
    }

    // 메모 ctgr 이동 리스트인 경우
    override fun moveCtgrList(oldCtgrIdx: Int, newCtgrIdx: Int) {
        var sortedList = adapter.selectedList.sortedBy { it.priority }
        for (memo in sortedList) {
            helper.updateMemoCtgr(memo.idx, oldCtgrIdx, newCtgrIdx)
        }
        adapter.itemList = helper.selectMemoList(title)
        if (adapter.itemList.isEmpty()) {
            binding.msgText.visibility = View.VISIBLE
        }
        adapter.notifyDataSetChanged()
    }

    // 메모 완료시
    override fun completeMemo(memoIdx: Int) {
        super.completeMemo(memoIdx)
        var memo = adapter.helper.selectMemo(memoIdx.toString())
        adapter.helper.updateMemoStatus(memo, 2)
        adapter.itemList = helper.selectMemoList(title)
        if (adapter.itemList.isEmpty()) {
            binding.msgText.visibility = View.VISIBLE
        }
        adapter.notifyDataSetChanged()
    }

    // 애니메이션 선택모드 시
    @SuppressLint("ObjectAnimatorBinding")
    private fun animateAllItems(mode: Int) {
        for (i in 0 until binding.recyclerContent1.childCount) {
            val viewHolder =
                binding.recyclerContent1.getChildViewHolder(binding.recyclerContent1.getChildAt(i))
            val item = viewHolder.itemView.findViewById<ConstraintLayout>(R.id.memoItem)
            val selectbtn = viewHolder.itemView.findViewById<ImageButton>(R.id.btnComplete)
            val toggleButton = viewHolder.itemView.findViewById<RadioButton>(R.id.toggleButton)

            if (mode == 0) {
                selectbtn.visibility = View.GONE
                toggleButton.visibility = View.VISIBLE
                itemTouchHelperCallback.setMode(1)
                ObjectAnimator.ofFloat(item, "translationX", 150f).apply {
                    start()
                }
            } else {
                itemTouchHelperCallback.setMode(0)
                ObjectAnimator.ofFloat(item, "translationX", 0f).apply {
                    start()
                    addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            selectbtn.visibility = View.VISIBLE
                            toggleButton.visibility = View.GONE
                        }
                    })
                }
            }
        }
    }

    // 애니메이션 선택모드 시 하단 바
    @SuppressLint("ObjectAnimatorBinding")
    private fun animateBottom(mode: Int) {
        if (mode == 0) {
            ObjectAnimator.ofFloat(binding.selectLayout, "translationY", 0f).apply {
                start()
            }
        } else {
            ObjectAnimator.ofFloat(binding.selectLayout, "translationY", 250f).apply {
                start()
            }
        }
    }

    // 리스너 변경
    private fun btnChange(mode: Int) {
        for (i in 0 until binding.recyclerContent1.childCount) {
            val viewHolder =
                binding.recyclerContent1.getChildViewHolder(binding.recyclerContent1.getChildAt(i))
            val memoItem = viewHolder.itemView.findViewById<ConstraintLayout>(R.id.memoItem)
            val toggleButton = viewHolder.itemView.findViewById<RadioButton>(R.id.toggleButton)

            if (mode == 0) {
                memoItem.setOnClickListener {
                    toggleButton.isChecked = !adapter.itemList[i].sel
                    if (!adapter.itemList[i].sel) {
                        adapter.selectedList.add(adapter.itemList[i])
                        if (adapter.selectedList.size == adapter.itemList.size) {
                            adapter.selectAll = true
                        }
                    } else {
                        adapter.selectedList.remove(adapter.itemList[i])
                        if (adapter.selectedList.size != adapter.itemList.size) {
                            adapter.selectAll = false
                        }
                    }
                    adapter.itemList[i].sel = !adapter.itemList[i].sel
                }
            } else {
                adapter.itemList[i].sel = false
                toggleButton.isChecked = false

                memoItem.setOnClickListener {
                    val intent = Intent(this, EditActivity::class.java)
                    intent.putExtra("memoIdx", "${adapter.itemList[i].idx}")
                    startActivity(intent)
                }
            }
        }
    }
}