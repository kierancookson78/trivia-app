package com.example.triviaappv2

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.util.Calendar

/**
 * This is the fragment that contains the quiz mode
 * selection and timer until the daily quiz is ready.
 */
class QuizzesFragment : Fragment() {
	private var countDownTimer: CountDownTimer? = null
	private lateinit var view: View
	private lateinit var dailyQuizButton: Button
	private lateinit var timeText: TextView

	/**
	 * onCreateView is called when the fragment is first created to inflate it.
	 * @param inflater The layout inflater to use to inflate the fragment's layout.
	 * @param container The parent view that the fragment's UI should be attached to.
	 * @param savedInstanceState The saved state of the fragment.
	 * @return The root view of the fragment's layout.
	 */
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		view = inflater.inflate(R.layout.fragment_quizzes, container, false)
		dailyQuizButton = view.findViewById(R.id.daily_quiz_button)
		timeText = view.findViewById(R.id.time_text)
		val standardQuizButton = view.findViewById<Button>(R.id.normal_quiz_button)
		val rankedQuizButton = view.findViewById<Button>(R.id.ranked_quiz_button)
		val timeAttackQuizButton = view.findViewById<Button>(R.id.timed_quiz_button)
		val preferencesHelper = PreferencesHelper(requireContext())
		val savedTime = preferencesHelper.getSelectedTime()
		val dailyQuizStatus = preferencesHelper.getDailyQuizStatus()

		dailyQuizButton.setOnClickListener {
			val quizIntent = Intent(requireContext(), DailyQuizzes::class.java)
			startActivity(quizIntent)
		}

		standardQuizButton.setOnClickListener {
			val quizIntent = Intent(requireContext(), StandardQuiz::class.java)
			startActivity(quizIntent)
		}

		rankedQuizButton.setOnClickListener {
			val quizIntent = Intent(requireContext(), RankedQuiz::class.java)
			startActivity(quizIntent)
		}

		timeAttackQuizButton.setOnClickListener {
			val quizIntent = Intent(requireContext(), TimeAttack::class.java)
			startActivity(quizIntent)
		}

		if (savedTime != null && !dailyQuizStatus) {
			// The user has set a time for the daily quiz and it isn't ready so start timer.
			setCountDownTimer(savedTime, preferencesHelper)
		} else if (savedTime != null) {
			// The user has set a time for the daily quiz and it is ready so enable the button.
			dailyQuizButton.isEnabled = true
			timeText.text = getString(R.string.quiz_ready)
		}
		return view
	}

	/**
	 * Sets a countdown timer that displays the time remaining until the daily quiz is ready
	 * and enables the button when it is ready.
	 */
	private fun setCountDownTimer(selectedTime: String, preferencesHelper: PreferencesHelper) {
		val hour = selectedTime.substring(0, 2).toInt()
		val minute = selectedTime.substring(3, 5).toInt()
		val currentTime = Calendar.getInstance()
		val selectedTimeCalendar = Calendar.getInstance().apply {
			set(Calendar.HOUR_OF_DAY, hour)
			set(Calendar.MINUTE, minute)
			set(Calendar.SECOND, 0)

			if (currentTime.get(Calendar.DAY_OF_YEAR) > preferencesHelper.getTimerStartDay()
				&& before(currentTime) && preferencesHelper.getTimerStartDay() != -1
			) {
				// A day has passed since the timer started, so enable the button and set the status
				preferencesHelper.setDailyQuizStatus(true)
				dailyQuizButton.isEnabled = true
				timeText.text = getString(R.string.quiz_ready)
				return
			} else if (before(currentTime)) {
				// The selected time has already passed, so add one day to the current time.
				add(Calendar.DAY_OF_MONTH, 1)
			}
		}

		val calendar = Calendar.getInstance()
		val currentDay = calendar.get(Calendar.DAY_OF_YEAR)
		preferencesHelper.setTimerStartDay(currentDay)
		val countDownMillis = selectedTimeCalendar.timeInMillis - currentTime.timeInMillis

		countDownTimer = object : CountDownTimer(countDownMillis, 1000) {

			/**
			 * onTick is called every second to update the time remaining
			 * until the daily quiz is ready.
			 * @param countDownMillis The time remaining in milliseconds.
			 */
			override fun onTick(countDownMillis: Long) {
				val hoursRemaining = countDownMillis / (60 * 60 * 1000)
				val minutesRemaining = (countDownMillis % (60 * 60 * 1000)) / (60 * 1000)
				val secondsRemaining = (countDownMillis % (60 * 1000)) / 1000
				dailyQuizButton.isEnabled = false
				timeText.text = getString(
					R.string.time_until_quiz, hoursRemaining,
					minutesRemaining, secondsRemaining
				)
			}

			/**
			 * onFinish is called when the timer is finished and the daily quiz is ready.
			 */
			override fun onFinish() {
				preferencesHelper.setDailyQuizStatus(true)
				dailyQuizButton.isEnabled = true
				timeText.text = getString(R.string.quiz_ready)
			}
		}
		countDownTimer?.start()
	}

	/**
	 * onDestroyView is called when the view is destroyed and cancels the countdown timer
	 * to prevent trying to change the countdown timer after the view has been destroyed.
	 */
	override fun onDestroyView() {
		super.onDestroyView()
		countDownTimer?.cancel()
	}
}