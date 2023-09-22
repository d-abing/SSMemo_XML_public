package com.aube.ssgmemo.callback

interface ItemTouchHelperListener {
    fun onItemMove(from : Int,to:Int) : Boolean
    fun onItemDrag()
}