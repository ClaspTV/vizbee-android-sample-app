package com.demo.myverizonapp.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.util.Arrays

abstract class BaseRecyclerAdapter<O, V : RecyclerView.ViewHolder?> protected constructor() : RecyclerView.Adapter<V>() {
    protected var items: MutableList<O> = ArrayList()

    init {
        setHasStableIds(true)
    }

    fun getView(parent: ViewGroup, layout: Int): View {
        return LayoutInflater.from(parent.context).inflate(layout, parent, false)
    }

    fun add(`object`: O) {
        items.add(`object`)
        notifyDataSetChanged()
    }

    fun add(index: Int, `object`: O) {
        items.add(index, `object`)
        notifyDataSetChanged()
    }

    fun addAll(collection: Collection<O>?) {
        if (collection != null) {
            items.addAll(collection)
            notifyDataSetChanged()
        }
    }

    fun addAll(vararg items: O) {
        addAll(Arrays.asList(*items))
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    fun remove(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun remove(`object`: O) {
        items.remove(`object`)
        notifyDataSetChanged()
    }

    fun remove(objects: List<O>?) {
        items.removeAll(objects!!)
        notifyDataSetChanged()
    }

    fun getItem(position: Int): O {
        //Temporary fix for position is becoming -1
        return if (position < 0) {
            items[0]
        } else items[position]
    }

    val all: List<O>
        get() = items

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    fun update(position: Int, `object`: O) {
        items[position] = `object`
        notifyItemChanged(position)
    }

    fun updateAll(items: MutableList<O>) {
        this.items = items
        notifyDataSetChanged()
    }
}