package com.aube.ssgmemo

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.Html
import android.text.SpannableString
import java.text.SimpleDateFormat
import java.util.*

class SqliteHelper(context: Context, name: String, version: Int):
	SQLiteOpenHelper(context, name, null, version) {
	// SQLiteOpenHelper : DB를 생성하고, 코틀린으로 DB를 사용할 수 있도록 연결하는 역할
	override fun onCreate(db: SQLiteDatabase?) {
		// 앱이 설치되어 SQLiteOpenHelper 클래스가 최초로 사용되는 순간 호출됨
		// 전체 앱에서 가장 처음 한 번만 수행되며, 대부분 테이블을 생성하는 코드를 작성
		val sql = "create table ctgr (idx integer primary key, name text)"
		db?.execSQL(sql)
		val sql1 = "create table memo (idx integer primary key, title text, content text, contentString text, contentAttribute text, datetime integer, ctgr integer, priority integer, status integer, FOREIGN KEY (ctgr) references ctgr(idx))"
		db?.execSQL(sql1)
	}
	override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) { }
	// DB 버전 정보가 변경될 때 마다 반복해서 호출되며, 테이블의 스키마 부분을 변경하기 위한 용도로 사용


	// <<INSERT>>----------------------------------------------------------------------------------------------------------------------------------------
	fun insertCtgr(ctgr: Ctgr) {
		val wd = writableDatabase
		val sql = "insert into ctgr (name) values " + "('${ctgr.name}')"
		wd.execSQL(sql)
		wd.close()
	}

	fun insertMemo(memo: Memo) {
		val wd = writableDatabase
		val sql = "insert into memo (title, content, contentString, contentAttribute, datetime, ctgr, priority, status) values " +
				"('${memo.title}', '${memo.content}', '${SpannableString.valueOf(Html.fromHtml(memo.content))}', '${memo.contentAttribute}', ${memo.datetime}, ${memo.ctgr}, ${memo.priority}, ${memo.status})"
		wd.execSQL(sql)
		wd.close()
	}

	// <<UPDATE>>----------------------------------------------------------------------------------------------------------------------------------------
	// <MEMO>
	fun updateMemoCtgr(memoidx: Int?, oldCtgr: Int, newCtgr: Int) {
		val values = ContentValues()
		values.put("ctgr", newCtgr)
		values.put("priority", checkTopPriority(newCtgr) + 1)

		val wd = writableDatabase
		wd.update("memo", values, "idx = ${memoidx}", null)
		wd.close()

		sortPriority(oldCtgr)
	}

	fun updateMemoStatus(memo: Memo, status: Int) {
		val wd = writableDatabase
		val values = ContentValues()
		values.put("status", status)
		values.put("datetime", System.currentTimeMillis())

		wd.update("memo", values, "idx = ${memo.idx}", null)
	}

	fun updateMemo(memo: Memo) {
		val db = this.writableDatabase
		val title = memo.title
		val content = memo.content
		val contentString = SpannableString.valueOf(Html.fromHtml(memo.content)).toString()
		val contentAttribute = memo.contentAttribute
		val ctgr = memo.ctgr
		val datetime = System.currentTimeMillis()
		val priority = memo.priority
		val contentValues = ContentValues().apply {
			put("title", title)
			put("content", content)
			put("contentString", contentString)
			put("contentAttribute", contentAttribute)
			put("datetime", datetime)
			put("priority",priority)
			put("ctgr",ctgr)
		}
		db.update("memo", contentValues, "idx = ${memo.idx}", null)
		db.close()
	}

	fun movePriority(itemList_from: Memo, itemList_to: Memo) {
		val wd = writableDatabase
		val wd1 = writableDatabase
		val sql = "update memo set priority = '" + itemList_from.priority + "' where idx = '" + itemList_from.idx + "'"
		val sql1 = "update memo set priority = '" + itemList_to.priority + "' where idx = '" + itemList_to.idx + "'"

		wd.execSQL(sql)
		wd1.execSQL(sql1)
		wd.close()
		wd1.close()
	}

	@SuppressLint("Range")
	fun sortPriority(cidx: Int?) {
		val sql1 = "select idx from memo where ctgr = '" + cidx + "' order by priority"
		val rd = readableDatabase
		val rs = rd.rawQuery(sql1, null)
		var priority = 0
		val wd = writableDatabase
		while (rs.moveToNext()) {
			val sql2 = "update memo set priority = '" + priority + "', ctgr = " + cidx + " where idx = '" + rs.getLong(rs.getColumnIndex("idx")) + "'"
			wd.execSQL(sql2)
			priority ++
		}
		wd.close()
		rs.close()
		rd.close()
	}

	// <CTGR>
	fun updateCtgrName(idx: String, name: String) {
		val db = this.writableDatabase
		val contentValues = ContentValues().apply {
			put("name", name)
		}
		db.update("ctgr", contentValues, "idx = ${idx}", null)
		db.close()
	}

	// <<SELECT>>----------------------------------------------------------------------------------------------------------------------------------------
	// <<MEMO>
	@SuppressLint("Range")
	fun selectUnclassifiedMemoList(): MutableList<Memo> {
		// classify
		val list = mutableListOf<Memo>()
		val sql = "select * from memo where ctgr = '0' and status = 1 order by priority asc"
		val rd = readableDatabase
		val rs = rd.rawQuery(sql, null)

		while (rs.moveToNext()) {
			val idx = rs.getInt(rs.getColumnIndex("idx"))
			val title = rs.getString(rs.getColumnIndex("title"))
			val content = rs.getString(rs.getColumnIndex("content"))
			val contentAttribute = rs.getString(rs.getColumnIndex("contentAttribute"))
			val datetime = rs.getLong(rs.getColumnIndex("datetime"))
			val ctgr = rs.getInt(rs.getColumnIndex("ctgr"))
			val priority = rs.getInt(rs.getColumnIndex("priority"))
			val status = rs.getInt(rs.getColumnIndex("status"))

			list.add(Memo(idx, title, content, contentAttribute, datetime, ctgr, priority, status))
		}
		rs.close()
		rd.close()

		return list
	}

	@SuppressLint("Range")
	fun selectMemoList(ctgr:String): MutableList<Memo> {
		// viewMemo
		val list = mutableListOf<Memo>()
		val sql = "select * from memo where ctgr = '" + ctgr + "' and status = 1 order by priority desc"
		val rd = readableDatabase
		val rs = rd.rawQuery(sql, null)

		while (rs.moveToNext()) {
			val idx = rs.getInt(rs.getColumnIndex("idx"))
			val title = rs.getString(rs.getColumnIndex("title"))
			val content = rs.getString(rs.getColumnIndex("content"))
			val contentAttribute = rs.getString(rs.getColumnIndex("contentAttribute"))
			val datetime = rs.getLong(rs.getColumnIndex("datetime"))
			val ctgr = rs.getInt(rs.getColumnIndex("ctgr"))
			val priority = rs.getInt(rs.getColumnIndex("priority"))
			val status = rs.getInt(rs.getColumnIndex("status"))

			list.add(Memo(idx, title, content, contentAttribute, datetime, ctgr, priority, status))
		}
		rs.close()
		rd.close()

		return list
	}

	@SuppressLint("Range")
	fun selectMemo(idx:String): Memo {
		// edit
		lateinit var memo:Memo
		val sql = "select * from memo where idx = '" + idx + "' order by priority desc"
		val rd = readableDatabase
		val rs = rd.rawQuery(sql, null)

		while (rs.moveToNext()) {
			val idx = rs.getInt(rs.getColumnIndex("idx"))
			val title = rs.getString(rs.getColumnIndex("title"))
			val content = rs.getString(rs.getColumnIndex("content"))
			val contentAttribute = rs.getString(rs.getColumnIndex("contentAttribute"))
			val datetime = rs.getLong(rs.getColumnIndex("datetime"))
			val ctgr = rs.getInt(rs.getColumnIndex("ctgr"))
			val priority = rs.getInt(rs.getColumnIndex("priority"))
			val status = rs.getInt(rs.getColumnIndex("status"))

			memo = Memo(idx, title, content, contentAttribute, datetime, ctgr, priority, status)
		}
		rs.close()
		rd.close()

		return memo
	}

	@SuppressLint("Range")
	fun selectSearchList(keyword: String, where: String, orderby: String): MutableList<Memo> {
		// viewctgr
		val list = mutableListOf<Memo>()
		var condition1 = ""
		var condition2 = ""

		when (where) {
			"제목" -> {
				condition1 = "where title like '%$keyword%' and status = 1"
			}
			"내용" -> {
				condition1 = "where contentString like '%$keyword%' and status = 1"
			}
			"제목+내용" -> {
				condition1 = "where (title like '%$keyword%' or contentString like '%$keyword%') and status = 1"
			}
		}

		condition2 = if( orderby == "최신순") {
			"order by datetime desc"
		} else {
			"order by datetime asc"
		}

		val sql = "select * from memo $condition1 $condition2"
		val rd = readableDatabase
		val rs = rd.rawQuery(sql, null)

		while (rs.moveToNext()) {
			val idx = rs.getInt(rs.getColumnIndex("idx"))
			val title = rs.getString(rs.getColumnIndex("title"))
			val content = rs.getString(rs.getColumnIndex("content"))
			val contentAttribute = rs.getString(rs.getColumnIndex("contentAttribute"))
			val datetime = rs.getLong(rs.getColumnIndex("datetime"))
			val ctgr = rs.getInt(rs.getColumnIndex("ctgr"))
			val priority = rs.getInt(rs.getColumnIndex("priority"))
			val status = rs.getInt(rs.getColumnIndex("status"))

			list.add(Memo(idx, title, content, contentAttribute, datetime, ctgr, priority, status))
		}
		rs.close()
		rd.close()

		return list
	}

	fun selectCompleteList(keyword: String, where: String, orderby: String): MutableList<Any> {
		// complete
		val list = mutableListOf<Memo>()
		val listList = mutableListOf<Any>()
		var condition1 = ""
		var condition2 = ""

		when (where) {
			"제목" -> {
				condition1 = "where title like '%$keyword%' and status = 2"
			}
			"내용" -> {
				condition1 = "where contentString like '%$keyword%' and status = 2"
			}
			"제목+내용" -> {
				condition1 = "where (title like '%$keyword%' or contentString like '%$keyword%') and status = 2"
			}
		}

		condition2 = if( orderby == "최신순") {
			"order by datetime desc"
		} else {
			"order by datetime asc"
		}


		val sql = "select * from memo $condition1 $condition2"
		val rd = readableDatabase
		val rs = rd.rawQuery(sql, null)

		while (rs.moveToNext()) {
			val idx = rs.getInt(rs.getColumnIndex("idx"))
			val title = rs.getString(rs.getColumnIndex("title"))
			val content = rs.getString(rs.getColumnIndex("content"))
			val contentAttribute = rs.getString(rs.getColumnIndex("contentAttribute"))
			val datetime = rs.getLong(rs.getColumnIndex("datetime"))
			val ctgr = rs.getInt(rs.getColumnIndex("ctgr"))
			val priority = rs.getInt(rs.getColumnIndex("priority"))
			val status = rs.getInt(rs.getColumnIndex("status"))

			list.add(Memo(idx, title, content, contentAttribute, datetime, ctgr, priority, status))
		}
		rs.close()
		rd.close()

		val dateFormat = SimpleDateFormat("yyyy년 M월 d일", Locale("ko", "KR"))
		var num = 0
		val list2 = mutableListOf<Memo>()

		for (i in list) {
			val date1 = dateFormat.format(Date(list.get(num).datetime))
			val date2 = if (num + 1 != list.size) {dateFormat.format(Date(list.get(num+1).datetime))} else {"0"}

			if (date1.equals(date2)) {
				list2.add(i)
			} else {
				list2.add(i)
				listList.add(date1)
				for (i in list2) {
					listList.add(i)
				}
				list2.clear()
			}
			num++
		}

		return listList
	}

	// <CTGR>
	@SuppressLint("Range")
	fun selectCtgrMap(): MutableMap<Int,String> {
		// write
		val map = mutableMapOf<Int,String>()
		val sql = "select name, idx from ctgr "
		val rd = readableDatabase
		val rs = rd.rawQuery(sql, null)

		while (rs.moveToNext()) {
			val name = rs.getString(rs.getColumnIndex("name"))
			val idx = rs.getLong(rs.getColumnIndex("idx"))
			map[idx.toInt()] = name
		}
		rs.close()
		rd.close()

		return map
	}

	@SuppressLint("Range")
	fun selectCtgrList(): MutableList<Ctgr> {
		// viewctgr, classify
		val list = mutableListOf<Ctgr>()
		val sql = "select * from ctgr"
		val rd = readableDatabase
		val rs = rd.rawQuery(sql, null)

		while (rs.moveToNext()) {
			val idx = rs.getInt(rs.getColumnIndex("idx"))
			val name = rs.getString(rs.getColumnIndex("name"))
			list.add(Ctgr(idx, name))
		}
		rs.close()
		rd.close()

		return list
	}

	@SuppressLint("Range")
	fun selectCtgrName(memoCtgr: String?): String {
		// viewmemo
		val sql = "select name from ctgr where idx = '"+ memoCtgr +"'"
		val rd = readableDatabase
		val rs = rd.rawQuery(sql, null)
		var name = ""

		if (rs.moveToNext()) {
			name = rs.getString(rs.getColumnIndex("name"))
		}
		rs.close()
		rd.close()

		return name
	}

	// <<DELETE>>----------------------------------------------------------------------------------------------------------------------------------------
	// <MEMO>
	fun deleteMemo(memo: Memo) { // 휴지통에서 메모 삭제
		// 삭제 할 데이터 보다 우선순위가 큰 경우 -1
		val wd1 = writableDatabase
		val sql1 = "update memo set priority = priority - 1 where ctgr = '" + memo.ctgr + "' and priority>'" + memo.priority + "'"
		wd1.execSQL(sql1)
		wd1.close()

		// 데이터 삭제
		val wd = writableDatabase
		val sql = "delete from memo where idx = '" + memo.idx + " '"

		wd.execSQL(sql)
		wd.close()
	}

	fun deleteMemoCtgr(memoIdx: String) { // ?????????????????????
		val memo = selectMemo(memoIdx)
		val priority = checkTopPriority(0) + 1
		val wd = writableDatabase
		val sql = "update memo set priority = '" + priority + "', ctgr = 0 where idx = '" + memoIdx + "'"
		val sql1 = "update memo set priority = priority - 1 where ctgr = '" + memo.ctgr + "' and priority>'" + memo.priority + "'"
		wd.execSQL(sql1)
		wd.execSQL(sql)

		wd.close()
	}

	// <CTGR>
	fun deleteCtgr(ctgrIdx: String) {
		// 카테고리만 삭제
		val memoList = selectMemoList(ctgrIdx)
		var unclassifiedTop = checkTopPriority(0)
		// 미분류로 이동
		val wd = writableDatabase
		val sql1 = "update memo set ctgr = 0 where ctgr = '" + ctgrIdx + " '"
		wd.execSQL(sql1)
		// 카테고리 삭제
		val sql2 = "delete from ctgr where idx = '" + ctgrIdx + " '"
		wd.execSQL(sql2)
		// 기존의 미분류에서 우선순위 조정
		for (memo in memoList){
			var sql3 = "update memo set priority = '" + ++unclassifiedTop + "' where idx = '" + memo.idx + " '"
			wd.execSQL(sql3)
		}
		wd.close()
	}

	// <<CHECK>>----------------------------------------------------------------------------------------------------------------------------------------
	@SuppressLint("Range")
	fun isMemoExist(ctgrIdx:String): Boolean{
		// 카테고리에 메모가 있다면 true 반환
		var result = false
		var flag: Int? = null
		val sql = "select exists(select * from memo where ctgr = '" + ctgrIdx + "' and status = 1) as ex"
		val rd = readableDatabase
		val rs = rd.rawQuery(sql, null)
		while (rs.moveToNext()) {
			flag = rs.getInt(rs.getColumnIndex("ex"))
		}
		if (flag == 1){
			result = true
		}
		rs.close()
		rd.close()
		return result
	}

	@SuppressLint("Range")
	fun checkTopPriority(ctgr: Int): Int {
		// 특정 카테고리의 priority 최상위값을 반환
		val sql = "select priority from memo where ctgr = '" + ctgr + "' order by priority desc limit 1"
		val rd = readableDatabase
		val rs = rd.rawQuery(sql, null)
		var result = 0

		if (rs.moveToNext()) {
			result = rs.getInt(rs.getColumnIndex("priority"))
		}
		rs.close()
		rd.close()
		return result
	}

	@SuppressLint("Range")
	fun checkDuplicationCtgr(ctgrname: String): Boolean{
		// ctgrname 중복 확인
		val sql = "select exists(select * from ctgr where name = '" + ctgrname + "') as dup"
		val rd = readableDatabase
		val rs = rd.rawQuery(sql, null)
		var result = false
		var flag = 0
		while (rs.moveToNext()) {
			flag = rs.getInt(rs.getColumnIndex("dup"))
		}
		if (flag == 1){
			result = true
		}
		rs.close()
		rd.close()
		return result
	}

	fun checkMemoListSize(ctgrIdx: Int?): String {
		// 카테고리에 몇 개의 메모가 있는지
		val sql = "select * from memo where ctgr = '"+ ctgrIdx +"' and status = 1"
		val rd = readableDatabase
		val rs = rd.rawQuery(sql, null)
		var size = 0

		while (rs.moveToNext()) {
			size++
		}
		rs.close()
		rd.close()

		return size.toString()
    }
}

data class Ctgr(var idx: Int?, var name: String)
data class Memo(var idx: Int?, var title: String?, var content: String, var contentAttribute: String, var datetime: Long, var ctgr: Int, var priority: Int, var status: Int, var sel: Boolean = false)
data class SpinnerModel(val image: Int, val name: String)