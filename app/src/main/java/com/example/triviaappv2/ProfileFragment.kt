package com.example.triviaappv2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * The profile fragment shows the user's profile information and where they can edit it.
 * This class uses firebase firestore, firebase auth and glide which can be found at:
 * https://firebase.google.com/docs/firestore,
 * https://firebase.google.com/docs/auth,
 * https://github.com/bumptech/glide
 */
class ProfileFragment : Fragment() {
	private var auth = FirebaseAuth.getInstance()
	private var loggedInUser = auth.currentUser
	private val database = Firebase.firestore
	private lateinit var view: View
	private lateinit var imageView: ImageView
	private lateinit var username: TextView
	private lateinit var level: TextView
	private lateinit var rank: TextView
	private lateinit var points: TextView
	private lateinit var levelProgressCircle: CircularProgressIndicator
	private var nextLevel = 0
	private var xpNeeded = 0
	private var currentXp = 0
	private var currentLevel = 0
	private var xpNeededForCurrent = 0

	/**
	 * onCreateView is used to inflate the fragment's view and calls the loadProfile function.
	 * @param inflater is used to inflate the fragment's view.
	 * @param container is the parent view that the fragment's UI should be attached to.
	 * @param savedInstanceState is the saved instance state of the fragment.
	 *
	 * @return the inflated view.
	 */
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		view = inflater.inflate(R.layout.fragment_profile, container, false)!!
		imageView = view.findViewById(R.id.profile_pic)
		val editProfileButton = view.findViewById<Button>(R.id.edit_profile)
		val freshStartButton = view.findViewById<Button>(R.id.fresh_start)
		val pastResponsesButton = view.findViewById<Button>(R.id.see_past_responses)
		levelProgressCircle = view.findViewById(R.id.level_progress_circle)
		username = view.findViewById(R.id.username_profile)
		level = view.findViewById(R.id.level)
		rank = view.findViewById(R.id.rank)
		points = view.findViewById(R.id.points)

		editProfileButton.setOnClickListener {
			val editIntent = Intent(requireContext(), EditProfile::class.java)
			startActivity(editIntent)
		}

		getCurrentUserLevelAndXp { level, xp ->
			nextLevel = level + 1
			xpNeeded = calculateXpNeededToLevelUp()
			currentLevel = level
			currentXp = xp
			xpNeededForCurrent = calculateXpNeededForCurrent()
			levelProgressCircle.progress = calculateLevelProgress()
			loadProfile()
			loadProfilePic()
		}

		freshStartButton.setOnClickListener {
			resetStats()
			loadProfile()
			levelProgressCircle.progress = 0
		}

		pastResponsesButton.setOnClickListener {
			val intent = Intent(requireContext(), PastResponsesScreen::class.java)
			startActivity(intent)
			requireActivity().finish()
		}

		return view
	}

	/**
	 * loadProfile is used to load the user's profile information from the database.
	 * Uses firebase firestore and firebase auth which can be found at:
	 * https://firebase.google.com/docs/firestore,
	 * https://firebase.google.com/docs/auth
	 */
	private fun loadProfile() {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).get()
			.addOnSuccessListener { document ->
				if (document != null) {
					// User document exists, display its data.
					username.text = document.getString("username").toString()
					level.text = document.getLong("level").toString()
					rank.text = document.getString("rank").toString()
					points.text = document.getLong("points").toString()
				} else {
					// User document does not exist, display an error message.
					val snack = Snackbar.make(view, "No user exists", Snackbar.LENGTH_LONG)
					snack.show()
				}
			}
			.addOnFailureListener {
				val snack = Snackbar.make(view, "Error loading profile", Snackbar.LENGTH_LONG)
				snack.show()
			}
	}

	/**
	 * resetStats is used to reset the user's stats in the database when they click the fresh start.
	 * Uses firebase firestore and firebase auth which can be found at:
	 * https://firebase.google.com/docs/firestore,
	 * https://firebase.google.com/docs/auth
	 */
	private fun resetStats() {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).update("level", 1)
		database.collection("users").document(userId).update("xp", 0)
		database.collection("users").document(userId).update(
			"rank",
			"Bronze"
		)
		database.collection("users").document(userId).update("points", 0)
	}

	/**
	 * Gets the user's level and xp from the database. Uses firebase firestore and firebase auth
	 * which can be found at: https://firebase.google.com/docs/firestore,
	 * https://firebase.google.com/docs/auth
	 *
	 * @param callback returns the user's level and xp.
	 */
	private fun getCurrentUserLevelAndXp(callback: (Int, Int) -> Unit) {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).get()
			.addOnSuccessListener { document ->
				if (document != null) {
					// User document exists, get level and xp and return them in callback.
					val level = document.getLong("level")!!.toInt()
					val xp = document.getLong("xp")!!.toInt()
					callback(level, xp)
				}
			}
	}

	/**
	 * calculateXpNeededToLevelUp is used to calculate the amount of xp needed to level up.
	 * @return xp needed to level up.
	 */
	private fun calculateXpNeededToLevelUp(): Int {
		return ((nextLevel * kotlin.math.log2(nextLevel.toDouble())) * (5.0 / 2.0)).toInt()
	}

	/**
	 * calculateXpNeededForCurrent is used to calculate the amount of xp needed
	 * for the current level.
	 * @return xp needed for current level.
	 */
	private fun calculateXpNeededForCurrent(): Int {
		return ((currentLevel * kotlin.math.log2(currentLevel.toDouble())) * (5.0 / 2.0)).toInt()
	}

	/**
	 * calculateLevelProgress is used to calculate the progress of the user's level.
	 * @return progress of the user's level.
	 */
	private fun calculateLevelProgress(): Int {
		val range = xpNeeded - xpNeededForCurrent
		val progress = currentXp - xpNeededForCurrent
		return (progress.toDouble() / range.toDouble() * 100.0).toInt()
	}

	/**
	 * loadProfilePic is used to load the users current profile pic and display it.
	 * Uses firebase firestore, firebase auth and Glide which can be found at:
	 * https://firebase.google.com/docs/firestore,
	 * https://firebase.google.com/docs/auth,
	 * https://github.com/bumptech/glide
	 */
	private fun loadProfilePic() {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).get()
			.addOnSuccessListener { document ->
				if (document != null) {
					// User document exists, display profile pic.
					val profilePicUrl = document.getString("profilePictureUrl")
					if (profilePicUrl != "") {
						// User has a custom profile pic, load it.
						Glide.with(requireContext()).load(profilePicUrl).into(imageView)
					} else {
						// User does not have a custom profile pic, use default.
						imageView.setImageResource(R.mipmap.default_profile_pic)
					}
				}
			}
	}
}