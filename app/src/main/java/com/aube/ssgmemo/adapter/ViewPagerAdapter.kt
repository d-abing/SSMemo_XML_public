package com.aube.ssgmemo.adapter

import android.content.Context
import android.graphics.Typeface
import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.aube.ssgmemo.Memo
import com.aube.ssgmemo.databinding.RecyclerClassifyMemoBinding
import com.aube.ssgmemo.etc.MyApplication

class ViewPagerAdapter(private val context: Context) : RecyclerView.Adapter<ViewPagerAdapter.Holder>() {
    var listData = mutableListOf<Memo>()
    private var memofont = MyApplication.prefs.getString("memofont", "")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : Holder {
        val binding: ViewBinding = RecyclerClassifyMemoBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return Holder(binding)
    }
    override fun getItemCount(): Int = listData.size
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val memo: Memo = listData.get(position)
        holder.setMemo(memo)
    }
    inner class Holder(val binding: ViewBinding): RecyclerView.ViewHolder(binding.root) {
        fun setMemo(memo: Memo) {
            (binding as RecyclerClassifyMemoBinding).memoTitle.text = memo.title

            // 폰트 설정
            if (!memofont.equals("")) {
                val typeface = Typeface.createFromAsset(context.assets, "font/" + memofont + ".ttf")
                binding.memoTitle.typeface = typeface
                binding.memoContent.typeface = typeface
            }

            val contentAtt = memo.contentAttribute.split(",")
            val contentGravity = contentAtt[0].toInt()
            binding.memoContent.text = Html.fromHtml(memo.content)
            binding.memoContent.movementMethod = ScrollingMovementMethod()
            binding.memoContent.gravity = contentGravity

        }
    }
}