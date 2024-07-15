package com.aube.ssgmemo.adapter

import android.content.Context
import android.content.Intent
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aube.ssgmemo.Memo
import com.aube.ssgmemo.common.EditActivity
import com.aube.ssgmemo.databinding.RecyclerSearchMemoBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SearchMemoAdapter(private val context: Context) :
    RecyclerView.Adapter<SearchMemoAdapter.ViewHolder>() {

    var listData = mutableListOf<Memo>()

    inner class ViewHolder(private val binding: RecyclerSearchMemoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(memo: Memo) {
            showMemo(memo, binding.searchTitle, binding.searchContent, binding.searchDate)
        }

        private fun showMemo(
            memo: Memo,
            titleTxt: TextView,
            contentTxt: TextView,
            dateTxt: TextView
        ) {
            val dateFormat1 = SimpleDateFormat("M월 d일", Locale("ko", "KR"))
            val dateFormat2 = SimpleDateFormat("HH:mm", Locale("ko", "KR"))
            val dateFormat3 = SimpleDateFormat("yyyy년\nM월 d일", Locale("ko", "KR"))
            var date = dateFormat1.format(Date(memo.datetime))
            val year = dateFormat3.format(Date(memo.datetime)).substring(0, 5)
            val currentDate = dateFormat1.format(System.currentTimeMillis())
            val currentYear = dateFormat3.format(System.currentTimeMillis()).substring(0, 5)

            if (year == currentYear && date == currentDate) {
                date = dateFormat2.format(Date(memo.datetime))
            } else if (year != currentYear) {
                date = dateFormat3.format(Date(memo.datetime))
            }

            dateTxt.text = date
            titleTxt.text = memo.title
            contentTxt.text = Html.fromHtml(memo.content).toString()

            itemView.setOnClickListener {
                val intent = Intent(context, EditActivity::class.java)
                intent.putExtra("memoIdx", memo.idx.toString())
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            RecyclerSearchMemoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listData[position])
    }

    override fun getItemCount(): Int = listData.size
}
