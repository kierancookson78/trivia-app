package com.example.triviaappv2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.koushikdutta.ion.Ion
import org.json.JSONObject

/**
 * The RankedQuiz class is an activity that allows the user to start a ranked quiz.
 * This class uses ion and the Open Trivia API which can be found at:
 * https://github.com/koush/ion,
 * https://opentdb.com/api_config.php
 */
class RankedQuiz : AppCompatActivity() {
	private val currentTopic = "Ranked"
	private lateinit var questionSelection: String
	private lateinit var quizQuestions: String

	/**
	 * onCreate is called when the activity is first created. It sets the layout for the activity.
	 * @param savedInstanceState The saved state of the activity.
	 */
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_ranked_quiz)

		val startButton = findViewById<Button>(R.id.start)
		val toolbar = findViewById<Toolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)

		toolbar.setNavigationOnClickListener { _ -> back() }
		startButton.setOnClickListener { _ -> start() }
	}

	/**
	 * Goes back to the home page when the back button is pressed.
	 */
	private fun back() {
		val backIntent = Intent(this, HomePage::class.java)
		startActivity(backIntent)
		finish()
	}

	/**
	 * Starts the quiz when the start button is pressed which takes the user to the quiz screen.
	 */
	private fun start() {
		getQuizQuestions()
		val startIntent = Intent(this, QuizScreen::class.java)
		startIntent.putExtra("selected_topic", currentTopic)
		startIntent.putExtra("quiz_questions", quizQuestions)
		startIntent.putExtra("ranked_quiz", true)
		startActivity(startIntent)
		finish()
	}

	/**
	 * Processes the questions from the API and stores them in a string.
	 * @param result The result of the API call.
	 */
	private fun processQuestions(result: String) {
		val myJSON = JSONObject(result)
		val quizQuestionsString = myJSON.getJSONArray("results").toString()
		quizQuestions = quizQuestionsString
	}

	/**
	 * Gets the questions from the API. Then sends them for processing.
	 * The Open Trivia API used can be found here: https://opentdb.com/api_config.php
	 * Ion is used to get the results from the API which can be found here:
	 * https://github.com/koush/ion
	 */
	private fun getQuizQuestions() {
		questionSelection = "https://opentdb.com/api.php?amount=10"

		val questionData = Ion.with(this)
			.load(questionSelection)
			.asString()
			.get()
		processQuestions(questionData)
	}
}