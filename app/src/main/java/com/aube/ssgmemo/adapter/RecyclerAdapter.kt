package com.aube.ssgmemo.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Html
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.aube.ssgmemo.*
import com.aube.ssgmemo.callback.CallbackListener
import com.aube.ssgmemo.common.EditActivity
import com.aube.ssgmemo.common.ViewMemoActivity
import com.aube.ssgmemo.databinding.ItemMemoCompleteBinding
import com.aube.ssgmemo.databinding.RecyclerViewCtgrBinding
import com.aube.ssgmemo.databinding.RecyclerSearchMemoBinding
import com.aube.ssgmemo.databinding.RecyclerClassifyCtgrBinding
import com.aube.ssgmemo.etc.BackPressEditText
import com.aube.ssgmemo.etc.MyApplication
import io.github.muddz.styleabletoast.StyleableToast
import java.text.SimpleDateFormat
import java.util.*


class RecyclerAdapter(val context: Context): RecyclerView.Adapter<RecyclerAdapter.Holder>() {
	var listData = mutableListOf<Any>()
	var helper: SqliteHelper? = null
	var parentName : String? = null
	private var vibration =  MyApplication.prefs.getString("vibration", "")
	private var darkmode = MyApplication.prefs.getString("darkmode", "0")
	var vibrator: Vibrator? = null
	var isExistMemoList : Boolean = false
	private var selectedItemPosition = -1
	private var selectedlayout: ConstraintLayout? = null
	private var selected1: BackPressEditText? = null
	private var selected2: TextView? = null
	private var selected3: ImageButton? = null

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
		parentName = parent.resources.getResourceEntryName(parent.id).toString()
		var binding: ViewBinding? =null
		if (parentName.equals("recyclerClassifyCtgr")){
			binding =
				RecyclerClassifyCtgrBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		} else if (parentName.equals("recyclerViewCtgr")){
			binding =
				RecyclerViewCtgrBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		} else if(parentName.equals("recyclerSearch")){
			binding =
				RecyclerSearchMemoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		}else if(parentName.equals("recyclerComplete")){
			binding =
				ItemMemoCompleteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
		}
		return Holder(binding)
	}

	override fun onBindViewHolder(holder: Holder, position: Int) {
		holder.setIsRecyclable(false)
		if(parentName.equals("recyclerClassifyCtgr")){
			val ctgr: Ctgr = listData[position] as Ctgr
			holder.setCtgr(ctgr)
		} else if (parentName.equals("recyclerViewCtgr")){
			val layout = holder.itemView.findViewById<ConstraintLayout>(R.id.ctgr_item)
			val delete = holder.itemView.findViewById<ImageButton>(R.id.delete)
			val txtCtgr2 = holder.itemView.findViewById<BackPressEditText>(R.id.txtCtgr2)
			val txtCtgr3 = holder.itemView.findViewById<TextView>(R.id.txtCtgr3)

			if (position == selectedItemPosition) {
				delete.visibility = View.VISIBLE
				txtCtgr2.visibility = View.VISIBLE
				txtCtgr3.visibility = View.INVISIBLE
			} else {
				delete.visibility = View.INVISIBLE
				txtCtgr2.visibility = View.INVISIBLE
				txtCtgr3.visibility = View.VISIBLE
			}

			layout.setOnLongClickListener {
				val currentPosition = holder.adapterPosition
				if (selectedItemPosition == currentPosition) {
					selectedItemPosition = -1
					selected1?.visibility = View.INVISIBLE
					selected2?.visibility = View.INVISIBLE
					selected3?.visibility = View.VISIBLE
					selectedlayout = null
				}else {
					// Item New Selected
					if (selectedItemPosition >= 0 || selectedlayout != null) {
						selected1?.visibility = View.INVISIBLE
						selected2?.visibility = View.VISIBLE
						selected3?.visibility = View.INVISIBLE
					}

					selectedItemPosition = currentPosition
					selectedlayout = layout
					selected1 = txtCtgr2
					selected2 = txtCtgr3
					selected3 = delete
				}
				if(vibration.equals("ON")) {
					vibrator?.vibrate(VibrationEffect.createOneShot(200, 50))
				}
				delete.visibility = View.VISIBLE
				txtCtgr2.visibility = View.VISIBLE
				txtCtgr3.visibility = View.INVISIBLE
				txtCtgr2.isEnabled = true
				txtCtgr2.requestFocus()
				txtCtgr2.setSelection(txtCtgr2.length())
				(context as CallbackListener).openKeyBoard(txtCtgr2)
				return@setOnLongClickListener true
			}

			val ctgr: Ctgr = listData[position] as Ctgr
			holder.setCtgr(ctgr)
		} else if (parentName.equals("recyclerComplete")){
			holder.setComplete(listData[position])
		}else {
			val memo: Memo = listData[position] as Memo
			holder.getMemo(memo)
		}
	}

	@SuppressLint("ResourceType")
	override fun getItemCount(): Int {
		return listData.size
	}

	inner class Holder(val binding: ViewBinding?): RecyclerView.ViewHolder(binding?.root!!) {
		@SuppressLint("NotifyDataSetChanged")
		fun setCtgr(ctgr: Ctgr) {
			if (parentName.equals("recyclerClassifyCtgr")) { // <분류>
				(binding as RecyclerClassifyCtgrBinding).txtCtgr.text = ctgr.name
				if (darkmode.equals("32")) {
					binding.txtCtgr.setTextColor(Color.GRAY)
				}
				binding.box.setImageResource(R.drawable.closed_box)
				binding.cidx.text = ctgr.idx.toString()
				val layoutParams = binding.box.layoutParams
				layoutParams.height = (binding.root.layoutParams.height * 0.5).toInt()
				binding.box.layoutParams = layoutParams
				if(isExistMemoList) {
					itemView.setOnClickListener {
						binding.box.setImageResource(R.drawable.opened_box) // 닫힌 상자를 열어주고
						val handler = android.os.Handler()
						handler.postDelayed(
							Runnable { binding.box.setImageResource(R.drawable.closed_box) },
							500
						) // 0.5초 후에 다시 닫아주기

						(context as CallbackListener).moveCtgr(
							binding.cidx.text.toString().toInt()
						) // cidx값을 액티비티로 전송
					}
				}

				if (ctgr.name == "+"){
					binding.txtCtgr.visibility = View.INVISIBLE
					binding.box.setImageResource(R.drawable.add_ctgr)
					itemView.setOnClickListener {
						(context as CallbackListener).fragmentOpen(ctgr.name, null)
					}
				} else {
					binding.txtCtgr.visibility = View.VISIBLE
				}

			} else if (parentName.equals("recyclerViewCtgr")) { // <보기>
				(binding as RecyclerViewCtgrBinding).txtCtgr2.setText(ctgr.name)
				// 이미지, 크기

				val layoutParams = binding.ctgrBtn.layoutParams
				layoutParams.width = (binding.ctgrItem.layoutParams.width * 0.9).toInt()
				binding.ctgrBtn.layoutParams = layoutParams

				val layoutParams2 = binding.txtCtgr3.layoutParams
				layoutParams2.width = layoutParams.width
				binding.txtCtgr3.layoutParams = layoutParams2

				if (ctgr.name == "+"){
					binding.txtCtgr3.setTextColor(Color.parseColor("#DAD6D0"))
					binding.memoCount.visibility = View.INVISIBLE
					if (darkmode.equals("32"))
						binding.ctgrBtn.setBackgroundResource(R.drawable.ctgrback7)
					else
						binding.ctgrBtn.setBackgroundResource(R.drawable.ctgrback1)
				} else if (ctgr.name == "미분류") {
					if (darkmode.equals("32"))
						binding.ctgrBtn.setBackgroundResource(R.drawable.ctgrback5)
					else
						binding.ctgrBtn.setBackgroundResource(R.drawable.ctgrback3)
				} else if (ctgr.name == "휴지통") {
					if (darkmode.equals("32"))
						binding.ctgrBtn.setBackgroundResource(R.drawable.ctgrback6)
					else
						binding.ctgrBtn.setBackgroundResource(R.drawable.ctgrback4)
				} else {
					if (darkmode.equals("32"))
						binding.ctgrBtn.setBackgroundResource(R.drawable.ctgrback2)
					else
						binding.ctgrBtn.setBackgroundResource(R.drawable.ctgrback1)
				}

				// 값 초기화
				binding.txtCtgr3.text = ctgr.name
				binding.delete.visibility = View.INVISIBLE
				binding.txtCtgr2.visibility = View.INVISIBLE
				binding.txtCtgr3.visibility = View.VISIBLE
				binding.memoCount.text = helper!!.checkMemoListSize(ctgr.idx)

				// 클릭 리스너
				if (ctgr.name == "미분류" || ctgr.name == "+" || ctgr.name == "휴지통") {
					itemView.setOnLongClickListener {return@setOnLongClickListener false}
				}

				binding.delete.setOnClickListener {
					// 해당 ctgr에 메모가 존재하는지 판단
					if(helper?.isMemoExist(ctgr.idx.toString()) == true){
						(context as CallbackListener).fragmentOpen("delete@#",ctgr.idx.toString())

					} else { // 아니면 바로 삭제
						val unclassifyCtgr = Ctgr(0, "미분류")
						val ctgrAddBtn = Ctgr(null,"+")
						val deleteBtn = Ctgr(-1,"휴지통")
						helper?.deleteCtgr(ctgr.idx.toString())
						listData = helper?.selectCtgrList() as MutableList<Any>
						if (helper?.isMemoExist("0")!!){
							listData.add(0,unclassifyCtgr)
						}
						listData.add(ctgrAddBtn)
						listData.add(deleteBtn)
						notifyDataSetChanged()
					}
					binding.delete.visibility = View.INVISIBLE
					binding.txtCtgr2.visibility = View.INVISIBLE
					binding.txtCtgr3.visibility = View.VISIBLE
				}

				// 수정 중 뒤로가기 클릭시
				binding.txtCtgr2.setOnBackPressListener(object : BackPressEditText.OnBackPressListener{
					override fun onBackPress() {
						modifyCtgrName(binding.txtCtgr3, binding.txtCtgr2, ctgr)
					}
				})

				// 수정 완료 후 엔터 클릭 시
				binding.txtCtgr2.setOnKeyListener { _, i, keyEvent ->
					if (i == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_UP) {
						modifyCtgrName(binding.txtCtgr3, binding.txtCtgr2, ctgr)
						(context as CallbackListener).closeKeyBoard()
						return@setOnKeyListener true
					}
					return@setOnKeyListener false
				}

				if (ctgr.name == "+") {
					itemView.setOnClickListener {
						(context as CallbackListener).fragmentOpen(
							ctgr.name,null
						)
						notifyDataSetChanged()
					}
				} else {
					itemView.setOnClickListener {
						val intent = Intent(context, ViewMemoActivity::class.java)
						intent.putExtra("idx", "${ctgr.idx}")
						intent.putExtra("ctgrname", "${ctgr.name}")
						context.startActivity(intent)
					}
				}
			}
		}

		fun modifyCtgrName(textView: TextView, editText: BackPressEditText, ctgr: Ctgr) {
			val ctgrName = editText.text.toString().trim()
			if (ctgrName.isNotEmpty() && ctgrName != "미분류" && ctgrName != "delete@#" && ctgrName != "+" && ctgrName != "휴지통") {
				if (!helper!!.checkDuplicationCtgr(ctgrName)) {
					// 이름 업데이트
					helper?.updateCtgrName(
						ctgr.idx.toString(),
						editText.text.toString()
					)
					// 플레인 텍스트 값 변경
					textView.text = editText.text
					ctgr.name = editText.text.toString()
					// 데이터 변경 알림
					listData[adapterPosition] = ctgr
					this@RecyclerAdapter.notifyDataSetChanged()
				} else if (ctgrName == textView.text) {
					this@RecyclerAdapter.notifyDataSetChanged()
				} else {
					val text = "이미 사용중 입니다"
					val toast = StyleableToast.makeText(context, text, R.style.toast)
					toast.show()
				}
			} else {
				val text = "사용할 수 없는 이름입니다"
				val toast = StyleableToast.makeText(context, text, R.style.toast)
				toast.show()
			}
		}

		fun getMemo(memo: Memo) {
			binding as RecyclerSearchMemoBinding
			showMemo(memo, binding.searchTitle, binding.searchContent, binding.searchDate)
		}

		fun setComplete(item: Any) {
			binding as ItemMemoCompleteBinding
			if(item is Memo){
				showMemo(item, binding.completeTitle, binding.completeContent, binding.completeDate)

				binding.completeTitle.visibility = View.VISIBLE
				binding.completeContent.visibility = View.VISIBLE
				binding.completeDate.visibility = View.VISIBLE
				binding.lineImage2.visibility = View.VISIBLE

			}else{
				binding.completeLayout.setBackgroundResource(R.color.darkgray)
				binding.completeDay.text = item as String
				binding.completeDay.visibility = View.VISIBLE
			}
		}

		fun showMemo(memo: Memo, titleTxt: TextView, contentTxt: TextView, dateTxt: TextView) {
			val dateFormat1 = SimpleDateFormat("M월 d일", Locale("ko", "KR"))
			val dateFormat2 = SimpleDateFormat("aa\nhh:mm:ss", Locale("ko", "KR"))
			val dateFormat3 = SimpleDateFormat("yyyy년 M월 d일", Locale("ko", "KR"))
			var date = dateFormat1.format(Date(memo.datetime))
			var year = dateFormat3.format(Date(memo.datetime)).substring(0,5)
			val currentDate = dateFormat1.format(System.currentTimeMillis())
			val currentYear = dateFormat3.format(System.currentTimeMillis()).substring(0,5)
			if (year.equals(currentYear)){
				if (date.equals(currentDate)) {
					date = dateFormat2.format(Date(memo.datetime))
				}
			} else {
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
}
