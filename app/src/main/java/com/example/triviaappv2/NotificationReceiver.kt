package com.example.triviaappv2

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat


/**
 * The NotificationReceiver class is a broadcast receiver that handles the daily notification in the
 * background. It creates a notification channel, builds the notification, shows the notification
 * and schedules the next notification using an alarm manager.
 */
class NotificationReceiver : BroadcastReceiver() {
	@SuppressLint("UnsafeProtectedBroadcastReceiver")
	override fun onReceive(context: Context, intent: Intent) {
		val preferencesHelper = PreferencesHelper(context)
		preferencesHelper.setDailyQuizStatus(true)
		val homeIntent = Intent(context, HomePage::class.java)
		homeIntent.putExtra("clicked_notification", true)
		val pendingIntent = PendingIntent.getActivity(
			context, 0, homeIntent,
			PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
		)
		buildNotificationChannel(context)
		val notificationBuilder = buildNotification(context, pendingIntent)
		showNotification(context, notificationBuilder)
		scheduleNextNotification(context)
	}

	/**
	 * Builds a notification channel with the specified name, description and importance.
	 * @param context The context of the application.
	 */
	private fun buildNotificationChannel(context: Context) {
		val channelName = "Daily Reminder Channel"
		val channelDescription = "A Channel to send daily quiz reminders"
		val channelImportance = NotificationManager.IMPORTANCE_DEFAULT
		val notificationChannel = NotificationChannel(CHANNEL_ID, channelName, channelImportance)
			.apply {
				description = channelDescription
			}
		val notificationManager: NotificationManager = context.getSystemService(
			Context.NOTIFICATION_SERVICE
		) as NotificationManager
		notificationManager.createNotificationChannel(notificationChannel)
	}

	/**
	 * Builds the daily notification that will be used to remind the user to take their daily quiz.
	 * @param context The context of the application.
	 * @param pendingIntent The pending intent launched when a user presses the notification.
	 *
	 * @return A notification builder that can be used to build the notification.
	 */
	private fun buildNotification(context: Context, pendingIntent: PendingIntent):
			NotificationCompat.Builder {
		val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
			.setSmallIcon(R.drawable.baseline_notifications_24)
			.setContentTitle("Daily Quiz Reminder!")
			.setContentText("It's time for your daily quiz!")
			.setPriority(NotificationCompat.PRIORITY_MAX)
			.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
			.setDefaults(NotificationCompat.DEFAULT_ALL)
			.setCategory(NotificationCompat.CATEGORY_REMINDER)
			.setContentIntent(pendingIntent)
			.setAutoCancel(true)

		return notificationBuilder
	}

	/**
	 * Shows the notification only if the user grants permission.
	 * @param context The context of the application.
	 * @param notificationBuilder The notification builder that can be used to build the
	 * notification.
	 */
	private fun showNotification(
		context: Context, notificationBuilder:
		NotificationCompat.Builder
	) {
		with(NotificationManagerCompat.from(context)) {
			if (ActivityCompat.checkSelfPermission( // Check for notification permission granted.
					context,
					android.Manifest.permission.POST_NOTIFICATIONS
				) != PackageManager.PERMISSION_GRANTED
			) {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					// If not granted, request permission.
					ActivityCompat.requestPermissions(
						Activity(),
						arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 0
					)
				}
				return
			}
			notify(NOTIFICATION_ID, notificationBuilder.build())
		}
	}

	/**
	 * Schedules the notification for the next day at the same time.
	 * @param context The context of the application.
	 */
	private fun scheduleNextNotification(context: Context) {
		val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
		val nextAlarmIntent = Intent(context, NotificationReceiver::class.java)
		val nextPendingIntent = PendingIntent.getBroadcast(
			context, 0, nextAlarmIntent,
			PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
		)
		val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
		val minute = Calendar.getInstance().get(Calendar.MINUTE)
		val nextAlarmTime = Calendar.getInstance().apply {
			timeInMillis = System.currentTimeMillis()
			add(Calendar.DAY_OF_MONTH, 1)
			set(Calendar.HOUR_OF_DAY, hour)
			set(Calendar.MINUTE, minute)
			set(Calendar.SECOND, 0)
		}.timeInMillis

		alarmManager.setExactAndAllowWhileIdle(
			AlarmManager.RTC_WAKEUP,
			nextAlarmTime,
			nextPendingIntent
		)
	}

	companion object {
		private const val NOTIFICATION_ID = 1
		private const val CHANNEL_ID = "daily_channel"
	}
}