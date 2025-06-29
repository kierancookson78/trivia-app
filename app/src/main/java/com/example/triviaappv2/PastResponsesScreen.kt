package com.example.triviaappv2

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * The PastResponsesScreen class is used to display the past responses of the user.
 * This class uses firebase firestore to get the past responses of the user and displays them and
 * firebase auth to get the current user. They can be found at:
 * https://firebase.google.com/docs/firestore,
 * https://firebase.google.com/docs/auth
 */
class PastResponsesScreen : AppCompatActivity() {
	private var auth = FirebaseAuth.getInstance()
	private var loggedInUser = auth.currentUser
	private val database = Firebase.firestore
	private lateinit var toolbar: Toolbar
	private var pastResponses: QuerySnapshot? = null
	private var pastResponseAmount = 0
	private var responseIndex = 0
	private lateinit var nextButton: Button
	private lateinit var prevButton: Button

	/**
	 * onCreate is called when the activity is first created.
	 * @param savedInstanceState is the activity's previously saved state.
	 */
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_past_responses_screen)
		toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)
		val container: ViewGroup = findViewById(R.id.past_response_holder)
		val view =
			LayoutInflater.from(this).inflate(
				R.layout.past_response_layout,
				container, false
			)
		val radioGroup: RadioGroup = view.findViewById(R.id.past_response_holder)
		nextButton = view.findViewById(R.id.next_button)
		prevButton = view.findViewById(R.id.prev_button)

		toolbar.setNavigationOnClickListener {
			back()
		}

		getPastResponses { querySnapshot ->
			pastResponses = querySnapshot
			pastResponseAmount = pastResponses!!.size()
			if (pastResponseAmount != 0) {
				// If there are past responses, display the first response.
				displayResponse(responseIndex, container, view)
			}

			nextButton.isEnabled = responseIndex in 0..<pastResponseAmount - 1
			prevButton.isEnabled = responseIndex > 0
			enableAndDisableButtons()

			nextButton.setOnClickListener {
				container.removeView(view)
				radioGroup.removeAllViews()
				responseIndex++
				displayResponse(responseIndex, container, view)
				nextButton.isEnabled = responseIndex in 0..<pastResponseAmount - 1
				prevButton.isEnabled = responseIndex > 0
				enableAndDisableButtons()
			}

			prevButton.setOnClickListener {
				container.removeView(view)
				radioGroup.removeAllViews()
				responseIndex--
				displayResponse(responseIndex, container, view)
				nextButton.isEnabled = responseIndex in 0..<pastResponseAmount - 1
				prevButton.isEnabled = responseIndex > 0
				enableAndDisableButtons()
			}
		}
	}

	/**
	 * back is called when the user clicks the back button.
	 * It takes the user back to the home page.
	 */
	private fun back() {
		val intent = Intent(this, HomePage::class.java)
		startActivity(intent)
		finish()
	}

	/**
	 * getPastResponses is called to get the past responses of the user.
	 * Uses firebase firestore and firebase auth which can be found at:
	 * https://firebase.google.com/docs/firestore,
	 * https://firebase.google.com/docs/auth
	 *
	 * @param callback is the results from the query that gets past responses sorted by time and
	 * date.
	 */
	private fun getPastResponses(callback: (QuerySnapshot) -> Unit) {
		val userId = loggedInUser!!.uid
		val pastResponsesRef = database.collection("users")
			.document(userId).collection("responses")
			.orderBy("createdAt", Query.Direction.DESCENDING)

		pastResponsesRef.get().addOnSuccessListener { querySnapshot ->
			callback(querySnapshot)
		}.addOnFailureListener {
			val snack = Snackbar.make(
				toolbar, "Error getting past responses",
				Snackbar.LENGTH_LONG
			)
			snack.show()
		}
	}

	/**
	 * enableAndDisableButtons is used to enable and disable the next and previous buttons.
	 */
	private fun enableAndDisableButtons() {
		if (!prevButton.isEnabled) {
			// If the previous button is disabled, hide it.
			prevButton.visibility = View.GONE
		} else {
			// If the previous button is enabled, show it.
			prevButton.visibility = View.VISIBLE
		}

		if (!nextButton.isEnabled) {
			// If the next button is disabled, hide it.
			nextButton.visibility = View.GONE
		} else {
			// If the next button is enabled, show it.
			nextButton.visibility = View.VISIBLE
		}
	}

	/**
	 * displayResponse is used to display the past response using a radio button for each choice
	 * and selecting the selected answer to show which response the user selected.
	 *
	 * @param index is the index of the past response to display.
	 * @param container is the container to display the past response in.
	 * @param view is the view to create the radio buttons for the past response.
	 */
	private fun displayResponse(index: Int, container: ViewGroup, view: View) {
		val response = pastResponses?.documents?.get(index)
		val dateAnswered = response?.get("createdAt") as Timestamp
		val question = response.getString("question")
		val choices = response.get("choices") as ArrayList<*>
		val selectedAnswer = response.getString("selectedAnswer")
		val correctAnswer = response.getString("correctAnswer")
		val radioGroup: RadioGroup = view.findViewById(R.id.past_response_holder)
		val radioButtons = mutableListOf<RadioButton>()

		val dateAnsweredText = view.findViewById<TextView>(R.id.time_answered)
		val questionText = view.findViewById<TextView>(R.id.question)
		val correctAnswerText = view.findViewById<TextView>(R.id.correct_answer)

		val date = dateAnswered.toDate()
		val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
		val dateAnsweredString = dateFormat.format(date)

		dateAnsweredText.text = getString(R.string.date_answered, dateAnsweredString)
		questionText.text = question
		correctAnswerText.text = getString(R.string.correct_answer, correctAnswer)

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
					Html.fromHtml(choices[choice].toString(), Html.FROM_HTML_MODE_LEGACY).toString()
				radioButtons.add(choiceButton)
				radioGroup.addView(choiceButton)
			}
		}

		for (i in radioButtons.indices) {
			// Loop through the radio buttons and set the selected answer to be checked.
			if (radioButtons[i].text == selectedAnswer) {
				// Radio button text matches the selected answer, set it to be checked.
				radioButtons[i].isChecked = true
			} else {
				// Radio buttons that do not match the selected answer are disabled.
				radioButtons[i].isChecked = false
				radioButtons[i].isEnabled = false
			}
		}
		radioGroup.isEnabled = false
		container.addView(view)
	}
}