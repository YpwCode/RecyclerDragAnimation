package com.ypw.code.itemanimator

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private val mList = arrayListOf<Bean>()

    private val mAdapter by lazy {
        RvAdapter().apply { setData(mList) }
    }

    private var callBack: ItemTouchHelper.Callback = object : ItemTouchHelper.Callback() {

        /**
         * Item是否能被Swipe到dismiss
         * 也就是删除这条数据
         */
        override fun isItemViewSwipeEnabled(): Boolean {
            return false
        }

        /**
         * Item长按是否可以拖拽
         */
        override fun isLongPressDragEnabled(): Boolean {
            return mAdapter.animatable
        }

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val dragFlag = ItemTouchHelper.START or ItemTouchHelper.END or ItemTouchHelper.UP or ItemTouchHelper.DOWN
            val swipeFlag: Int = ItemTouchHelper.START or ItemTouchHelper.END
            return makeMovementFlags(dragFlag, swipeFlag)
        }

        /**
         * 拖动后回调
         */
        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            Collections.swap(mList, viewHolder.adapterPosition, target.adapterPosition)
            mAdapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
            return false
        }

        /**
         * 滑动删除
         */
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            println("onSwiped")
//            mList.remove(viewHolder.adapterPosition)
//            mAdapter.notifyItemRemoved(viewHolder.adapterPosition)
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            if (ItemTouchHelper.ACTION_STATE_DRAG == actionState) {
                viewHolder?.itemView?.let {
                    var animator = it.tag as ObjectAnimator?
                    animator?.cancel()
                    it.rotation = 0f
                    it.scaleX = 1.1f
                    it.scaleY = 1.1f
                    it.isSelected = true
                }
            }
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            viewHolder.itemView.let {
                it.rotation = 0f
                it.scaleX = 1f
                it.scaleY = 1f
                it.isSelected = false
                var animator = it.tag as ObjectAnimator?
                animator?.start()
            }
            mAdapter.notifyItemRangeChanged(0, mList.size)
        }
    }

    private val itemTouchHelper = ItemTouchHelper(callBack)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        for (i in 0..10) {
            mList.add(Bean("item: $i"))
        }

        rv.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = mAdapter
            itemAnimator = YNotItemAnimator()
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    fun onClick(v: View) {
        when(v.id) {
            R.id.tv_edit -> {
                mAdapter.animatable = !mAdapter.animatable
                if (mAdapter.animatable) {
                    tv_edit.text = "完成"
                } else {
                    tv_edit.text = "管理"
                }
                mAdapter.notifyDataSetChanged()
            }
        }
    }
}