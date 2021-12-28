package com.nbow.advanceeditorpro

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class FragmentAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager,lifecycle) {

    lateinit var fragmentList : MutableList<EditorFragment>

    fun addFragment(fragment:EditorFragment){
        fragmentList.add(fragment)

    }

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): EditorFragment {
        return fragmentList[position]
    }

    override fun getItemId(position: Int): Long {
        return fragmentList.get(position).hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        for(frag in fragmentList){
            if(frag.hashCode().toLong()==itemId)return true
        }
        return false
    }
}