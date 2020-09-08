package com.ypw.code.itemanimator

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.view.ViewPropertyAnimatorListenerAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.SimpleItemAnimator
import java.util.*

abstract class YBaseItemAnimator : SimpleItemAnimator() {

    class MoveInfo(
        var holder: ViewHolder?,
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int
    )

    class ChangeInfo(
        var oldHolder: ViewHolder?,
        var newHolder: ViewHolder?,
        val fromX: Int,
        val fromY: Int,
        val toX: Int,
        val toY: Int
    )

    private val DEBUG = false

    private val sDefaultInterpolator: TimeInterpolator by lazy {
        ValueAnimator().interpolator
    }

    private val mPendingRemovals = ArrayList<ViewHolder>()
    private val mPendingAdditions = ArrayList<ViewHolder>()
    private val mPendingMoves = ArrayList<MoveInfo>()
    private val mPendingChanges = ArrayList<ChangeInfo>()

    var mAdditionsList = ArrayList<ArrayList<ViewHolder>>()
    var mMovesList = ArrayList<ArrayList<MoveInfo>>()
    var mChangesList = ArrayList<ArrayList<ChangeInfo>>()

    var mAddAnimations = ArrayList<ViewHolder>()
    var mMoveAnimations = ArrayList<ViewHolder>()
    var mRemoveAnimations = ArrayList<ViewHolder>()
    var mChangeAnimations = ArrayList<ViewHolder>()

    override fun runPendingAnimations() {
        val removalsPending = mPendingRemovals.isNotEmpty()
        val movesPending = mPendingMoves.isNotEmpty()
        val changesPending = mPendingChanges.isNotEmpty()
        val additionsPending = mPendingAdditions.isNotEmpty()
        if (!removalsPending && !movesPending && !additionsPending && !changesPending) {
            // nothing to animate
            return
        }
        // First, remove stuff
        for (holder in mPendingRemovals) {
            animateRemoveImpl(holder)
        }
        mPendingRemovals.clear()
        // Next, move stuff
        if (movesPending) {
            val moves = ArrayList<MoveInfo>()
            moves.addAll(mPendingMoves)
            mMovesList.add(moves)
            mPendingMoves.clear()
            val mover = Runnable {
                for (moveInfo in moves) {
                    animateMoveImpl(
                        moveInfo.holder,
                        moveInfo.fromX,
                        moveInfo.fromY,
                        moveInfo.toX,
                        moveInfo.toY
                    )
                }
                moves.clear()
                mMovesList.remove(moves)
            }
            if (removalsPending) {
                val view = moves[0].holder?.itemView!!
                ViewCompat.postOnAnimationDelayed(view, mover, removeDuration)
            } else {
                mover.run()
            }
        }
        // Next, change stuff, to run in parallel with move animations
        if (changesPending) {
            val changes = ArrayList<ChangeInfo>()
            changes.addAll(mPendingChanges)
            mChangesList.add(changes)
            mPendingChanges.clear()
            val changer = Runnable {
                for (change in changes) {
                    animateChangeImpl(change)
                }
                changes.clear()
                mChangesList.remove(changes)
            }
            if (removalsPending) {
                val view = changes[0].oldHolder?.itemView!!
                ViewCompat.postOnAnimationDelayed(view, changer, removeDuration)
            } else {
                changer.run()
            }
        }
        // Next, add stuff
        if (additionsPending) {
            val additions = ArrayList<ViewHolder>()
            additions.addAll(mPendingAdditions)
            mAdditionsList.add(additions)
            mPendingAdditions.clear()
            val adder = Runnable {
                for (holder in additions) {
                    animateAddImpl(holder)
                }
                additions.clear()
                mAdditionsList.remove(additions)
            }
            if (removalsPending || movesPending || changesPending) {
                val removeDuration = if (removalsPending) removeDuration else 0
                val moveDuration = if (movesPending) moveDuration else 0
                val changeDuration = if (changesPending) changeDuration else 0
                val totalDelay =
                    removeDuration + Math.max(moveDuration, changeDuration)
                val view = additions[0].itemView
                ViewCompat.postOnAnimationDelayed(view, adder, totalDelay)
            } else {
                adder.run()
            }
        }
    }

    override fun animateRemove(holder: ViewHolder): Boolean {
        resetAnimation(holder)
        mPendingRemovals.add(holder)
        return true
    }

    private fun animateRemoveImpl(holder: ViewHolder) {
        val view = holder.itemView
        val animation = ViewCompat.animate(view)
        mRemoveAnimations.add(holder)

        /**
         * *************************
         * 设置删除动画属性
         * *************************
         */
        onSetRemoveAnimator(view, animation)

        animation.setDuration(removeDuration).setListener(
            object : ViewPropertyAnimatorListenerAdapter() {

                override fun onAnimationStart(v: View?) {
                    dispatchRemoveStarting(holder)
                }

                override fun onAnimationEnd(v: View?) {
                    /**
                     * *************************
                     * 还原动画属性
                     * *************************
                     */
                    onResetAnimator(view)

                    animation.setListener(null)
                    dispatchRemoveFinished(holder)
                    mRemoveAnimations.remove(holder)
                    dispatchFinishedWhenDone()
                }
            }).start()
    }

    override fun animateAdd(holder: ViewHolder): Boolean {
        resetAnimation(holder)
        mPendingAdditions.add(holder)
        return true
    }

    private fun animateAddImpl(holder: ViewHolder) {
        val view = holder.itemView
        val animation = ViewCompat.animate(view)
        mAddAnimations.add(holder)

        /**
         * *************************
         * 设置添加动画属性
         * *************************
         */
        onSetAddAnimator(view, animation)

        animation.setDuration(addDuration).setListener(
            object : ViewPropertyAnimatorListenerAdapter() {
                override fun onAnimationStart(v: View?) {
                    dispatchAddStarting(holder)
                }

                override fun onAnimationCancel(v: View?) {
                    /**
                     * *************************
                     * 还原动画属性
                     * *************************
                     */
                    onResetAnimator(view)
                }

                override fun onAnimationEnd(view: View?) {
                    animation.setListener(null)
                    dispatchAddFinished(holder)
                    mAddAnimations.remove(holder)
                    dispatchFinishedWhenDone()
                }
            }).start()
    }

    override fun animateMove(
        holder: ViewHolder?, fromX: Int, fromY: Int,
        toX: Int, toY: Int
    ): Boolean {
        var fromX = fromX
        var fromY = fromY
        val view = holder?.itemView!!
        fromX += view.translationX.toInt()
        fromY += view.translationY.toInt()
        resetAnimation(holder)
        val deltaX = toX - fromX
        val deltaY = toY - fromY
        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder)
            return false
        }
        if (deltaX != 0) {
            view.translationX = -deltaX.toFloat()
        }
        if (deltaY != 0) {
            view.translationY = -deltaY.toFloat()
        }
        mPendingMoves.add(MoveInfo(holder, fromX, fromY, toX, toY))
        return true
    }

    private fun animateMoveImpl(
        holder: ViewHolder?,
        fromX: Int,
        fromY: Int,
        toX: Int,
        toY: Int
    ) {
        val view = holder?.itemView!!
        val deltaX = toX - fromX
        val deltaY = toY - fromY
        if (deltaX != 0) {
            view.animate().translationX(0f)
        }
        if (deltaY != 0) {
            view.animate().translationY(0f)
        }
        // TODO: make EndActions end listeners instead, since end actions aren't called when
        // vpas are canceled (and can't end them. why?)
        // need listener functionality in VPACompat for this. Ick.
        val animation = view.animate()
        mMoveAnimations.add(holder)
        animation.setDuration(moveDuration).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animator: Animator) {
                dispatchMoveStarting(holder)
            }

            override fun onAnimationCancel(animator: Animator) {
                if (deltaX != 0) {
                    view.translationX = 0f
                }
                if (deltaY != 0) {
                    view.translationY = 0f
                }
            }

            override fun onAnimationEnd(animator: Animator) {
                animation.setListener(null)
                dispatchMoveFinished(holder)
                mMoveAnimations.remove(holder)
                dispatchFinishedWhenDone()
            }
        }).start()
    }

    override fun animateChange(
        oldHolder: ViewHolder?, newHolder: ViewHolder?,
        fromX: Int, fromY: Int, toX: Int, toY: Int
    ): Boolean {
        if (oldHolder === newHolder) {
            // Don't know how to run change animations when the same view holder is re-used.
            // run a move animation to handle position changes.
            return animateMove(oldHolder, fromX, fromY, toX, toY)
        }
        val oldView = oldHolder?.itemView!!
        val prevTranslationX = oldView.translationX
        val prevTranslationY = oldView.translationY
        val prevAlpha = oldView.alpha
        resetAnimation(oldHolder)
        val deltaX = (toX - fromX - prevTranslationX).toInt()
        val deltaY = (toY - fromY - prevTranslationY).toInt()
        // recover prev translation state after ending animation
        oldHolder.itemView.translationX = prevTranslationX
        oldHolder.itemView.translationY = prevTranslationY
        oldHolder.itemView.alpha = prevAlpha
        if (newHolder != null) {
            // carry over translation values
            resetAnimation(newHolder)
            newHolder.itemView.translationX = -deltaX.toFloat()
            newHolder.itemView.translationY = -deltaY.toFloat()
        }
        mPendingChanges.add(
            ChangeInfo(
                oldHolder,
                newHolder,
                fromX,
                fromY,
                toX,
                toY
            )
        )
        return true
    }

    private fun animateChangeImpl(changeInfo: ChangeInfo) {
        val oldHolder = changeInfo.oldHolder
        val oldView = oldHolder?.itemView!!
        val newHolder = changeInfo.newHolder
        val newView = newHolder?.itemView!!
        if (oldView != null) {
            mChangeAnimations.add(changeInfo.oldHolder!!)

            val oldViewAnim = ViewCompat.animate(oldView)
            oldViewAnim.translationX(changeInfo.toX - changeInfo.fromX.toFloat())
            oldViewAnim.translationY(changeInfo.toY - changeInfo.fromY.toFloat())

            /**
             * *************************
             * 设置数据更新时 原 view 更新动画属性
             * *************************
             */
            onSetOldChangAnimator(oldView, oldViewAnim)

            oldViewAnim.setDuration(changeDuration).setListener(
                object : ViewPropertyAnimatorListenerAdapter() {
                    override fun onAnimationStart(view: View?) {
                        dispatchChangeStarting(changeInfo.oldHolder, true)
                    }

                    override fun onAnimationEnd(view: View?) {
                        /**
                         * *************************
                         * 还原动画属性
                         * *************************
                         */
                        onResetAnimator(oldView)

                        oldViewAnim.setListener(null)
                        oldView.translationX = 0f
                        oldView.translationY = 0f
                        dispatchChangeFinished(changeInfo.oldHolder, true)
                        mChangeAnimations.remove(changeInfo.oldHolder!!)
                        dispatchFinishedWhenDone()
                    }
                }).start()
        }
        if (newView != null) {
            mChangeAnimations.add(changeInfo.newHolder!!)

            val newViewAnimation = ViewCompat.animate(newView)
            newViewAnimation.translationX(0f)
            newViewAnimation.translationY(0f)

            /**
             * *************************
             * 设置数据更新时 新 view 更新动画属性
             * *************************
             */
            onSetNewChangAnimator(newView, newViewAnimation)

            newViewAnimation.setDuration(changeDuration).setListener(
                object : ViewPropertyAnimatorListenerAdapter() {
                    override fun onAnimationStart(view: View?) {
                        dispatchChangeStarting(changeInfo.newHolder, false)
                    }

                    override fun onAnimationEnd(view: View?) {
                        /**
                         * *************************
                         * 还原动画属性
                         * *************************
                         */
                        onResetAnimator(oldView)

                        newViewAnimation.setListener(null)
                        newView.translationX = 0f
                        newView.translationY = 0f
                        dispatchChangeFinished(changeInfo.newHolder, false)
                        mChangeAnimations.remove(changeInfo.newHolder!!)
                        dispatchFinishedWhenDone()
                    }
                }).start()
        }
    }

    private fun endChangeAnimation(
        infoList: MutableList<ChangeInfo>,
        item: ViewHolder
    ) {
        for (i in infoList.indices.reversed()) {
            val changeInfo = infoList[i]
            if (endChangeAnimationIfNecessary(changeInfo, item)) {
                if (changeInfo.oldHolder == null && changeInfo.newHolder == null) {
                    infoList.remove(changeInfo)
                }
            }
        }
    }

    private fun endChangeAnimationIfNecessary(changeInfo: ChangeInfo) {
        if (changeInfo.oldHolder != null) {
            endChangeAnimationIfNecessary(changeInfo, changeInfo.oldHolder)
        }
        if (changeInfo.newHolder != null) {
            endChangeAnimationIfNecessary(changeInfo, changeInfo.newHolder)
        }
    }

    private fun endChangeAnimationIfNecessary(changeInfo: ChangeInfo, item: ViewHolder?): Boolean {
        var oldItem = false
        when {
            changeInfo.newHolder === item -> {
                changeInfo.newHolder = null
            }
            changeInfo.oldHolder === item -> {
                changeInfo.oldHolder = null
                oldItem = true
            }
            else -> {
                return false
            }
        }
        item?.itemView?.let {
            it.alpha = 1f
            it.translationX = 0f
            it.translationY = 0f
        }
        dispatchChangeFinished(item, oldItem)
        return true
    }

    override fun endAnimation(item: ViewHolder) {
        val view = item.itemView
        // this will trigger end callback which should set properties to their target values.
        view.animate().cancel()
        // TODO if some other animations are chained to end, how do we cancel them as well?
        for (i in mPendingMoves.indices.reversed()) {
            val moveInfo: MoveInfo = mPendingMoves[i]
            if (moveInfo.holder === item) {
                view.translationY = 0f
                view.translationX = 0f
                dispatchMoveFinished(item)
                mPendingMoves.removeAt(i)
            }
        }
        endChangeAnimation(mPendingChanges, item)
        if (mPendingRemovals.remove(item)) {
            /**
             * *************************
             * 还原动画属性
             * *************************
             */
            onResetAnimator(item.itemView)

            dispatchRemoveFinished(item)
        }
        if (mPendingAdditions.remove(item)) {
            /**
             * *************************
             * 还原动画属性
             * *************************
             */
            onResetAnimator(item.itemView)

            dispatchAddFinished(item)
        }
        for (i in mChangesList.indices.reversed()) {
            val changes: ArrayList<ChangeInfo> = mChangesList[i]
            endChangeAnimation(changes, item)
            if (changes.isEmpty()) {
                mChangesList.removeAt(i)
            }
        }
        for (i in mMovesList.indices.reversed()) {
            val moves: ArrayList<MoveInfo> = mMovesList[i]
            for (j in moves.indices.reversed()) {
                val moveInfo = moves[j]
                if (moveInfo.holder === item) {
                    view.translationY = 0f
                    view.translationX = 0f
                    dispatchMoveFinished(item)
                    moves.removeAt(j)
                    if (moves.isEmpty()) {
                        mMovesList.removeAt(i)
                    }
                    break
                }
            }
        }
        for (i in mAdditionsList.indices.reversed()) {
            val additions = mAdditionsList[i]
            if (additions.remove(item)) {
                /**
                 * *************************
                 * 还原动画属性
                 * *************************
                 */
                onResetAnimator(item.itemView)

                dispatchAddFinished(item)
                if (additions.isEmpty()) {
                    mAdditionsList.removeAt(i)
                }
            }
        }

        // animations should be ended by the cancel above.
        check(!(mRemoveAnimations.remove(item) && DEBUG)) {
            ("after animation is cancelled, item should not be in "
                    + "mRemoveAnimations list")
        }
        check(!(mAddAnimations.remove(item) && DEBUG)) {
            ("after animation is cancelled, item should not be in "
                    + "mAddAnimations list")
        }
        check(!(mChangeAnimations.remove(item) && DEBUG)) {
            ("after animation is cancelled, item should not be in "
                    + "mChangeAnimations list")
        }
        check(!(mMoveAnimations.remove(item) && DEBUG)) {
            ("after animation is cancelled, item should not be in "
                    + "mMoveAnimations list")
        }
        dispatchFinishedWhenDone()
    }

    private fun resetAnimation(holder: ViewHolder?) {
        holder?.itemView!!.animate().interpolator = sDefaultInterpolator
        endAnimation(holder)
    }

    override fun isRunning(): Boolean {
        return (mPendingAdditions.isNotEmpty()
                || mPendingChanges.isNotEmpty()
                || mPendingMoves.isNotEmpty()
                || mPendingRemovals.isNotEmpty()
                || mMoveAnimations.isNotEmpty()
                || mRemoveAnimations.isNotEmpty()
                || mAddAnimations.isNotEmpty()
                || mChangeAnimations.isNotEmpty()
                || mMovesList.isNotEmpty()
                || mAdditionsList.isNotEmpty()
                || mChangesList.isNotEmpty())
    }

    /**
     * Check the state of currently pending and running animations. If there are none
     * pending/running, call [.dispatchAnimationsFinished] to notify any
     * listeners.
     */
    fun dispatchFinishedWhenDone() {
        if (!isRunning) {
            dispatchAnimationsFinished()
        }
    }

    override fun endAnimations() {
        var count = mPendingMoves.size
        for (i in count - 1 downTo 0) {
            val item: MoveInfo = mPendingMoves[i]
            val view = item.holder?.itemView!!
            view.translationY = 0f
            view.translationX = 0f
            dispatchMoveFinished(item.holder)
            mPendingMoves.removeAt(i)
        }
        count = mPendingRemovals.size
        for (i in count - 1 downTo 0) {
            val item = mPendingRemovals[i]
            dispatchRemoveFinished(item)
            mPendingRemovals.removeAt(i)
        }
        count = mPendingAdditions.size
        for (i in count - 1 downTo 0) {
            val item = mPendingAdditions[i]

            /**
             * *************************
             * 还原动画属性
             * *************************
             */
            onResetAnimator(item.itemView)

            dispatchAddFinished(item)
            mPendingAdditions.removeAt(i)
        }
        count = mPendingChanges.size
        for (i in count - 1 downTo 0) {
            endChangeAnimationIfNecessary(mPendingChanges[i])
        }
        mPendingChanges.clear()
        if (!isRunning) {
            return
        }
        var listCount = mMovesList.size
        for (i in listCount - 1 downTo 0) {
            val moves: ArrayList<MoveInfo> = mMovesList[i]
            count = moves.size
            for (j in count - 1 downTo 0) {
                val moveInfo = moves[j]
                val item = moveInfo.holder
                val view = item?.itemView!!
                view.translationY = 0f
                view.translationX = 0f
                dispatchMoveFinished(moveInfo.holder)
                moves.removeAt(j)
                if (moves.isEmpty()) {
                    mMovesList.remove(moves)
                }
            }
        }
        listCount = mAdditionsList.size
        for (i in listCount - 1 downTo 0) {
            val additions = mAdditionsList[i]
            count = additions.size
            for (j in count - 1 downTo 0) {
                val item = additions[j]

                /**
                 * *************************
                 * 还原动画属性
                 * *************************
                 */
                onResetAnimator(item.itemView)

                dispatchAddFinished(item)
                additions.removeAt(j)
                if (additions.isEmpty()) {
                    mAdditionsList.remove(additions)
                }
            }
        }
        listCount = mChangesList.size
        for (i in listCount - 1 downTo 0) {
            val changes: ArrayList<ChangeInfo> = mChangesList[i]
            count = changes.size
            for (j in count - 1 downTo 0) {
                endChangeAnimationIfNecessary(changes[j])
                if (changes.isEmpty()) {
                    mChangesList.remove(changes)
                }
            }
        }
        cancelAll(mRemoveAnimations)
        cancelAll(mMoveAnimations)
        cancelAll(mAddAnimations)
        cancelAll(mChangeAnimations)
        dispatchAnimationsFinished()
    }

    private fun cancelAll(viewHolders: List<ViewHolder>) {
        for (i in viewHolders.indices.reversed()) {
            viewHolders[i].itemView.animate().cancel()
        }
    }

    // abstract fun onAddAnimatorInit(view: View)
    abstract fun onSetAddAnimator(view: View, animator: ViewPropertyAnimatorCompat)
    abstract fun onSetRemoveAnimator(view: View, animator: ViewPropertyAnimatorCompat)
    abstract fun onSetOldChangAnimator(view: View, animator: ViewPropertyAnimatorCompat)
    abstract fun onSetNewChangAnimator(view: View, animator: ViewPropertyAnimatorCompat)
    abstract fun onResetAnimator(view: View)
}