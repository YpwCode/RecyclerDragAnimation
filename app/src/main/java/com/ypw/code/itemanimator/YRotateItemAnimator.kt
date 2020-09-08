package com.ypw.code.itemanimator

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorCompat

class YRotateItemAnimator : YBaseItemAnimator() {

    override fun onSetAddAnimator(view: View, animator: ViewPropertyAnimatorCompat) {
        println(">>>>>>>>>>>>>>>>>>>> onSetAddAnimator <<<<<<<<<<<<<<<<<<<<")
        ViewCompat.setPivotX(view, view.width.toFloat())
        ViewCompat.setPivotY(view, view.height / 2f)
        ViewCompat.setRotationY(view, -90f)
        animator.rotationY(0f)
    }
    override fun onSetRemoveAnimator(view: View, animator: ViewPropertyAnimatorCompat) {
        println(">>>>>>>>>>>>>>>>>>>> onSetRemoveAnimator <<<<<<<<<<<<<<<<<<<<")
        ViewCompat.setPivotX(view, 0f)
        ViewCompat.setPivotY(view, view.height / 2f)
        animator.rotationY(90f)
    }
    override fun onSetOldChangAnimator(view: View, animator: ViewPropertyAnimatorCompat) {
        println(">>>>>>>>>>>>>>>>>>>> onSetOldChangAnimator <<<<<<<<<<<<<<<<<<<<")
        ViewCompat.setPivotX(view, view.width / 2f)
        ViewCompat.setPivotY(view, view.height / 2f)
        ViewCompat.setRotationY(view, 0f)
        animator.rotationY(90f).startDelay = 0
    }
    override fun onSetNewChangAnimator(view: View, animator: ViewPropertyAnimatorCompat) {
        println(">>>>>>>>>>>>>>>>>>>> onSetNewChangAnimator <<<<<<<<<<<<<<<<<<<<")
        ViewCompat.setPivotX(view, view.width / 2f)
        ViewCompat.setPivotY(view, view.height / 2f)
        ViewCompat.setRotationY(view, -90f)
        animator.rotationY(0f).startDelay = changeDuration
    }
    override fun onResetAnimator(view: View) {
        println(">>>>>>>>>>>>>>>>>>>> onResetAnimator <<<<<<<<<<<<<<<<<<<<")
        ViewCompat.setRotationY(view, 0f)
    }
}