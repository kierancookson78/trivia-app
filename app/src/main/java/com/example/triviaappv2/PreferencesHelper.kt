package com.example.triviaappv2

import android.content.Context

/**
 * The PreferencesHelper class is used to save and retrieve data from the shared preferences.
 */
class PreferencesHelper(context: Context) {
	private val sharedPreference =
		context.getSharedPreferences("local_storage", Context.MODE_PRIVATE)

	/**
	 * Gets the user selected time from the shared preferences.
	 * @return The user selected time.
	 */
	fun getSelectedTime(): String? {
		return sharedPreference.getString("selected_time", null)
	}

	/**
	 * Sets the user selected time in the shared preferences.
	 * @param time The user selected time.
	 */
	fun setSelectedTime(time: String) {
		val editor = sharedPreference.edit()
		editor.putString("selected_time", time)
		editor.apply()
	}

	/**
	 * Stores the daily quiz status in the shared preferences.
	 */
	fun setDailyQuizStatus(status: Boolean) {
		val editor = sharedPreference.edit()
		editor.putBoolean("daily_quiz_status", status)
		editor.apply()
	}

	/**
	 * Gets the daily quiz status from the shared preferences.
	 * @return The daily quiz status.
	 */
	fun getDailyQuizStatus(): Boolean {
		return sharedPreference.getBoolean("daily_quiz_status", false)
	}

	/**
	 * Gets the timer start day from the shared preferences.
	 * @return The timer start day.
	 */
	fun getTimerStartDay(): Int {
		return sharedPreference.getInt("start_day", -1)
	}

	/**
	 * Sets the timer start day in the shared preferences.
	 * @param timeStartDay The timer start day.
	 */
	fun setTimerStartDay(timeStartDay: Int) {
		val editor = sharedPreference.edit()
		editor.putInt("start_day", timeStartDay)
		editor.apply()
	}

	/**
	 * Resets the timer start day in the shared preferences.
	 */
	fun resetTimerStartDay() {
		setTimerStartDay(-1)
	}

	/**
	 * Sets the one week from now in the shared preferences.
	 * @param time The one week from now.
	 */
	fun setOneWeekFromNow(time: Long) {
		val editor = sharedPreference.edit()
		editor.putLong("one_week_from_now", time)
		editor.apply()

	}

	/**
	 * Gets the one week from now from the shared preferences.
	 * @return The one week from now.
	 */
	fun getOneWeekFromNow(): Long {
		return sharedPreference.getLong("one_week_from_now", -1)
	}
}