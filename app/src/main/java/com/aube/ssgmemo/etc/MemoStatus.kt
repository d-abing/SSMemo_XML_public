package com.aube.ssgmemo.etc


enum class MemoStatus(val code: Int) {
    NORMAL(1),
    COMPLETED(2),
    DELETED(-1)
}