package com.example.triviaappv2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.slider.Slider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.koushikdutta.ion.Ion
import org.json.JSONObject
import kotlin.properties.Delegates

/**
 * The DailyQuizzes class is an activity that allows the user to start a daily quiz and choose
 * question topic, type and amount. The questions are provided by the Open Trivia API.
 * This can be found at: https://opentdb.com/api_config.php The data from the api is loaded
 * using the ion library. The ion library can be found at: https://github.com/koush/ion
 * This class also uses Firebase auth and Firestore which can be found at:
 * https://firebase.google.com/docs/auth,
 * https://firebase.google.com/docs/firestore
 */
class DailyQuizzes : AppCompatActivity() {
	private var currentTopic: String = "Sports"
	private var topicNumber: Int = 21
	private var questionAmount by Delegates.notNull<Int>()
	private var typeString: String = "Any"
	private lateinit var questionSelection: String
	private lateinit var quizQuestions: String
	private var auth = FirebaseAuth.getInstance()
	private var loggedInUser = auth.currentUser
	private val database = Firebase.firestore
	private var topicPosition by Delegates.notNull<Int>()
	private var questionTypePosition by Delegates.notNull<Int>()

	/**
	 * onCreate is called when the activity is first created. It sets the layout for the activity.
	 * @param savedInstanceState The saved state of the activity.
	 */
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_daily_quizzes)

		val startButton = findViewById<Button>(R.id.start)
		val sliderSelection = findViewById<Slider>(R.id.question_amount)
		getNumberOfQuestions { numberOfQuestionsSelected ->
			sliderSelection.value = numberOfQuestionsSelected.toFloat()
		}
		var topicDropdown: Spinner?
		getTopicSelected { selectedTopic ->
			topicPosition = selectedTopic
			topicDropdown = topicDropDown()

			topicDropdown!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
				override fun onItemSelected(
					parent: AdapterView<*>?,
					view: View?,
					position: Int,
					id: Long
				) {
					when (position) {
						0 -> {
							currentTopic = parent!!.getItemAtPosition(position) as String
							setTopicSelected(0)
						}

						1 -> {
							currentTopic = parent!!.getItemAtPosition(position) as String
							setTopicSelected(1)
						}

						2 -> {
							currentTopic = parent!!.getItemAtPosition(position) as String
							setTopicSelected(2)
						}

						3 -> {
							currentTopic = parent!!.getItemAtPosition(position) as String
							setTopicSelected(3)
						}

						4 -> {
							currentTopic = parent!!.getItemAtPosition(position) as String
							setTopicSelected(4)
						}

						5 -> {
							currentTopic = parent!!.getItemAtPosition(position) as String
							setTopicSelected(5)
						}

						6 -> {
							currentTopic = parent!!.getItemAtPosition(position) as String
							setTopicSelected(6)
						}
					}
				}

				override fun onNothingSelected(parent: AdapterView<*>?) {
					return
				}
			}
		}
		var questionTypeDropdown: Spinner?
		getQuestionTypeSelected { selectedQuestionType ->
			questionTypePosition = selectedQuestionType
			questionTypeDropdown = questionTypeDropDown()

			questionTypeDropdown!!.onItemSelectedListener =
				object : AdapterView.OnItemSelectedListener {
					override fun onItemSelected(
						parent: AdapterView<*>?,
						view: View?,
						position: Int,
						id: Long
					) {
						when (position) {
							0 -> {
								typeString = parent!!.getItemAtPosition(position) as String
								setQuestionTypeSelected(0)
							}

							1 -> {
								typeString = parent!!.getItemAtPosition(position) as String
								setQuestionTypeSelected(1)
							}

							2 -> {
								typeString = parent!!.getItemAtPosition(position) as String
								setQuestionTypeSelected(2)
							}
						}
					}

					override fun onNothingSelected(parent: AdapterView<*>?) {
						return
					}
				}
		}
		val toolbar = findViewById<Toolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)

		toolbar.setNavigationOnClickListener { _ -> back() }
		startButton.setOnClickListener { _ -> start() }
	}

	/**
	 * Creates a spinner for the topic dropdown with all the topics in the array.
	 * @return The spinner for the topic dropdown.
	 */
	private fun topicDropDown(): Spinner {
		val dropdown = findViewById<Spinner>(R.id.topic_dropdown)
		val topics = resources.getStringArray(R.array.topics)
		val arrayAdapter = ArrayAdapter(
			this, android.R.layout.simple_spinner_dropdown_item,
			topics
		)
		dropdown.adapter = arrayAdapter

		topicPosition.let { selectedTopic ->
			dropdown.setSelection(selectedTopic)
		}

		return dropdown
	}

	/**
	 * Creates a spinner for the question type dropdown with all the types in the array.
	 * @return The spinner for the question type dropdown.
	 */
	private fun questionTypeDropDown(): Spinner {
		val dropdown = findViewById<Spinner>(R.id.question_type_dropdown)
		val questionTypes = resources.getStringArray(R.array.question_types)
		val arrayAdapter = ArrayAdapter(
			this, android.R.layout.simple_spinner_dropdown_item,
			questionTypes
		)
		dropdown.adapter = arrayAdapter

		questionTypePosition.let { selectedQuestionType ->
			dropdown.setSelection(selectedQuestionType)
		}

		return dropdown
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
		startIntent.putExtra("daily_quiz", true)
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
	 * Gets the questions from the API that the user has selected based on the topic, type and
	 * amount. Then sends them for processing.
	 * The Open Trivia API used can be found here: https://opentdb.com/api_config.php
	 * The data from the api is loaded using the ion library. The ion library can
	 * be found at: https://github.com/koush/ion
	 */
	private fun getQuizQuestions() {
		val sliderSelection = findViewById<Slider>(R.id.question_amount)
		questionAmount = sliderSelection.value.toInt()
		setNumberOfQuestions(questionAmount)

		when (currentTopic) {
			"Sport" -> topicNumber = 21
			"Science and Nature" -> topicNumber = 17
			"History" -> topicNumber = 23
			"Geography" -> topicNumber = 22
			"Politics" -> topicNumber = 24
			"General Knowledge" -> topicNumber = 9
			"Films" -> topicNumber = 11
		}

		when (typeString) {
			"Multiple Choice" -> typeString = "multiple"
			"True/False" -> typeString = "boolean"
		}

		questionSelection = if (typeString != "Any") {
			// Multiple or True/False question type has been selected.
			"https://opentdb.com/api.php?amount=$questionAmount&category=" +
					"$topicNumber&type=$typeString"
		} else {
			// Any question type has been selected.
			"https://opentdb.com/api.php?amount=$questionAmount&category=" +
					"$topicNumber"
		}

		val questionData = Ion.with(this)
			.load(questionSelection)
			.asString()
			.get()
		processQuestions(questionData)
	}

	/**
	 * Gets the topic that the user has selected from the database.
	 * Uses Firebase auth and Firestore which can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 *
	 * @param callback The callback function that is called when the topic is retrieved.
	 */
	private fun getTopicSelected(callback: (Int) -> Unit) {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).get()
			.addOnSuccessListener { document ->
				if (document != null) {
					val selectedTopic = document.getLong("selectedTopic")!!.toInt()
					callback(selectedTopic)
				}
			}
	}

	/**
	 * saves the user's choice of topic to the database.
	 * Uses Firebase auth and Firestore which can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 *
	 * @param selectedTopic The question type that the user has selected.
	 */
	private fun setTopicSelected(selectedTopic: Int) {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).update(
			"selectedTopic",
			selectedTopic
		)
	}

	/**
	 * Gets the question type that the user has selected from the database.
	 * Uses Firebase auth and Firestore which can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 *
	 * @param callback The callback function that is called when the question type is retrieved.
	 */
	private fun getQuestionTypeSelected(callback: (Int) -> Unit) {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).get()
			.addOnSuccessListener { document ->
				if (document != null) {
					val selectedQuestionType = document.getLong("selectedQuestionType")!!.toInt()

					callback(selectedQuestionType)
				}
			}
	}

	/**
	 * saves the user's choice of question type to the database.
	 * Uses Firebase auth and Firestore which can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 *
	 * @param selectedQuestionType The question type that the user has selected.
	 */
	private fun setQuestionTypeSelected(selectedQuestionType: Int) {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).update(
			"selectedQuestionType",
			selectedQuestionType
		)
	}

	/**
	 * saves the user's choice of number of questions to the database.
	 * Uses Firebase auth and Firestore which can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 *
	 * @param selectedNumberOfQuestions The number of questions that the user has selected.
	 */
	private fun setNumberOfQuestions(selectedNumberOfQuestions: Int) {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).update(
			"numberOfQuestionsSelected",
			selectedNumberOfQuestions
		)
	}

	/**
	 * Gets the number of questions that the user has selected from the database.
	 * Uses Firebase auth and Firestore which can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 *
	 * @param callback the callback function to return the number of questions selected.
	 */
	private fun getNumberOfQuestions(callback: (Int) -> Unit) {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).get()
			.addOnSuccessListener { document ->
				if (document != null) {
					val numberOfQuestionsSelected =
						document.getLong("numberOfQuestionsSelected")!!.toInt()

					callback(numberOfQuestionsSelected)
				}
			}
	}
}