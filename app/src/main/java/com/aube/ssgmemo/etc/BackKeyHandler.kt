package com.aube.ssgmemo.etc

import android.app.Activity
import com.aube.ssgmemo.R
import com.aube.ssgmemo.callback.CallbackListener
import io.github.muddz.styleabletoast.StyleableToast

class BackKeyHandler(private val activity: Activity) {
    private var backKeyPressedTime: Long = 0
    private var toast: StyleableToast? = null

    fun onBackPressed() {
        if (System.currentTimeMillis() > backKeyPressedTime + 3000) {
            backKeyPressedTime = System.currentTimeMillis()
            toast = StyleableToast.makeText(activity, "저장되지 않은 메모는 사라집니다", R.style.toast)
            toast!!.show()
            return
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 3000) {
            toast!!.cancel()
            activity.finish()
        }
    }
}