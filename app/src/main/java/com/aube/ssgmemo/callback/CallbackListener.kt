package com.aube.ssgmemo.callback

import android.view.View

interface CallbackListener {
    fun moveCtgr(ctgrIdx: Int) {}
    fun fragmentOpen(memoIdx: Int) {}
    fun fragmentOpen(item:String, ctgrIdx: String?) {}
    fun fragmentOpen(ctgrIdx: Int, memoIdx: String){}
    fun fragmentOpen(ctgrIdx:String, memoIdx:String, isList:Boolean) {}
    fun addCtgr(ctgrName:String) {}
    fun deleteCtgr(ctgrIdx: String){}
    fun deleteMemo(memoIdx: String){}
    fun openKeyBoard(view:View){}
    fun closeKeyBoard(){}
    fun deleteMemoList(){}
    fun deleteCtgrList(){}
    fun moveCtgrList(oldCtgrIdx: Int, newCtgrIdx: Int){}
    fun moveCtgr(memoIdx: Int?, ctgrIdx: Int, status: Int){}
    fun completeMemo(memoIdx: Int){}
}