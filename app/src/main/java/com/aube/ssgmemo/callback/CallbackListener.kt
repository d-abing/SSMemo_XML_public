package com.aube.ssgmemo.callback

import android.view.View

interface CallbackListener {
    fun moveCtgr(ctgrIdx: Int) {}
    fun completeFragmentOpen(memoIdx: Int) {}
    fun fragmentOpen(item: String, ctgrIdx: String?) {}
    fun addCtgr(ctgrName: String) {}
    fun deleteCtgr(ctgrIdx: String) {}
    fun openKeyBoard(view: View) {}
    fun closeKeyBoard() {}
    fun deleteMemoList() {}
    fun completeMemoList() {}
    fun moveCtgrList(oldCtgrIdx: Int, newCtgrIdx: Int) {}
    fun moveCtgr(memoIdx: Int?, ctgrIdx: Int, status: Int) {}
    fun completeMemo(memoIdx: Int) {}
}