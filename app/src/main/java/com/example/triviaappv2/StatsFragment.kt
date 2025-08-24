package com.example.triviaappv2

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * The StatsFragment class is a fragment that displays the user's stats.
 * This class uses Firebase Firestore and Firebase Auth which can be found at:
 * https://firebase.google.com/docs/firestore,
 * https://firebase.google.com/docs/auth
 */
class StatsFragment : Fragment() {
    private lateinit var view: View
    private lateinit var streakText: TextView
    private val auth = FirebaseAuth.getInstance()
    private val loggedInUser = auth.currentUser
    private val database = Firebase.firestore
    private lateinit var percentageCorrectTextView: TextView
    private lateinit var longestStreak: TextView
    private lateinit var bestTopic: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        view = inflater.inflate(R.layout.fragment_stats, container, false)!!
        streakText = view.findViewById(R.id.streak_text)
        percentageCorrectTextView = view.findViewById(R.id.correct_answer_percent)
        longestStreak = view.findViewById(R.id.longest_correct_streak)
        bestTopic = view.findViewById(R.id.best_topic)
        bestTopic.text = getString(R.string.best_topic, "None")
        getBestTopic { topic ->
            if (isAdded) {
                bestTopic.text = getString(R.string.best_topic, topic)
            }
        }
        var numCorrect: Int
        var numQuestions: Int
        updateStreak()
        displayUserBestCorrectStreak()

        getNumberOfCorrectAnswersAndQuestions { correct, questions ->
            if (isAdded) {
                numCorrect = correct
                numQuestions = questions
                var percentage = 0
                if (numQuestions != 0) {
                    // User has answered some questions. Calculate their percentage correct.
                    percentage = (numCorrect.toDouble() / numQuestions.toDouble() * 100.0).toInt()
                }
                percentageCorrectTextView.text =
                    getString(R.string.correct_answer_percent, percentage)
            }
        }
        return view
    }

    /**
     * displayUserBestCorrectStreak displays the user's best correct streak.
     * Use Firebase Firestore and Firebase Auth which can be found at:
     * https://firebase.google.com/docs/firestore,
     * https://firebase.google.com/docs/auth
     */
    private fun displayUserBestCorrectStreak() {
        val userId = loggedInUser!!.uid
        database.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (isAdded && document != null) {
                    // User exists in the database. Display their best correct streak.
                    val streak = document.getLong("longestStreak")!!
                    longestStreak.text = getString(R.string.longest_correct_streak, streak)
                }
            }
    }

    /**
     * Gets the number of correct answers and questions the user has answered.
     * Use Firebase Firestore and Firebase Auth which can be found at:
     * https://firebase.google.com/docs/firestore,
     * https://firebase.google.com/docs/auth
     *
     * @param callback returns the number of correct answers and questions the user has answered.
     */
    private fun getNumberOfCorrectAnswersAndQuestions(callback: (Int, Int) -> Unit) {
        val userId = loggedInUser!!.uid
        database.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // User exists in the database. callback correct answers and no of questions.
                    val numCorrect = document.getLong("numberOfCorrectAnswers")!!.toInt()
                    val numQuestions = document.getLong("numberOfQuestions")!!.toInt()
                    callback(numCorrect, numQuestions)
                }
            }
    }

    /**
     * Updates the user's daily completion streak.
     * Use Firebase Firestore and Firebase Auth which can be found at:
     * https://firebase.google.com/docs/firestore,
     * https://firebase.google.com/docs/auth
     */
    private fun updateStreak() {
        val userId = loggedInUser!!.uid
        database.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val lastDateAnswered = document.getLong("lastDateAnswered")!!
                var streak = document.getLong("streak")!!

                if (!DateUtils.isToday(lastDateAnswered + DateUtils.DAY_IN_MILLIS)
                    && !DateUtils.isToday(lastDateAnswered)
                ) {
                    // User has not answered in the last 2 days. Reset streak.
                    streak = 1
                }
                if (isAdded && document != null) {
                    streakText.text = getString(R.string.daily_streak, streak)
                }
                database.collection("users").document(userId)
                    .update("streak", streak)
            }
    }

    /**
     * gets the users current best performing topic.
     * Use Firebase Firestore and Firebase Auth which can be found at:
     * https://firebase.google.com/docs/firestore,
     * https://firebase.google.com/docs/auth
     *
     * @param callback returns the users current best performing topic.
     */
    private fun getBestTopic(callback: (String) -> Unit) {
        val userId = loggedInUser!!.uid
        val topics = context?.resources?.getStringArray(R.array.topics)

        if (topics == null || topics.isEmpty()) {
            callback("None") // No topics to check
            return
        }

        var bestPercentageFound = -1
        var finalBestTopic = "None"
        val topicsToProcess = topics.size
        var topicsProcessed = 0

        for (topic in topics) {
            database.collection("users").document(userId)
                .collection("topics").document(topic).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val percentageCorrect = document.getLong("percentageCorrect")?.toInt() ?: 0
                        if (percentageCorrect > bestPercentageFound) {
                            bestPercentageFound = percentageCorrect
                            finalBestTopic = topic
                        }
                    }
                }
                .addOnCompleteListener {
                    topicsProcessed++
                    if (topicsProcessed == topicsToProcess) {
                        // Now, call the callback with the best topic found so far.
                        if (isAdded) {
                            callback(finalBestTopic)
                        }
                    }
                }
        }
    }
}