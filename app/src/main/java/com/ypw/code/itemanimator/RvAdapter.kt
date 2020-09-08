package com.ypw.code.itemanimator

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RvAdapter : RecyclerView.Adapter<RvAdapter.VH>() {

    private var mList = arrayListOf<Bean>()

    private var selected = -1

//    private val objectAnimator by lazy {
//        ObjectAnimator.ofFloat(iView, "rotation", -2f, 0f, 2f).apply {
//            duration = 150
//            repeatCount = ObjectAnimator.INFINITE
//            repeatMode = ObjectAnimator.REVERSE
//        }
//    }

    var animatable = false

    fun setData(list: ArrayList<Bean>) {
        mList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        println(">>>>>>>>>>>>>>>>> onCreateViewHolder <<<<<<<<<<<<<<<<<<<")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_item, null)
        return VH(view)
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        println(">>>>>>>>>>>>>>>>> onBindViewHolder: $position <<<<<<<<<<<<<<<<<<<")

        val bean = mList[position]
        holder.tv.text = bean.name
        holder.ivDelete.setOnClickListener {
            mList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(0, mList.size)
        }
        if (animatable) {
            holder.ivDelete.visibility = View.VISIBLE
        } else {
            holder.ivDelete.visibility = View.GONE
        }

        val iView = holder.itemView

        var animator = iView.tag as ObjectAnimator?
        if (null == animator) {
            animator = ObjectAnimator.ofFloat(iView, "rotation", -2f, 0f, 2f).apply {
                duration = 150
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
            }
            iView.tag = animator
        }
        if (animatable && selected != position) {
            animator?.start()
        } else {
            animator?.cancel()
            iView.rotation = 0f
        }
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tv: TextView = itemView.findViewById(R.id.tv)
        val ivDelete: ImageView = itemView.findViewById(R.id.iv_delete)
    }

}