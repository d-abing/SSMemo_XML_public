package com.aube.ssgmemo.adapter

import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aube.ssgmemo.Memo
import com.aube.ssgmemo.R
import com.aube.ssgmemo.SqliteHelper
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.callback.ItemTouchHelperListener
import com.aube.ssgmemo.common.EditActivity
import com.aube.ssgmemo.databinding.RecyclerViewMemoBinding
import com.aube.ssgmemo.etc.ModeStatus
import com.aube.ssgmemo.etc.MyApplication
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecyclerSwipeAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerSwipeAdapter.ViewHolder>(),
    ItemTouchHelperListener {

    lateinit var helper: SqliteHelper
    lateinit var callbackListener: CallbackListener
    lateinit var binding: RecyclerViewMemoBinding
    lateinit var itemList: MutableList<Memo>
    private val vibration = MyApplication.prefs.getString("vibration", "OFF")
    private val darkmode = MyApplication.prefs.getInt("darkmode", 16)
    var vibrator: Vibrator? = null
    var mode = 0
    var selectAll = false
    var selectedList: MutableList<Memo> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        binding =
            RecyclerViewMemoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        holder.bind(itemList[position])
    }

    override fun getItemCount(): Int = itemList.size

    inner class ViewHolder(val binding: RecyclerViewMemoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(memo: Memo) {
            // Date formatting
            val dateFormat1 = SimpleDateFormat("M월 d일", Locale("ko", "KR"))
            val dateFormat2 = SimpleDateFormat("HH:mm", Locale("ko", "KR"))
            val dateFormat3 = SimpleDateFormat("yyyy년 M월 d일", Locale("ko", "KR"))
            val currentDate = dateFormat1.format(System.currentTimeMillis())
            val currentYear = dateFormat3.format(System.currentTimeMillis()).substring(0, 5)

            var date = dateFormat1.format(Date(memo.datetime))
            val year = dateFormat3.format(Date(memo.datetime)).substring(0, 5)

            if (year == currentYear && date == currentDate) {
                date = dateFormat2.format(Date(memo.datetime))
            } else if (year != currentYear) {
                date = dateFormat3.format(Date(memo.datetime))
            }

            // Initialize item views
            binding.memoTitle.text = memo.title
            binding.memoContent.text = Html.fromHtml(memo.content).toString()
            binding.memoDate.text = date
            binding.btnComplete.visibility = View.VISIBLE
            binding.toggleButton.visibility = View.GONE

            // Set dark mode background if applicable
            if (darkmode == 32) {
                binding.memoItem.setBackgroundResource(R.drawable.memoback2)
            }

            // Restore toggle button state
            binding.toggleButton.isChecked = memo.isSelected

            // Toggle button click listener
            binding.toggleButton.setOnClickListener {
                memo.isSelected = !memo.isSelected
                binding.toggleButton.isChecked = memo.isSelected
                if (memo.isSelected) {
                    selectedList.add(memo)
                } else {
                    selectedList.remove(memo)
                }
                selectAll = selectedList.size == itemList.size
            }

            // Handle item click and mode change
            binding.memoItem.setOnClickListener {
                if (mode == ModeStatus.SELECT.code) {
                    memo.isSelected = !memo.isSelected
                    binding.toggleButton.isChecked = memo.isSelected
                    if (memo.isSelected) {
                        selectedList.add(memo)
                    } else {
                        selectedList.remove(memo)
                    }
                    selectAll = selectedList.size == itemList.size
                } else if (mode == ModeStatus.COMPLETE.code) {
                    val intent = Intent(context, EditActivity::class.java)
                    intent.putExtra("memoIdx", memo.idx.toString())
                    intent.putExtra("ctgrname", helper.selectCtgrName(memo.ctgr.toString()))
                    context.startActivity(intent)
                }
            }

            // Complete button click listener
            binding.btnComplete.setOnClickListener {
                if (vibration == "ON") {
                    vibrator?.vibrate(VibrationEffect.createOneShot(200, 50))
                }
                callbackListener.completeFragmentOpen(memo.idx!!)
            }

            // Adjust visibility based on mode
            if (mode == ModeStatus.SELECT.code) {
                binding.memoItem.translationX = 150f
                binding.btnComplete.visibility = View.GONE
                binding.toggleButton.visibility = View.VISIBLE
            } else {
                binding.btnComplete.visibility = View.VISIBLE
                binding.toggleButton.visibility = View.GONE
            }
        }

        fun onDragStarted() {
            if (darkmode == 32) {
                binding.memoItem.setBackgroundResource(R.drawable.memoback4)
            } else {
                binding.memoItem.setBackgroundResource(R.drawable.memoback3)
            }
        }

        fun onDragEnded() {
            if (darkmode == 32) {
                binding.memoItem.setBackgroundResource(R.drawable.memoback2)
            } else {
                binding.memoItem.setBackgroundResource(R.drawable.memoback1)
            }
        }
    }

    override fun onItemMove(from: Int, to: Int): Boolean {
        if (vibration == "ON") {
            vibrator?.vibrate(VibrationEffect.createOneShot(200, 50))
        }

        // Update item priorities
        val priorityGap = itemList[from].priority - itemList[to].priority
        when (priorityGap) {
            1 -> {
                itemList[to].priority += 1
                itemList[from].priority -= 1
            }

            -1 -> {
                itemList[to].priority -= 1
                itemList[from].priority += 1
            }
        }

        val data = itemList[from]
        helper.movePriority(itemList[from], itemList[to])
        itemList.removeAt(from)
        itemList.add(to, data)

        notifyItemMoved(from, to)
        return true
    }

    override fun onItemDrag() {
        if (vibration == "ON") {
            vibrator?.vibrate(VibrationEffect.createOneShot(200, 50))
        }
    }
}
