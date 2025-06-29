package com.example.triviaappv2

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

/**
 * The DailyQuizSettings class is an activity that allows the user to set the time for their daily
 * quiz and then a notification will be scheduled to be sent at that time.
 */
class DailyQuizSettings : AppCompatActivity() {
	private var hour = 0
	private var minute = 0

	/**
	 * onCreate is called when the activity is first created. It sets the layout for the activity.
	 * @param savedInstanceState The saved state of the activity.
	 */
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_daily_quiz_settings)
		registerReceiver(NotificationReceiver(), IntentFilter(Intent.ACTION_BOOT_COMPLETED))
		val selectTimeButton = findViewById<Button>(R.id.selectTimeButton)
		val toolbar = findViewById<Toolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)
		toolbar.setNavigationOnClickListener { _ -> back() }

		selectTimeButton.setOnClickListener {
			var oneWeekFromNow = PreferencesHelper(this).getOneWeekFromNow()
			val currentTime = System.currentTimeMillis()
			if (currentTime < oneWeekFromNow) {
				// The user has already selected a time for the daily quiz within the past week.
				val snack = Snackbar.make(
					this, findViewById(R.id.main),
					"You can only change daily quiz time once a week", Snackbar.LENGTH_LONG
				)
				snack.show()
			} else {
				// A week has passed since the user selected a time for the daily quiz. So set time.
				oneWeekFromNow = Calendar.getInstance().apply {
					add(Calendar.DAY_OF_MONTH, 7)
				}.timeInMillis
				PreferencesHelper(this).setOneWeekFromNow(oneWeekFromNow)
				val dialogView = LayoutInflater.from(this).inflate(
					R.layout.time_selector,
					null
				)
				val timePicker = dialogView.findViewById<TimePicker>(R.id.time_spinner)
				timePicker.setIs24HourView(true)
				val timePickerDialog = androidx.appcompat.app.AlertDialog.Builder(
					this,
					R.style.time_selector_theme
				)
					.setView(dialogView)
					.setPositiveButton("Ok") { _, _ ->
						hour = timePicker.hour
						minute = timePicker.minute
						val selectedTimeHelper = PreferencesHelper(this)
						val selectedTime = getString(R.string.time_format, hour, minute)
						selectedTimeHelper.setSelectedTime(selectedTime)
						scheduleNotification(this, hour, minute)
					}
					.setNegativeButton("Cancel", null)
					.create()
				timePickerDialog.show()

				val positiveButton = timePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE)
				val negativeButton = timePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE)

				if (QuizScreen.getNightModeActive(this)) {
					// Set the text color of the buttons to white
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
					// Set the text color of the buttons to black
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
		}
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
	 * schedules a notification to be sent at the selected time if the notification permission
	 * is granted.
	 * @param context The context of the activity.
	 * @param hour The hour of the day to send the notification.
	 * @param minute The minute of the hour to send the notification.
	 */
	private fun scheduleNotification(context: Context, hour: Int, minute: Int) {
		val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
		val intent = Intent(context, NotificationReceiver::class.java)
		val pendingIntent = PendingIntent.getBroadcast(
			context.applicationContext, 0,
			intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
		)
		val calendar = Calendar.getInstance().apply {
			timeInMillis = System.currentTimeMillis()
			set(Calendar.HOUR_OF_DAY, hour)
			set(Calendar.MINUTE, minute)
			set(Calendar.SECOND, 0)
		}

		if (ContextCompat.checkSelfPermission(
				context,
				android.Manifest.permission.POST_NOTIFICATIONS
			)
			!= PackageManager.PERMISSION_GRANTED
		) {

			// Permission is not granted, request it.
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				ActivityCompat.requestPermissions(
					context as Activity, arrayOf(
						android.Manifest.permission.POST_NOTIFICATIONS
					),
					NOTIFICATION_PERMISSION_REQUEST_CODE
				)
			}
			return
		}

		if (calendar.before(Calendar.getInstance())) {
			// If the time is already past, set the alarm for the next day.
			calendar.add(Calendar.DAY_OF_MONTH, 1)
		}

		// Set the alarm to send the notification to the receiver.
		alarm.setExactAndAllowWhileIdle(
			AlarmManager.RTC_WAKEUP,
			calendar.timeInMillis,
			pendingIntent
		)
	}

	/**
	 * Processes the result of the notification permission request.
	 * @param requestCode The request code of the permission request.
	 * @param permissions The permissions requested.
	 * @param grantResults The results of the permission request.
	 */
	override fun onRequestPermissionsResult(
		requestCode: Int,
		permissions: Array<out String>,
		grantResults: IntArray,
	) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
			if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// Permission is granted, schedule the notification.
				scheduleNotification(this, hour, minute)
			} else {
				// Permission is not granted. Display message.
				val snack = Snackbar.make(
					this, findViewById(R.id.main),
					"Notification permission denied", Snackbar.LENGTH_LONG
				)
				snack.show()
			}
		}
	}

	companion object {
		private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1
	}
}