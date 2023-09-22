package com.aube.ssgmemo.common

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
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
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.adapter.RecyclerAdapter
import com.aube.ssgmemo.databinding.ActivityCompleteBinding
import com.aube.ssgmemo.etc.MyApplication

class CompleteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCompleteBinding
    private val helper = SqliteHelper(this, "ssgMemo", 1)

    private var darkmode = MyApplication.prefs.getString("darkmode", "0")

    private val recyclerAdapter = RecyclerAdapter(this)
    private var where = "제목+내용"          // sql where 조건
    private var orderby = "최신순"          // sql orderby 조건
    private var keyword = ""               // sql where의 keyword

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompleteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 다크모드 설정
        if (darkmode.equals("32")) {
            binding.parentLayout.setBackgroundColor(Color.BLACK)
            binding.adView.setBackgroundColor(Color.BLACK)
            binding.linearLayout.setBackgroundResource(R.drawable.graysearchbar2)
            binding.btnFilter.setImageResource(R.drawable.grayfilter2)
        }

        // dpi 값에 맞게 layoutParams 조절
        val dpi = resources.displayMetrics.densityDpi
        val multiplier = -0.001 * dpi + 1.12

        val rect = Rect()
        binding.parentLayout.getWindowVisibleDisplayFrame(rect)

        dpiLayoutParams(binding.linearLayout, (rect.width() * 0.9).toInt())
        dpiLayoutParams(binding.recyclerComplete, (rect.width() * 0.9).toInt())
        dpiLayoutParams(binding.keyword, ((rect.width() * 0.9).toInt() * multiplier).toInt())
        dpiLayoutParams(binding.condition1, (rect.width() * 0.45).toInt())
        dpiLayoutParams(binding.condition2, (rect.width() * 0.45).toInt())
        dpiLayoutParams(binding.emptyText2, (rect.width() * 0.9).toInt())

        recyclerAdapter.helper = helper
        showDataList(recyclerAdapter, keyword, where, orderby)
        binding.recyclerComplete.adapter = recyclerAdapter

        binding.keyword.doOnTextChanged { _, _, _, _ ->
            keyword = binding.keyword.text.toString()
            recyclerAdapter.listData.clear()
            showDataList(recyclerAdapter, keyword, where, orderby)
        }

        val conditionList1: MutableList<String> = arrayListOf("제목+내용", "제목", "내용")
        val conditionList2: MutableList<String> = arrayListOf("최신순", "오래된순")

        // <"제목", "내용", "제목+내용">
        binding.condition1.adapter = ArrayAdapter(this, R.layout.spinner_layout, conditionList1)
        binding.condition1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // where 조건 바꿔서 select
                where = binding.condition1.getItemAtPosition(position).toString()
                recyclerAdapter.listData.clear()
                showDataList(recyclerAdapter, keyword, where, orderby)
            }
        }

        // <"최신순", "오래된순">
        binding.condition2.adapter = ArrayAdapter(this, R.layout.spinner_layout, conditionList2)
        binding.condition2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                // order by 조건 바꿔서 select
                orderby = binding.condition2.getItemAtPosition(position).toString()
                recyclerAdapter.listData.clear()
                showDataList(recyclerAdapter, keyword, where, orderby)

            }
        }

        var flag = false
        binding.btnFilter.setOnClickListener {
            if (flag == false) {
                binding.condition1.visibility = View.VISIBLE
                binding.condition2.visibility = View.VISIBLE
                binding.recyclerComplete.margin(top = 60F)
                binding.emptyText2.margin(top = 60F)
                flag = true
            } else {
                binding.condition1.visibility = View.GONE
                binding.condition2.visibility = View.GONE
                binding.recyclerComplete.margin(top = 20F)
                binding.emptyText2.margin(top = 20F)
                flag = false
            }
        }
    }

    fun dpiLayoutParams(view: View, width: Int) {
        val tmp_layoutParams = view.layoutParams
        tmp_layoutParams.width = width
        view.layoutParams = tmp_layoutParams
    }

    override fun onRestart() {
        super.onRestart()
        recyclerAdapter.listData.clear()
        showDataList(recyclerAdapter, keyword, where, orderby)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val imm: InputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        return true
    }

    fun showDataList(
        recyclerAdapter: RecyclerAdapter,
        keyword: String,
        where: String,
        orderby: String
    ) {
        val data = helper.selectCompleteList(keyword, where, orderby)
        recyclerAdapter.listData.addAll(helper.selectCompleteList(keyword, where, orderby))
        if (data.isEmpty()) {
            binding.recyclerComplete.visibility = View.INVISIBLE
            binding.emptyText2.visibility = View.VISIBLE
        } else {
            binding.recyclerComplete.visibility = View.VISIBLE
            binding.emptyText2.visibility = View.INVISIBLE
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