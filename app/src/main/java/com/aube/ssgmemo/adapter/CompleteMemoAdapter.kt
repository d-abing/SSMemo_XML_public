package com.aube.ssgmemo.adapter

import android.content.Context
import android.content.Intent
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aube.ssgmemo.Memo
import com.aube.ssgmemo.R
import com.aube.ssgmemo.common.EditActivity
import com.aube.ssgmemo.databinding.RecyclerCompleteMemoBinding
import com.aube.ssgmemo.etc.MyApplication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CompleteMemoAdapter(private val context: Context) :
    RecyclerView.Adapter<CompleteMemoAdapter.ViewHolder>() {

    var listData = mutableListOf<Any>()
    private val darkmode = MyApplication.prefs.getInt("darkmode", 16)

    inner class ViewHolder(private val binding: RecyclerCompleteMemoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Any) {
            if (item is Memo) {
                if (darkmode == 32) {
                    binding.completeLayout.setBackgroundResource(R.drawable.memoback2)
                } else {
                    binding.completeLayout.setBackgroundResource(R.drawable.memoback1)
                }
                showMemo(item, binding.completeTitle, binding.completeContent, binding.completeDate)
                binding.completeTitle.visibility = View.VISIBLE
                binding.completeContent.visibility = View.VISIBLE
                binding.completeDate.visibility = View.VISIBLE
                binding.completeDay.visibility = View.GONE
            } else {
                binding.completeLayout.setBackgroundResource(R.color.lightgray)
                binding.completeDay.text = item as String
                binding.completeDay.visibility = View.VISIBLE
                binding.completeTitle.visibility = View.GONE
                binding.completeContent.visibility = View.GONE
                binding.completeDate.visibility = View.GONE
                itemView.setOnClickListener { }
            }
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
                intent.putExtra("memoIdx", "${memo.idx}")
                context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            RecyclerCompleteMemoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(listData[position])
    }

    override fun getItemCount(): Int = listData.size
}
