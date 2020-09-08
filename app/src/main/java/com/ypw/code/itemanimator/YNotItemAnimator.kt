package com.ypw.code.itemanimator

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorCompat

class YNotItemAnimator : YBaseItemAnimator() {

    override fun onSetAddAnimator(view: View, animator: ViewPropertyAnimatorCompat) {
        println(">>>>>>>>>>>>>>>>>>>> onSetAddAnimator <<<<<<<<<<<<<<<<<<<<")
    }
    override fun onSetRemoveAnimator(view: View, animator: ViewPropertyAnimatorCompat) {
        println(">>>>>>>>>>>>>>>>>>>> onSetRemoveAnimator <<<<<<<<<<<<<<<<<<<<")
    }
    override fun onSetOldChangAnimator(view: View, animator: ViewPropertyAnimatorCompat) {
        println(">>>>>>>>>>>>>>>>>>>> onSetOldChangAnimator <<<<<<<<<<<<<<<<<<<<")
    }
    override fun onSetNewChangAnimator(view: View, animator: ViewPropertyAnimatorCompat) {
        println(">>>>>>>>>>>>>>>>>>>> onSetNewChangAnimator <<<<<<<<<<<<<<<<<<<<")
    }
    override fun onResetAnimator(view: View) {
        println(">>>>>>>>>>>>>>>>>>>> onResetAnimator <<<<<<<<<<<<<<<<<<<<")
    }

}