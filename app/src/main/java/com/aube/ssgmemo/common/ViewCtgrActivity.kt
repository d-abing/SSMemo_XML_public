package com.aube.ssgmemo.common

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
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
import com.aube.ssgmemo.adapter.RecyclerAdapter
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.databinding.ActivityViewCtgrBinding
import com.aube.ssgmemo.etc.MyApplication
import com.aube.ssgmemo.fragment.CtgrAddFragment
import com.aube.ssgmemo.fragment.CtgrDeleteFragment
import io.github.muddz.styleabletoast.StyleableToast

class ViewCtgrActivity : AppCompatActivity(), CallbackListener {
    private lateinit var binding: ActivityViewCtgrBinding
    private val helper = SqliteHelper(this, "ssgMemo", 1)

    private var darkmode = MyApplication.prefs.getString("darkmode", "0")

    private lateinit var ctgrAdapter: RecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewCtgrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 다크모드 설정
        if (darkmode.equals("32")) {
            binding.viewCtgrLayout.setBackgroundColor(Color.BLACK)
            binding.adView.setBackgroundColor(Color.BLACK)
            binding.linearLayout.setBackgroundResource(R.drawable.graysearchbar2)
            binding.btnFilter.setImageResource(R.drawable.grayfilter2)
        }

        // dpi 값에 맞게 layoutParams 조절
        val dpi = resources.displayMetrics.densityDpi
        val multiplier = -0.001 * dpi + 1.12

        val rect = Rect()
        binding.viewCtgrLayout.getWindowVisibleDisplayFrame(rect)

        dpiLayoutParams(binding.linearLayout, (rect.width() * 0.9).toInt())
        dpiLayoutParams(binding.recyclerViewCtgr, (rect.width() * 0.9).toInt())
        dpiLayoutParams(binding.keyword, ((rect.width() * 0.9).toInt() * multiplier).toInt())
        dpiLayoutParams(binding.recyclerSearch, (rect.width() * 0.9).toInt())
        dpiLayoutParams(binding.condition4, (rect.width() * 0.45).toInt())
        dpiLayoutParams(binding.condition3, (rect.width() * 0.45).toInt())

        // < 검색 >
        val memoAdapter = RecyclerAdapter(this)
        var where = "제목+내용"          // sql where 조건
        var orderby = "최신순"          // sql orderby 조건
        var keyword = ""               // sql where의 keyword

        memoAdapter.helper = helper
        showDataList(memoAdapter, keyword, where, orderby)
        binding.recyclerSearch.adapter = memoAdapter

        binding.keyword.doOnTextChanged { _, _, _, _ ->
            keyword = binding.keyword.text.toString()
            memoAdapter.listData.clear()
            showDataList(memoAdapter, keyword, where, orderby)
            false
        }

        val conditionList1: MutableList<String> = arrayListOf("제목+내용", "제목", "내용")
        val conditionList2: MutableList<String> = arrayListOf("최신순", "오래된순")

        // <"제목", "내용", "제목+내용">

        binding.condition3.adapter = ArrayAdapter(this, R.layout.spinner_layout, conditionList1)
        binding.condition3.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // where 조건 바꿔서 select
                where = binding.condition3.getItemAtPosition(position).toString()
                memoAdapter.listData.clear()
                showDataList(memoAdapter, keyword, where, orderby)
            }
        }

        // <"최신순", "오래된순">
        binding.condition4.adapter = ArrayAdapter(this, R.layout.spinner_layout, conditionList2)
        binding.condition4.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // order by 조건 바꿔서 select
                orderby = binding.condition4.getItemAtPosition(position).toString()
                memoAdapter.listData.clear()
                showDataList(memoAdapter, keyword, where, orderby)

            }
        }

        var flag = false
        binding.btnFilter.setOnClickListener {
            if (flag == false) {
                binding.condition4.visibility = View.VISIBLE
                binding.condition3.visibility = View.VISIBLE
                binding.recyclerViewCtgr.margin(top = 48F)
                binding.recyclerSearch.margin(top = 60F)
                binding.emptyText4.margin(top = 60F)
                flag = true
            } else {
                binding.condition4.visibility = View.GONE
                binding.condition3.visibility = View.GONE
                binding.recyclerViewCtgr.margin(top = 20F)
                binding.recyclerSearch.margin(top = 20F)
                binding.emptyText4.margin(top = 20F)
                flag = false
            }
        }

        // < 카테고리 list >
        // ctgr뷰 어댑터 초기화
        ctgrAdapter = RecyclerAdapter(this)
        ctgrAdapter.helper = helper
        ctgrAdapter.vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        setDefaultCtgr()
        binding.recyclerViewCtgr.adapter = ctgrAdapter
        binding.recyclerViewCtgr.layoutManager = GridLayoutManager(this, 2)

        // < 검색 & 카테고리 화면 전환 >
        binding.viewCtgrLayout.viewTreeObserver.addOnGlobalLayoutListener {
            if (binding.keyword.text!!.isNotEmpty()) {
                binding.recyclerSearch.visibility = View.VISIBLE
                binding.recyclerViewCtgr.visibility = View.INVISIBLE
            } else {
                binding.recyclerViewCtgr.visibility = View.VISIBLE
                binding.recyclerSearch.visibility = View.INVISIBLE
                binding.emptyText4.visibility = View.INVISIBLE
            }
        }
    }

    fun dpiLayoutParams(view: View, width: Int) {
        val tmp_layoutParams = view.layoutParams
        tmp_layoutParams.width = width
        view.layoutParams = tmp_layoutParams
    }

    fun setDefaultCtgr() {
        val btnCtgrAdd = Ctgr(null, "+")
        val btnUnclassified = Ctgr(0, "미분류")
        val btnDeleted = Ctgr(-1, "휴지통")

        val ctgrList = helper.selectCtgrList() as MutableList<Any>
        if (helper.isMemoExist("0")) {
            ctgrList.add(0, btnUnclassified)
        }
        ctgrList.add(btnCtgrAdd)
        ctgrList.add(btnDeleted)
        ctgrAdapter.listData = ctgrList
        ctgrAdapter.notifyDataSetChanged()
    }

    override fun onRestart() {
        super.onRestart()
        setDefaultCtgr()
    }

    override fun fragmentOpen(item: String, ctgrIdx: String?) {
        if (item == "+") {
            val ctgrAddFragment = CtgrAddFragment(this)
            ctgrAddFragment.show(supportFragmentManager, "CtgrAdd")
        } else if (item == "delete@#") {
            val ctgrDeleteFragment = CtgrDeleteFragment(this)
            val bundle = Bundle()
            bundle.putString("ctgrIdx", ctgrIdx)
            ctgrDeleteFragment.arguments = bundle
            ctgrDeleteFragment.show(supportFragmentManager, "DeleteFragment")
        }
    }

    // ctgr 추가
    override fun addCtgr(ctgrName: String) {
        val ctgr = Ctgr(null, ctgrName)
        if (ctgrName != "미분류" && ctgrName != "delete@#" && ctgrName != "+" && ctgrName != "휴지통") {
            if (!helper.checkDuplicationCtgr(ctgrName)) {
                helper.insertCtgr(ctgr)
                setDefaultCtgr()
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

    // ctgr 삭제
    override fun deleteCtgr(ctgrIdx: String) {
        helper.deleteCtgr(ctgrIdx)
        setDefaultCtgr()
    }

    // ctgr 이동
    override fun moveCtgrList(oldCtgrIdx: Int, newCtgrIdx: Int) {
        val sortedList = helper.selectMemoList(oldCtgrIdx.toString()).sortedBy { it.priority }
        for (memo in sortedList) {
            helper.updateMemoCtgr(memo.idx, oldCtgrIdx, newCtgrIdx)
        }
        deleteCtgr(oldCtgrIdx.toString())
        ctgrAdapter.notifyDataSetChanged()
    }

    // 키보드 관련
    override fun openKeyBoard(view: View) {
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, 0)
    }

    override fun closeKeyBoard() {
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    // 검색 결과
    fun showDataList(
        recyclerAdapter: RecyclerAdapter,
        keyword: String,
        where: String,
        orderby: String
    ) {
        val data = helper.selectSearchList(keyword, where, orderby)
        recyclerAdapter.listData.addAll(helper.selectSearchList(keyword, where, orderby))
        if (data.isEmpty()) {
            binding.recyclerSearch.visibility = View.INVISIBLE
            binding.emptyText4.visibility = View.VISIBLE
        } else {
            binding.recyclerSearch.visibility = View.VISIBLE
            binding.emptyText4.visibility = View.INVISIBLE
        }
        recyclerAdapter.notifyDataSetChanged()
    }

    fun View.margin(
        left: Float? = null,
        top: Float? = null,
        right: Float? = null,
        bottom: Float? = null
    ) {
        layoutParams<ViewGroup.MarginLayoutParams> {
            left?.run { leftMargin = dpToPx(this) }
            top?.run { topMargin = dpToPx(this) }
            right?.run { rightMargin = dpToPx(this) }
            bottom?.run { bottomMargin = dpToPx(this) }
        }
    }

    inline fun <reified T : ViewGroup.LayoutParams> View.layoutParams(block: T.() -> Unit) {
        if (layoutParams is T) block(layoutParams as T)
    }

    fun View.dpToPx(dp: Float): Int = context.dpToPx(dp)
    fun Context.dpToPx(dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

}

