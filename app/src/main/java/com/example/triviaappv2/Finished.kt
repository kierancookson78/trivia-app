package com.example.triviaappv2

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.properties.Delegates

/**
 * The finished activity is called when the user has finished the quiz.
 * Their score is displayed and the user can go back to the home page.
 * This class uses Firebase Authentication and Firebase Firestore.
 * The libraries can be found at:
 * https://firebase.google.com/docs/auth,
 * https://firebase.google.com/docs/firestore
 */
class Finished : AppCompatActivity() {
	private var score by Delegates.notNull<Int>()
	private var totalQuestions by Delegates.notNull<Int>()
	private var outOfTime = false
	private var rankedQuiz = false
	private var auth = FirebaseAuth.getInstance()
	private var loggedInUser = auth.currentUser
	private val database = Firebase.firestore
	private lateinit var levelProgressCircle: CircularProgressIndicator
	private lateinit var levelText: TextView
	private var nextLevel = 0
	private var xpNeeded = 0
	private var currentXp = 0
	private var currentLevel = 0
	private var xpNeededForCurrent = 0
	private var currentRank = ""
	private var chosenTopic = ""
	private var currentPoints = 0

	/**
	 * onCreate is called when the activity is first created.
	 * @param savedInstanceState is the saved instance state of the activity.
	 */
	@SuppressLint("NewApi")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_finished)
		val toolbar = findViewById<Toolbar>(R.id.toolbar)
		score = intent.getIntExtra("score", "0".toInt())
		totalQuestions = intent.getIntExtra("total_questions", "0".toInt())
		outOfTime = intent.getBooleanExtra("out_of_time", false)
		rankedQuiz = intent.getBooleanExtra("ranked_quiz", false)
		chosenTopic = intent.getStringExtra("topic").toString()
		levelProgressCircle = findViewById(R.id.level_progress_circle)
		levelText = findViewById(R.id.level)
		val gson = Gson()
		val pastResponsesExtra = intent.getStringExtra("responses")
		val pastResponses = gson.fromJson<ArrayList<ArrayList<ArrayList<String>>>>(
			pastResponsesExtra,
			object : TypeToken<ArrayList<ArrayList<ArrayList<String>>>>() {}.type
		)
		val scoreDisplay = findViewById<TextView>(R.id.score_display)
		scoreDisplay.text = getString(R.string.score_display, score, totalQuestions)
		if (outOfTime) {
			// The timed quiz has run out of time. Display message.
			val outOfTimeText = findViewById<TextView>(R.id.out_of_time)
			outOfTimeText.text = getString(R.string.out_of_time_string)
		}

		setSupportActionBar(toolbar)
		toolbar.setNavigationOnClickListener { _ -> back() }
		getCurrentUserLevelAndXp { level, xp ->
			nextLevel = level + 1
			xpNeeded = calculateXpNeededToLevelUp()
			currentLevel = level
			currentXp = xp
			levelText.text = getString(R.string.level_stayed_display, currentLevel)
			xpNeededForCurrent = calculateXpNeededForCurrent()

			if (hasReachedNextLevel()) {
				// The user has reached the next level. Update the level.
				updateLevel()
			}
			updateXp()
			levelProgressCircle.progress = calculateLevelProgress()
			updateNumberOfCorrectAnswersAndQuestions()

			if (rankedQuiz) {
				// The user has completed a ranked quiz. Update the rank and points.
				getCurrentUserRankAndPoints { rank, points ->
					currentRank = rank
					currentPoints = points

					if (hasReachedNextRank()) {
						// The user has reached the next rank. Update the rank.
						updateRank()
						val nextRankText = findViewById<TextView>(R.id.ranked_up)
						nextRankText.text = getString(R.string.ranked_up, nextRank())
					}
					updatePoints()
				}
			}
			uploadResponses(pastResponses)
			if (!rankedQuiz) {
				updateTopicStats()
			}
		}
	}

	/**
	 * back is called when the user clicks the back button.
	 * It takes the user back to the home page.
	 */
	private fun back() {
		val backIntent = Intent(this, HomePage::class.java)
		startActivity(backIntent)
		finish()
	}

	/**
	 * Gets the current user's level and xp from the database.
	 * Uses Firebase Authentication and Firebase Firestore.
	 * The libraries can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 *
	 * @param callback the callback that returns the level and xp.
	 */
	private fun getCurrentUserLevelAndXp(callback: (Int, Int) -> Unit) {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).get()
			.addOnSuccessListener { document ->
				if (document != null) {
					// User exists in the database. Get level and xp.
					val level = document.getLong("level")!!.toInt()
					val xp = document.getLong("xp")!!.toInt()
					callback(level, xp)
				}
			}
	}

	/**
	 * Updates the user's level in the database if they earned enough xp.
	 * Uses Firebase Authentication and Firebase Firestore.
	 * The libraries can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 */
	private fun updateLevel() {
		val userId = loggedInUser!!.uid
		levelText.text = getString(R.string.leveled_up, nextLevel)
		database.collection("users").document(userId).update("level", nextLevel)
		currentLevel++
		nextLevel++
		xpNeeded = calculateXpNeededToLevelUp()
		xpNeededForCurrent = calculateXpNeededForCurrent()
		if (hasReachedNextLevel()) {
			// The user has earned enough xp to level up more than once. Update the level.
			updateLevel()
		}
	}

	/**
	 * Updates the user's xp in the database based on their score.
	 * Uses Firebase Authentication and Firebase Firestore.
	 * The libraries can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 */
	private fun updateXp() {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).update(
			"xp", currentXp + score
		)
	}

	/**
	 * Calculates the amount of xp needed to level up.
	 * @return the amount of xp needed to level up.
	 */
	private fun calculateXpNeededToLevelUp(): Int {
		return ((nextLevel * kotlin.math.log2(nextLevel.toDouble())) * (5.0 / 2.0)).toInt()
	}

	/**
	 * Checks if the user has earned enough xp to level up.
	 * @return true if the user has enough xp to level up, false otherwise.
	 */
	private fun hasReachedNextLevel(): Boolean {
		return currentXp + score >= calculateXpNeededToLevelUp()
	}

	/**
	 * Gets the current user's rank and points from the database.
	 * Uses Firebase Authentication and Firebase Firestore.
	 * The libraries can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 *
	 * @param callback the callback that returns the rank and points.
	 */
	private fun getCurrentUserRankAndPoints(callback: (String, Int) -> Unit) {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).get()
			.addOnSuccessListener { document ->
				if (document != null) {
					// User exists in the database. Get rank and points.
					val rank = document.getString("rank")!!
					val points = document.getLong("points")!!.toInt()
					callback(rank, points)
				}
			}
	}

	/**
	 * Checks if the user has earned enough points to rank up.
	 * @return true if the user has enough points to rank up, false otherwise.
	 */
	private fun hasReachedNextRank(): Boolean {
		val newPoints = currentPoints + (score * 50)
		return newPoints >= pointsNeededForNextRank()
	}

	/**
	 * Calculates the amount of points needed to rank up based on their current rank.
	 * @return the amount of points needed to rank up.
	 */
	private fun pointsNeededForNextRank(): Int {
		var pointsNeeded = 0
		when (currentRank) {
			"Bronze" -> pointsNeeded = 1000
			"Silver" -> pointsNeeded = 2500
			"Gold" -> pointsNeeded = 7500
			"Platinum" -> pointsNeeded = 18000
		}
		return pointsNeeded
	}

	/**
	 * updates the user's rank in the database if they earned enough points to rank up.
	 * Uses Firebase Authentication and Firebase Firestore.
	 * The libraries can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 */
	private fun updateRank() {
		val userId = loggedInUser!!.uid
		val nextRank = nextRank()
		database.collection("users").document(userId).update("rank", nextRank)
	}

	/**
	 * updates the user's points in the database.
	 * Uses Firebase Authentication and Firebase Firestore.
	 * The libraries can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 */
	private fun updatePoints() {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).update(
			"points", currentPoints + (score * 50)
		)
	}

	/**
	 * Gets the next rank based on the current rank.
	 * @return the next rank.
	 */
	private fun nextRank(): String {
		var nextRank = ""
		when (currentRank) {
			"Bronze" -> nextRank = "Silver"
			"Silver" -> nextRank = "Gold"
			"Gold" -> nextRank = "Platinum"
			"Platinum" -> nextRank = "Diamond"
		}
		return nextRank
	}

	/**
	 * uploads the user's past responses from the quiz they just finished to the database.
	 * Uses Firebase Authentication and Firebase Firestore.
	 * The libraries can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 *
	 * @param pastResponses the user's past responses.
	 */
	private fun uploadResponses(pastResponses: ArrayList<ArrayList<ArrayList<String>>>) {
		val userId = loggedInUser!!.uid
		val responseCollection =
			database.collection("users").document(userId)
				.collection("responses")
		for (pastResponse in pastResponses) {
			// Upload each past response to the database.
			val question = pastResponse[0][0]
			val choices = pastResponse[1]
			val userAnswer = pastResponse[2][0]
			val correctAnswer = pastResponse[3][0]
			val response = PastResponse(question, choices, userAnswer, correctAnswer)

			responseCollection.add(response)
		}
	}

	/**
	 * calculates the amount of xp needed to get to their current level.
	 * @return xp needed to get to their current level.
	 */
	private fun calculateXpNeededForCurrent(): Int {
		return ((currentLevel * kotlin.math.log2(currentLevel.toDouble())) * (5.0 / 2.0)).toInt()
	}

	/**
	 * calculates the progress of the user's level in percentage.
	 * @return the progress of the user's level in percentage.
	 */
	private fun calculateLevelProgress(): Int {
		val range = xpNeeded - xpNeededForCurrent
		val progress = (currentXp + score) - xpNeededForCurrent
		var progressPercent = (progress.toDouble() / range.toDouble() * 100.0).toInt()
		if (progressPercent >= 100) {
			// The user has reached another level and their progress is over 100.
			while (progressPercent >= 100) {
				// Remove 100 from the progress until it is less than or equal to 100.
				progressPercent -= 100
			}
		}
		return progressPercent
	}

	/**
	 * updates the number of correct answers and the amount of questions the user has answered
	 * in the database. Uses Firebase Authentication and Firebase Firestore.
	 * The libraries can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 */
	private fun updateNumberOfCorrectAnswersAndQuestions() {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).get()
			.addOnSuccessListener { document ->
				if (document != null) {
					// User exists in the database. Update the correct answers and no of questions.
					val numCorrect = document.getLong("numberOfCorrectAnswers")!!.toInt()
					val numQuestions = document.getLong("numberOfQuestions")!!.toInt()

					database.collection("users").document(userId).update(
						"numberOfCorrectAnswers",
						numCorrect + score
					)

					database.collection("users").document(userId).update(
						"numberOfQuestions",
						numQuestions + totalQuestions
					)
				}
			}
	}

	/**
	 * updates the number of correct answers and the amount of questions the user has answered
	 * the selected topic in the database. Also updates their percentage of correct answers.
	 * Uses Firebase Authentication and Firebase Firestore.
	 * The libraries can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 */
	private fun updateTopicStats() {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId)
			.collection("topics").document(chosenTopic).get()
			.addOnSuccessListener { document ->
				if (document != null) {
					val amountAnswered = document.getLong("amountAnswered")!!.toInt()
					val correctAnswers = document.getLong("correctAnswers")!!.toInt()
					val percentageCorrect = (((correctAnswers + score).toDouble() / (totalQuestions
							+ amountAnswered).toDouble()) * 100).toInt()

					database.collection("users").document(userId)
						.collection("topics").document(chosenTopic).update(
							"amountAnswered",
							amountAnswered + totalQuestions,
							"correctAnswers",
							correctAnswers + score,
							"percentageCorrect",
							percentageCorrect
						)
				}
			}
	}
}