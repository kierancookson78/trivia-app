package com.example.triviaappv2

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Html
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import org.json.JSONArray
import java.util.Calendar

/**
 * This activity is used to display the quiz questions and allow the user to answer them.
 * This class uses firebase firestore and firebase auth which can be found at:
 * https://firebase.google.com/docs/firestore,
 * https://firebase.google.com/docs/auth
 */
class QuizScreen : AppCompatActivity() {
	private lateinit var chosenTopic: String
	private lateinit var quizQuestions: String
	private var listOfQuestions = ArrayList<ArrayList<ArrayList<String>>>()
	private var questionNumber = 0
	private var correctAnswers = 0
	private var lastAnswerCorrect = false
	private var dailyQuiz = false
	private var timeAttack = false
	private var rankedQuiz = false
	private var pastResponsesVar = ArrayList<ArrayList<ArrayList<String>>>()
	private var alreadyAnswered = false
	private lateinit var nextButton: Button
	private lateinit var finishButton: Button
	private var countDownTimer: CountDownTimer? = null
	private val auth = FirebaseAuth.getInstance()
	private val loggedInUser = auth.currentUser
	private val database = Firebase.firestore
	private var currentStreak = 0
	private var longestStreak = 0

	/**
	 * onCreate is used to set up the activity and display the quiz questions.
	 * Uses firebase firestore and firebase auth which can be found at:
	 * https://firebase.google.com/docs/firestore,
	 * https://firebase.google.com/docs/auth
	 *
	 * @param savedInstanceState is the saved instance state of the activity.
	 */
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_quiz_screen)

		val toolbar = findViewById<Toolbar>(R.id.toolbar)
		val extras = intent.extras ?: return
		val inflater = LayoutInflater.from(this)
		val container: ViewGroup = findViewById(R.id.question_holder)
		val view = inflater.inflate(R.layout.multiple_choice_layout, container, false)
		val radioGroup: RadioGroup = view.findViewById(R.id.question_choice_holder)
		val quizProgressBar = findViewById<LinearProgressIndicator>(R.id.quiz_progress_bar)
		nextButton = view.findViewById(R.id.submit_button)
		finishButton = view.findViewById(R.id.finish_button)
		chosenTopic = extras.getString("selected_topic").toString()
		quizQuestions = extras.getString("quiz_questions").toString()
		dailyQuiz = extras.getBoolean("daily_quiz") == true
		timeAttack = extras.getBoolean("time_attack") == true
		rankedQuiz = extras.getBoolean("ranked_quiz") == true

		toolbar.title = chosenTopic
		setSupportActionBar(toolbar)

		toolbar.setNavigationOnClickListener { _ ->
			val quitAlert = AlertDialog.Builder(this, R.style.time_selector_theme)
			quitAlert.setMessage("Quitting Will lose All Progress!")
			quitAlert.setPositiveButton("Ok") { _, _ -> quit() }
			quitAlert.setNegativeButton("Cancel") { alert, _ -> alert.dismiss() }
			val alertBox = quitAlert.create()
			alertBox.show()

			val positiveButton = alertBox.getButton(DialogInterface.BUTTON_POSITIVE)
			val negativeButton = alertBox.getButton(DialogInterface.BUTTON_NEGATIVE)

			if (getNightModeActive(this)) {
				// Set the text color of the buttons to white when the app is in night mode.
				positiveButton.setTextColor(
					ContextCompat.getColor(
						this,
						R.color.text_and_icons
					)
				)
				negativeButton.setTextColor(
					ContextCompat.getColor(
						this,
						R.color.text_and_icons
					)
				)
			} else {
				// Set the text color of the buttons to black when the app is in day mode.
				positiveButton.setTextColor(
					ContextCompat.getColor(
						this,
						R.color.primary_text
					)
				)
				negativeButton.setTextColor(
					ContextCompat.getColor(
						this,
						R.color.primary_text
					)
				)
			}
		}

		processQuestions()
		displayQuestion(questionNumber, container, view)

		processAnswers(radioGroup)


		nextButton.isEnabled = listOfQuestions.size > 1
		finishButton.isEnabled = false
		finishButton.visibility = View.GONE

		enableAndDisableButtons()

		getUserAnswerStreak { answerStreak, bestStreak ->
			currentStreak = answerStreak
			longestStreak = bestStreak

			nextButton.setOnClickListener { _ ->
				container.removeView(view)
				radioGroup.removeAllViews()
				questionNumber++
				val percentComplete = ((questionNumber + 1).toDouble() / listOfQuestions.size) * 100
				quizProgressBar.setProgressCompat(percentComplete.toInt(), true)
				displayQuestion(questionNumber, container, view)
				nextButton.isEnabled = questionNumber in 0..<listOfQuestions.size - 1
				enableAndDisableButtons()
				if (!lastAnswerCorrect) {
					// User answered incorrectly, reset their streak.
					val userId = loggedInUser!!.uid
					if (currentStreak > longestStreak) {
						// User's current streak is longer than their longest streak, update it.
						database.collection("users").document(userId)
							.update("longestStreak", currentStreak)
					}
					currentStreak = 0
				} else {
					// User answered correctly, increment their streak and correct answers.
					correctAnswers++
					currentStreak++
				}
				alreadyAnswered = false
			}

			finishButton.setOnClickListener { _ ->
				val userId = loggedInUser!!.uid
				if (!lastAnswerCorrect) {
					// User answered incorrectly, reset their streak.
					if (currentStreak > longestStreak) {
						// User's current streak is longer than their longest streak, update it.
						database.collection("users").document(userId)
							.update("longestStreak", currentStreak)
					}
					currentStreak = 0
				} else {
					// User answered correctly, increment their streak and correct answers.
					correctAnswers++
					currentStreak++
				}

				database.collection("users").document(userId)
					.update("currentStreak", currentStreak)
				finishButton()
			}
		}

		if (timeAttack) {
			// Time attack quiz chosen, set a countdown timer.
			setCountdownTimer()
			countDownTimer?.start()
		}
	}

	/**
	 * quit is used to quit the quiz and return to the home page.
	 */
	private fun quit() {
		val backIntent = Intent(this, HomePage::class.java)
		startActivity(backIntent)
		finish()
	}

	/**
	 * enableAndDisableButtons is used to enable or disable the buttons on the screen.
	 */
	private fun enableAndDisableButtons() {
		if (!nextButton.isEnabled) {
			// If the next button is disabled, hide it and enable the finish button.
			nextButton.visibility = View.GONE
			finishButton.isEnabled = true
			finishButton.visibility = View.VISIBLE
		} else {
			// If the next button is enabled, show it and disable the finish button.
			nextButton.visibility = View.VISIBLE
			finishButton.isEnabled = false
			finishButton.visibility = View.GONE
		}
	}

	/**
	 * processQuestions is used to process the questions from a JSON array ready to be displayed.
	 */
	private fun processQuestions() {
		val questions = JSONArray(quizQuestions)

		for (i in 0..<questions.length()) {
			// Loop through the questions in the JSON array and process them all for display.
			val questionObject = questions.getJSONObject(i)
			val choices = ArrayList<String>()
			val questionArray = ArrayList<ArrayList<String>>()
			val question = questionObject.getString("question")
			val correctAnswer = questionObject.getString("correct_answer")
			val incorrectAnswers = questionObject.getString("incorrect_answers")
			val incorrectAnswersArray: Array<String> =
				incorrectAnswers.substring(1, incorrectAnswers.length - 1).split("\"")
					.toTypedArray()
			val incorrectAnswersArrayList = ArrayList<String>()
			val correctArrayList = ArrayList<String>()
			val questionArrayList = ArrayList<String>()
			choices.add(correctAnswer)
			correctArrayList.add(correctAnswer)

			for (incorrectAnswer in incorrectAnswersArray.indices) {
				// Loop through the incorrect answers and add them to the choices array.
				incorrectAnswersArray[incorrectAnswer] =
					incorrectAnswersArray[incorrectAnswer].replace("\"", "")
				choices.add(incorrectAnswersArray[incorrectAnswer])
				incorrectAnswersArrayList.add(incorrectAnswersArray[incorrectAnswer])
			}

			choices.removeIf { it == "" || it == "," }
			choices.shuffle()

			questionArrayList.add(question)
			questionArray.add(questionArrayList)
			questionArray.add(choices)
			questionArray.add(correctArrayList)
			questionArray.add(incorrectAnswersArrayList)
			listOfQuestions.add(questionArray)
		}
	}

	/**
	 * displayQuestion is used to display a question and its choices using
	 * radio buttons in a radio group.
	 *
	 * @param questionNumber The index of the question to display.
	 * @param container The view group to add the question view to.
	 * @param view The view to display the question in.
	 */
	private fun displayQuestion(questionNumber: Int, container: ViewGroup, view: View) {
		val radioButtons = mutableListOf<RadioButton>()
		val questionArray = listOfQuestions[questionNumber]
		val choices = questionArray[1]
		var question = questionArray[0][0]
		question = Html.fromHtml(question, Html.FROM_HTML_MODE_LEGACY).toString()
		val radioGroup: RadioGroup = view.findViewById(R.id.question_choice_holder)
		val questionText = view.findViewById<TextView>(R.id.question)
		questionText.text = question

		if (choices.size == 2) {
			// If the question has only two choices, display true and false buttons.
			val trueButton = RadioButton(this)
			trueButton.id = View.generateViewId()
			trueButton.text = getString(R.string.true_text)
			radioButtons.add(trueButton)
			radioGroup.addView(trueButton)

			val falseButton = RadioButton(this)
			falseButton.id = View.generateViewId()
			falseButton.text = getString(R.string.false_text)
			radioButtons.add(falseButton)
			radioGroup.addView(falseButton)
		} else {
			// If the question has more than two choices, display radio buttons for each choice.
			for (choice in choices.indices) {
				// Loop through the choices and add them to the radio group.
				val choiceButton = RadioButton(this)
				choiceButton.id = View.generateViewId()
				choiceButton.text =
					Html.fromHtml(choices[choice], Html.FROM_HTML_MODE_LEGACY).toString()
				radioButtons.add(choiceButton)
				radioGroup.addView(choiceButton)
			}
		}
		container.addView(view)
	}

	/**
	 * processAnswers is used to process the user's answers and check if they are correct.
	 *
	 * @param radioGroup The radio group to check for checked buttons.
	 */
	private fun processAnswers(radioGroup: RadioGroup) {
		radioGroup.setOnCheckedChangeListener { _, checkedButtonId ->
			val checkedButton = radioGroup.findViewById<RadioButton>(checkedButtonId)
			val response = ArrayList<ArrayList<String>>()

			if (checkedButton != null) {
				// If a button is checked, check if it is the correct answer.
				val answerSelected = checkedButton.text.toString()
				var correctAnswer = listOfQuestions[questionNumber][2][0]
				correctAnswer = Html.fromHtml(correctAnswer, Html.FROM_HTML_MODE_LEGACY).toString()

				lastAnswerCorrect = if (answerSelected == correctAnswer) {
					// If the answer is correct, set the lastAnswerCorrect to true.
					true
				} else {
					// If the answer is incorrect, set the lastAnswerCorrect to false.
					false
				}

				val question = listOfQuestions[questionNumber][0]
				val choices = listOfQuestions[questionNumber][1]
				val userAnswer = ArrayList<String>()
				userAnswer.add(answerSelected)
				for (choice in choices.indices) {
					// Loop through the choices and convert them from HTML entities to String.
					choices[choice] = Html.fromHtml(
						choices[choice],
						Html.FROM_HTML_MODE_LEGACY
					).toString()
				}
				question[0] = Html.fromHtml(question[0], Html.FROM_HTML_MODE_LEGACY).toString()

				if (response.size > 1) {
					// User has selected multiple answers, clear the response.
					response.clear()
				}

				response.add(question)
				response.add(choices)
				response.add(userAnswer)
				response.add(arrayListOf(correctAnswer))
				if (alreadyAnswered) {
					// User has already answered so remove the old response and add the new one.
					pastResponsesVar[pastResponsesVar.lastIndex] = response
				} else {
					// User has not answered so add the new response.
					pastResponsesVar.add(response)
				}
				alreadyAnswered = true
			}
		}
	}

	/**
	 * finishButton is used to finish the quiz and go to the finished screen which displays
	 * the user's score.
	 */
	private fun finishButton() {
		if (dailyQuiz) {    // If the quiz is a daily quiz, reset the timer start day.
			val preferencesHelper = PreferencesHelper(this)
			preferencesHelper.setDailyQuizStatus(false)
			preferencesHelper.resetTimerStartDay()
		}

		countDownTimer?.cancel()
		val finishIntent = Intent(this, Finished::class.java)
		finishIntent.putExtra("score", correctAnswers)
		finishIntent.putExtra("total_questions", listOfQuestions.size)
		finishIntent.putExtra("ranked_quiz", rankedQuiz)
		finishIntent.putExtra("topic", chosenTopic)
		val gson = Gson()
		val pastResponsesExtra = gson.toJson(pastResponsesVar)
		updateStreak()
		finishIntent.putExtra("responses", pastResponsesExtra)
		startActivity(finishIntent)
		finish()
	}

	/**
	 * outOfTime is used to go to the finished screen if the user runs out of time when they are
	 * playing a timed quiz. Uses firebase firestore and firebase auth which can be found at:
	 * https://firebase.google.com/docs/firestore,
	 * https://firebase.google.com/docs/auth
	 */
	private fun outOfTime() {
		val finishIntent = Intent(this, Finished::class.java)
		finishIntent.putExtra("score", correctAnswers)
		finishIntent.putExtra("total_questions", listOfQuestions.size)
		finishIntent.putExtra("out_of_time", true)
		val gson = Gson()
		val pastResponsesExtra = gson.toJson(pastResponsesVar)
		finishIntent.putExtra("responses", pastResponsesExtra)
		val userId = loggedInUser!!.uid
		if (!lastAnswerCorrect) {
			// User answered incorrectly, reset their streak.
			if (currentStreak > longestStreak) {
				// User's current streak is longer than their longest streak, update it.
				database.collection("users").document(userId)
					.update("longestStreak", currentStreak)
			}
			currentStreak = 0
		} else {
			// User answered correctly, increment their streak and correct answers.
			correctAnswers++
			currentStreak++
		}
		database.collection("users").document(userId)
			.update("currentStreak", currentStreak)
		startActivity(finishIntent)
		finish()
	}

	/**
	 * setCountdownTimer is used to set a countdown timer for the timed quiz and display the
	 * timer and it goes red with 10 seconds remaining.
	 */
	private fun setCountdownTimer() {
		val quizTime: Long = (10000 * listOfQuestions.size).toLong()
		val timer = findViewById<TextView>(R.id.time_remaining_text)
		countDownTimer = object : CountDownTimer(quizTime, 1000) {

			/**
			 * onTick is called every second to update the time remaining until the quiz is over.
			 * @param countDownMillis The time remaining in milliseconds.
			 */
			override fun onTick(countDownMillis: Long) {
				val minutesRemaining = (countDownMillis % (60 * 60 * 1000)) / (60 * 1000)
				val secondsRemaining = (countDownMillis % (60 * 1000)) / 1000

				if (secondsRemaining <= 10 && minutesRemaining.toInt() == 0) {
					// If there are 10 or less seconds remaining, set the timer text to red.
					timer.setTextColor(getColor(R.color.red))
				}

				timer.text = getString(R.string.time_remaining, minutesRemaining, secondsRemaining)
			}

			/**
			 * onFinish is called when the time is over and displays the finished screen but with
			 * a message that the user ran out of time.
			 */
			override fun onFinish() {
				outOfTime()
			}
		}
	}

	/**
	 * Updates the users daily streak for quiz completion. Uses firebase firestore and firebase auth
	 * which can be found at:
	 * https://firebase.google.com/docs/firestore,
	 * https://firebase.google.com/docs/auth
	 */
	private fun updateStreak() {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).get()
			.addOnSuccessListener { document ->
				val lastDateAnswered = document.getLong("lastDateAnswered")!!
				var streak = document.getLong("streak")!!
				val date = Calendar.getInstance().timeInMillis

				if (DateUtils.isToday(lastDateAnswered + DateUtils.DAY_IN_MILLIS)) {
					// User completed their quiz yesterday and today, increment their streak.
					streak++
				}

				database.collection("users").document(userId)
					.update(
						"streak", streak,
						"lastDateAnswered", date
					)
			}
	}

	/**
	 * Gets the user's answer streak and longest streak from the database. Uses firebase firestore
	 * and firebase auth which can be found at:
	 * https://firebase.google.com/docs/firestore,
	 * https://firebase.google.com/docs/auth
	 */
	private fun getUserAnswerStreak(callback: (Int, Int) -> Unit) {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).get()
			.addOnSuccessListener { document ->
				val userAnswerStreak = document.getLong("currentStreak")!!.toInt()
				val longestStreak = document.getLong("longestStreak")!!.toInt()
				callback(userAnswerStreak, longestStreak)
			}
	}

	/**
	 * onDestroy is called when the activity is destroyed and cancels the countdown timer
	 * to prevent trying to change the countdown timer after the activity has been destroyed.
	 */
	override fun onDestroy() {
		super.onDestroy()
		countDownTimer?.cancel()
	}

	companion object {
		/**
		 * getNightModeActive is used to check if the app is in night mode.
		 *
		 * @param context The context to use to check the night mode.
		 * @return true if the app is in night mode, false otherwise.
		 */
		fun getNightModeActive(context: Context): Boolean {
			val currentMode =
				context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
			return currentMode == Configuration.UI_MODE_NIGHT_YES
		}
	}
}