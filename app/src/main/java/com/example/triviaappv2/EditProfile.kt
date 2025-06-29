package com.example.triviaappv2

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.triviaappv2.QuizScreen.Companion.getNightModeActive
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

/**
 * The edit profile activity is where the user can edit their profile information.
 * This activity makes use of external libraries such as Glide, Firebase Authentication,
 * Firebase Firestore and Firebase Storage. Glide is used to load the user's profile picture,
 * Firebase Authentication is used to get the current logged in user and Firebase Firestore
 * is used to get and update the user's profile information. Firebase Storage is used to store
 * and update the users profile picture. The libraries can be found at:
 * https://github.com/bumptech/glide,
 * https://firebase.google.com/docs/auth,
 * https://firebase.google.com/docs/firestore,
 * https://firebase.google.com/docs/storage
 */
class EditProfile : AppCompatActivity() {
	private var auth = FirebaseAuth.getInstance()
	private var loggedInUser = auth.currentUser
	private val database = Firebase.firestore
	private val storage = Firebase.storage
	private lateinit var usernameField: EditText
	private lateinit var profilePic: ImageView
	private val loggedInUserEmail = loggedInUser!!.email
	private lateinit var imageSelector: ActivityResultLauncher<Intent>

	/**
	 * onCreate is called when the activity is first created.
	 * @param savedInstanceState is the saved instance state of the activity.
	 */
	override fun onCreate(savedInstanceState: Bundle?) {
		imageSelector = registerForActivityResult(
			ActivityResultContracts
				.StartActivityForResult()
		) { result ->
			if (result.resultCode == RESULT_OK) {
				val imageUri = result.data?.data
				setProfilePic(imageUri)
			}
		}
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_edit_profile)

		val toolbar = findViewById<Toolbar>(R.id.toolbar)
		val saveButton = findViewById<Button>(R.id.save_changes_button)
		val deleteProfileButton = findViewById<Button>(R.id.delete_profile_button)
		val uploadProfilePicButton = findViewById<Button>(R.id.upload_photo_button)
		profilePic = findViewById(R.id.edit_profile_pic)
		usernameField = findViewById(R.id.username_change)
		loadUsername()
		loadProfilePic()
		setSupportActionBar(toolbar)
		toolbar.setNavigationOnClickListener { _ -> back() }
		saveButton.setOnClickListener { saveChanges() }
		deleteProfileButton.setOnClickListener { deleteAlert() }
		uploadProfilePicButton.setOnClickListener {
			requestGalleryPermission()
		}
	}

	/**
	 * back is called when the user clicks the back button.
	 * It takes the user back to the home page.
	 */
	private fun back() {
		val backIntent = Intent(this, HomePage::class.java)
		startActivity(backIntent)
		finish()
	}

	/**
	 * loadUsername loads the currently logged in user's username from the database.
	 * Firebase Firestore is used to get the user's username and Firebase Authentication
	 * is used to get the current logged in user. The libraries can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 */
	private fun loadUsername() {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).get()
			.addOnSuccessListener { document ->
				if (document != null) {
					// The user exists in the database. Display their username.
					usernameField.setText(document.getString("username"))
				}
			}
	}

	/**
	 * saveChanges is called when the user clicks the save changes button. It saves the changes
	 * that a user has made to their username and updates this in the database. Firebase Firestore
	 * and Authentication is used to save the changes based on who the currently logged in user is.
	 * The libraries can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 */
	private fun saveChanges() {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).update(
			"username",
			usernameField.text.toString()
		)
		loadUsername()
	}

	/**
	 * Creates an alert dialog to ask the user if they want to delete their account.
	 */
	private fun deleteAlert() {
		val deleteAlert = AlertDialog.Builder(this, R.style.time_selector_theme)
		deleteAlert.setMessage("Are you sure you want to delete your account?")
		deleteAlert.setPositiveButton("Ok") { _, _ ->
			val intent = Intent(this, MainActivity::class.java)
			intent.putExtra("delete", true)
			intent.putExtra("email", loggedInUserEmail)
			startActivity(intent)
			finish()
		}
		deleteAlert.setNegativeButton("Cancel") { alert, _ -> alert.dismiss() }
		val alertBox = deleteAlert.create()
		alertBox.show()
		val positiveButton = alertBox.getButton(AlertDialog.BUTTON_POSITIVE)
		val negativeButton = alertBox.getButton(AlertDialog.BUTTON_NEGATIVE)
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

	/**
	 * sets the profile picture that was selected by the user. Uploads the picture to cloud storage
	 * and gets a download url which is then stored in the users document in the database.
	 * The libraries used were Firebase Storage, Firebase Firestore and Firebase Authentication.
	 * The libraries can be found at:
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore,
	 * https://firebase.google.com/docs/storage
	 */
	private fun setProfilePic(imageUrl: Uri?) {
		val imageFilePath = "profile_pics/${loggedInUser!!.uid}.jpg"
		val imageRef = storage.reference.child(imageFilePath)
		imageRef.putFile(imageUrl!!).addOnSuccessListener {
			imageRef.downloadUrl.addOnSuccessListener { uri ->
				val userId = loggedInUser!!.uid
				database.collection("users").document(userId).update(
					"profilePictureUrl",
					uri.toString()
				)
			}
		}
	}

	/**
	 * Requests the permission to access the gallery if permission is not granted and then
	 * opens the gallery.
	 */
	private fun requestGalleryPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			// Build version is greater than or equal to Tiramisu so request read media images.
			if (ContextCompat
					.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
				!= PackageManager.PERMISSION_GRANTED
			) {
				// Permission is not granted so request it.
				ActivityCompat.requestPermissions(
					this,
					arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
					1
				)
			} else {
				// Permission is granted so access the gallery.
				accessGallery()
			}
		} else {
			// Build version is less than Tiramisu so request read external storage.
			if (ContextCompat
					.checkSelfPermission(
						this, android.Manifest.permission
							.READ_EXTERNAL_STORAGE
					)
				!= PackageManager.PERMISSION_GRANTED
			) {
				// Permission is not granted so request it.
				ActivityCompat.requestPermissions(
					this,
					arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
					1
				)
			} else {
				// Permission is granted so access the gallery.
				accessGallery()
			}
		}
	}

	/**
	 * Opens the gallery and gets the selected image uri using the image selector.
	 */
	private fun accessGallery() {
		val galleryIntent = Intent(Intent.ACTION_PICK, EXTERNAL_CONTENT_URI)
		galleryIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
		imageSelector.launch(galleryIntent)
	}

	/**
	 * Loads the users profile picture from the database. Firebase Authentication, Firestore and
	 * Glide are used to load the users profile picture. The libraries can be found at:
	 * https://github.com/bumptech/glide,
	 * https://firebase.google.com/docs/auth,
	 * https://firebase.google.com/docs/firestore
	 */
	private fun loadProfilePic() {
		val userId = loggedInUser!!.uid
		database.collection("users").document(userId).get()
			.addOnSuccessListener { document ->
				if (document != null) {
					// The user exists in the database. Display their profile picture.
					val profilePicUrl = document.getString("profilePictureUrl")
					if (profilePicUrl != "") {
						// User has a custom profile picture. Load it from storage.
						Glide.with(this).load(profilePicUrl).into(profilePic)
					} else {
						// User has no custom profile picture. Use default profile picture.
						profilePic.setImageResource(R.mipmap.default_profile_pic)
					}
				}
			}
	}
}