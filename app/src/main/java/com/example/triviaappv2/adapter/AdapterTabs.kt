package com.example.triviaappv2.adapter

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.triviaappv2.LeaderboardFragment
import com.example.triviaappv2.ProfileFragment
import com.example.triviaappv2.QuizzesFragment
import com.example.triviaappv2.StatsFragment

/**
 * This is an adapter used with the viewpager in the home page to cycle through the four fragments
 * in a tabbed view. Code for this class has been adapted from the example taken from lecture 9
 * of CSC306: https://canvas.swansea.ac.uk/courses/52677/pages/lecture-9-app-bar-
 * and-fragments-2?module_item_id=2984533 by Tom Owen.
 */
class AdapterTabs(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

	/**
	 * Returns the number of fragments in the tabbed view.
	 * @return the number of fragments
	 */
	override fun getItemCount(): Int {
		return 4
	}

	/**
	 * Returns the fragment at the given position.
	 * @param position the position of the fragment
	 * @return the fragment at the given position
	 */
	override fun createFragment(position: Int): Fragment {
		when (position) {
			0 -> return QuizzesFragment()
			1 -> return LeaderboardFragment()
			2 -> return StatsFragment()
			3 -> return ProfileFragment()
		}
		return QuizzesFragment()
	}
}