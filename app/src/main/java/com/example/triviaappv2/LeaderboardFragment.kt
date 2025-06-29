package com.example.triviaappv2

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * This is the LeaderBoard fragment which is used to display the leaderboard using a recyclerview.
 * This class uses Firebase Firestore which can be found at:
 * https://firebase.google.com/docs/firestore
 */
class LeaderboardFragment : Fragment() {
	private val database = Firebase.firestore
	private var leaderBoardList = ArrayList<ArrayList<String>>()
	private lateinit var view: View
	private lateinit var recyclerView: RecyclerView

	/**
	 * Creates the view of the fragment.
	 * @param inflater The layout inflater.
	 * @param container The parent view of the fragment.
	 * @param savedInstanceState The saved instance state of the fragment.
	 */
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		view = inflater.inflate(R.layout.fragment_leaderboard, container, false)!!
		recyclerView = view.findViewById(R.id.leaderboard)
		getLeaderboardData {
			Log.i("myLog", "Leaderboard data retrieved")
			makeRecyclerView()
		}

		val searchView = view.findViewById<SearchView>(R.id.leaderboard_search)

		searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
			/**
			 * Called when the user submits the query.
			 * @param query The query that the user submitted.
			 * @return True if the query was handled.
			 */
			override fun onQueryTextSubmit(query: String?): Boolean {
				leaderBoardList.clear()
				getSearchedLeaderboardData(query!!) {
					makeRecyclerView()
				}
				hideKeyboard()
				return true
			}

			/**
			 * Called when the user changes the query text.
			 * @param newText The new query text.
			 * @return True if the query text was handled.
			 */
			override fun onQueryTextChange(newText: String?): Boolean {
				leaderBoardList.clear()
				getSearchedLeaderboardData(newText!!) {
					makeRecyclerView()
				}
				return true
			}
		})
		return view
	}

	/**
	 * Parses all the leaderboard data into a 2D array.
	 * uses Firebase Firestore which can be found at:
	 * https://firebase.google.com/docs/firestore
	 *
	 * @return A 2D array of the leaderboard data.
	 */
	private fun getLeaderboardData(callback: (ArrayList<ArrayList<String>>) -> Unit) {
		database.collection("users").orderBy(
			"points",
			Query.Direction.DESCENDING
		).get()
			.addOnSuccessListener { documents ->
				for (document in documents) {
					// Add each user to the leaderboard
					val leaderboardRow = ArrayList<String>()
					leaderboardRow.add(document.getString("profilePictureUrl")!!)
					leaderboardRow.add(document.getString("username")!!)
					leaderboardRow.add(document.getLong("points").toString())
					leaderboardRow.add(document.getString("rank")!!)
					leaderBoardList.add(leaderboardRow)
				}
				callback(leaderBoardList)
			}
	}

	/**
	 * Parses all the leaderboard data that the user has queried into a 2D array.
	 * uses Firebase Firestore which can be found at:
	 * https://firebase.google.com/docs/firestore
	 *
	 * @return A 2D array of the leaderboard data.
	 */
	private fun getSearchedLeaderboardData(
		query: String, callback: (ArrayList<ArrayList<String>>)
		-> Unit
	) {
		database.collection("users").whereGreaterThanOrEqualTo(
			"username",
			query.trim()
		)
			.whereLessThanOrEqualTo("username", query.trim() + "\uf8ff").get()
			.addOnSuccessListener { documents ->
				for (document in documents) {
					// Add each user to the leaderboard
					val leaderboardRow = ArrayList<String>()
					leaderboardRow.add(document.getString("profilePictureUrl")!!)
					leaderboardRow.add(document.getString("username")!!)
					leaderboardRow.add(document.getLong("points").toString())
					leaderboardRow.add(document.getString("rank")!!)
					leaderBoardList.add(leaderboardRow)
				}
				callback(leaderBoardList)
			}
	}

	/**
	 * Creates the recyclerview with the leaderboard data.
	 */
	private fun makeRecyclerView() {
		val layoutManager = LinearLayoutManager(this.context)
		recyclerView.layoutManager = layoutManager
		val adapter = Adapter(leaderBoardList)
		recyclerView.adapter = adapter
	}

	/**
	 * hideKeyboard is called when the user clicks the signup or login button to hide the keyboard.
	 * This is taken from CSC306 Lecture 12 by Tom Owen found at: https://canvas.swansea.ac.uk
	 * /courses/52677/pages/lecture-12-firebase?module_item_id=2984546
	 */
	private fun hideKeyboard() {
		val view = requireActivity().currentFocus
		if (view != null) {
			// Hide the keyboard using the input method manager.
			val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE)
					as InputMethodManager
			imm.hideSoftInputFromWindow(view.windowToken, 0)
		}
	}
}