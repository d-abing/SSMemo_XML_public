package com.aube.ssgmemo.common

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.aube.ssgmemo.R
import com.aube.ssgmemo.etc.MyApplication
import io.github.muddz.styleabletoast.StyleableToast

abstract class BaseSearchActivity : AppCompatActivity() {
    protected var darkmode = MyApplication.prefs.getInt("darkmode", 16)

    protected var keyword = ""
    protected var where = "제목+내용"
    protected var orderby = "최신순"


    protected open fun setupDarkMode(view: View) {
        if (darkmode == 32) {
            view.setBackgroundColor(Color.BLACK)
        }
    }

    /*protected fun setupAds(adView: AdView) {
        MobileAds.initialize(this)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }*/

    protected fun setupSearchBar(searchBar: EditText, reloadData: () -> Unit) {
        searchBar.doOnTextChanged { _, _, _, _ ->
            keyword = searchBar.text.toString()
            reloadData()
        }
    }

    protected fun setupSpinners(
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

    fun setupFilterButton(
        filterButton: View,
        conditionContain: Spinner,
        conditionLatest: Spinner,
        recyclerViewCtgr: androidx.recyclerview.widget.RecyclerView? = null,
        recyclerSearch: androidx.recyclerview.widget.RecyclerView,
        emptyText: TextView,

        ) {
        var isFilterVisible = false
        filterButton.setOnClickListener {
            isFilterVisible = !isFilterVisible
            val visibility = if (isFilterVisible) View.VISIBLE else View.GONE
            conditionContain.visibility = visibility
            conditionLatest.visibility = visibility
            val topMargin = if (isFilterVisible) 60F else 15F
            recyclerSearch.updateMargin(top = topMargin)
            emptyText.updateMargin(top = topMargin)
            recyclerViewCtgr?.updateMargin(top = topMargin)
        }
    }

    protected fun View.updateMargin(
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        return true
    }

    protected fun showToast(message: String) {
        StyleableToast.makeText(applicationContext, message, R.style.toast).show()
    }
}
