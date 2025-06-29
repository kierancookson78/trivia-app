package com.example.triviaappv2

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.triviaappv2.adapter.AdapterTabs
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth

/**
 * This is the home page which the user is greeted to after logging in.
 * It contains the main functionality of the app that the user can access.
 * This class uses Firebase Authentication which can be found at:
 * https://firebase.google.com/docs/auth
 */
class HomePage : AppCompatActivity() {
	private var auth = FirebaseAuth.getInstance()
	private lateinit var toolbar: Toolbar

	/**
	 * onCreate is called when the activity is first created. It sets the layout for the activity.
	 * @param savedInstanceState The saved state of the activity.
	 */
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_home_page)
		toolbar = findViewById(R.id.toolbar)
		setSupportActionBar(toolbar)

		val tabs = findViewById<TabLayout>(R.id.tab_layout)
		val viewPager = findViewById<ViewPager2>(R.id.viewpager)
		val tabHeaders = resources.getStringArray(R.array.tab_headers)

		viewPager.adapter = AdapterTabs(this)
		TabLayoutMediator(tabs, viewPager) { tab, position ->
			when (position) {
				// Set the tab headers based on the tab position.
				0 -> tab.text = tabHeaders[0]
				1 -> tab.text = tabHeaders[1]
				2 -> tab.text = tabHeaders[2]
				3 -> tab.text = tabHeaders[3]
			}
		}.attach()

		val extras = intent.extras ?: return
		val notificationWasPressed = extras.getBoolean("clicked_notification")

		if (notificationWasPressed) {
			// The user clicked the daily quiz notification, so set the daily quiz status to true.
			val preferencesHelper = PreferencesHelper(this)
			preferencesHelper.setDailyQuizStatus(true)
		}
	}

	/**
	 * inflates the menu items in the toolbar.
	 * @param menu The menu to inflate.
	 * @return true if the menu is inflated, false otherwise.
	 */
	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		if (QuizScreen.getNightModeActive(this)) {
			// night mode is active, inflate the night mode menu items.
			menuInflater.inflate((R.menu.my_nav_item_night), menu)
		} else {
			// night mode is not active, inflate the day mode menu items.
			menuInflater.inflate((R.menu.my_nav_items), menu)
		}
		return super.onCreateOptionsMenu(menu)
	}

	/**
	 * Handles the click events for the menu items in the toolbar.
	 * @param item The menu item that was clicked.
	 * @return true if the event was handled, false otherwise.
	 */
	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.logout -> {
				val logoutAlert = AlertDialog.Builder(this, R.style.time_selector_theme)
				logoutAlert.setMessage("Are you sure you want to logout?")
				logoutAlert.setPositiveButton("Ok") { _, _ -> logout() }
				logoutAlert.setNegativeButton("Cancel") { alert, _ -> alert.dismiss() }
				val alertBox = logoutAlert.create()
				alertBox.show()

				val positiveButton = alertBox.getButton(DialogInterface.BUTTON_POSITIVE)
				val negativeButton = alertBox.getButton(DialogInterface.BUTTON_NEGATIVE)

				if (QuizScreen.getNightModeActive(this)) {
					// night mode is active, set the button colors to the night mode colors.
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
					// night mode is not active, set the button colors to the day mode colors.
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
				return true
			}

			R.id.settings -> {
				val newIntent = Intent(this, DailyQuizSettings::class.java)
				startActivity(newIntent)
				finish()
				return true
			}
		}
		return super.onOptionsItemSelected(item)
	}

	/**
	 * Logs the user out of the app. Uses Firebase Authentication which can be found at:
	 * https://firebase.google.com/docs/auth
	 */
	private fun logout() {
		auth.signOut()
		val newIntent = Intent(this, MainActivity::class.java)
		newIntent.putExtra("snack_bar_msg", getString(R.string.logout_message))
		startActivity(newIntent)
		finish()
	}
}