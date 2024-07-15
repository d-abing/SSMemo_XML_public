package com.aube.ssgmemo.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aube.ssgmemo.Ctgr
import com.aube.ssgmemo.R
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.databinding.RecyclerClassifyCtgrBinding
import com.aube.ssgmemo.etc.MyApplication

class ClassifyCtgrAdapter(private val context: Context) :
    ListAdapter<Ctgr, ClassifyCtgrAdapter.ViewHolder>(diffUtil) {

    private var darkmode = MyApplication.prefs.getInt("darkmode", 0)
    var isExistMemoList: Boolean = false

    inner class ViewHolder(private val binding: RecyclerClassifyCtgrBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(ctgr: Ctgr) {
            binding.ctgrName.text = ctgr.name
            if (darkmode == 32) binding.ctgrName.setTextColor(Color.GRAY)
            binding.box.setImageResource(R.drawable.closed_box)
            binding.box.layoutParams.height = (binding.root.layoutParams.height * 0.5).toInt()

            if (ctgr.name == "+") {
                binding.ctgrName.visibility = View.INVISIBLE
                binding.box.setImageResource(R.drawable.add_ctgr)
                binding.root.setOnClickListener {
                    (context as CallbackListener).fragmentOpen(ctgr.name, null)
                }
            } else {
                binding.ctgrName.visibility = View.VISIBLE

                if (isExistMemoList) {
                    binding.box.setOnClickListener {
                        binding.box.setImageResource(R.drawable.opened_box)
                        binding.root.postDelayed(
                            { binding.box.setImageResource(R.drawable.closed_box) },
                            500
                        )
                        (context as CallbackListener).moveCtgr(ctgr.idx!!)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            RecyclerClassifyCtgrBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<Ctgr>() {
            override fun areItemsTheSame(oldItem: Ctgr, newItem: Ctgr): Boolean {
                return oldItem.idx == newItem.idx
            }

            override fun areContentsTheSame(oldItem: Ctgr, newItem: Ctgr): Boolean {
                return oldItem == newItem
            }
        }
    }
}
