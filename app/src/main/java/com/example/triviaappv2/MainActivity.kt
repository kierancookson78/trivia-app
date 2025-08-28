package com.example.triviaappv2

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

/**
 * The main activity acts as the login/signup page of the app.
 * This class uses firebase authentication to log in and sign up users and firebase firestore
 * to store their data. Firebase Storage is also used to delete the user's profile picture.
 * The libraries can be found at:
 * https://firebase.google.com/docs/firestore,
 * https://firebase.google.com/docs/auth,
 * https://firebase.google.com/docs/storage
 */
class MainActivity : AppCompatActivity() {
    private lateinit var signupBtn: Button
    private lateinit var loginBtn: Button
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var usernameField: EditText
    private var auth = FirebaseAuth.getInstance()
    private var loggedInUser = auth.currentUser
    private lateinit var loggedinmsg: String
    private val database = Firebase.firestore
    private val storage = Firebase.storage.reference
    private var requestedDelete = false
    private var requestedDeleteEmail = ""

    /**
     * onCreate is called when the activity is first created.
     * @param savedInstanceState is the activity's previously saved state.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val extras = intent.extras
        signupBtn = findViewById(R.id.signup_button)
        loginBtn = findViewById(R.id.login_button)
        emailField = findViewById(R.id.email_field)
        passwordField = findViewById(R.id.password_field)
        usernameField = findViewById(R.id.username_field)
        requestedDelete = extras?.getBoolean("delete") == true
        requestedDeleteEmail = extras?.getString("email") ?: ""

        signupBtn.setOnClickListener { view ->
            signup(view)
        }
        loginBtn.setOnClickListener { _ -> login() }

        if (requestedDelete) {
            auth.signOut()
            val snack =
                Snackbar.make(loginBtn, "Login to delete your account", Snackbar.LENGTH_LONG)
            snack.show()
        }
    }

    /**
     * signup is called when the user clicks the signup button which will create a
     * new user in firebase auth and store their data in firebase firestore.
     * The libraries can be found at:
     * https://firebase.google.com/docs/firestore,
     * https://firebase.google.com/docs/auth
     *
     * @param view is the to display a snack bar on the status of the signup process.
     */
    private fun signup(view: View) {
        if (emailField.text.toString().isNotBlank() && passwordField.text.toString()
                .isNotBlank() && usernameField.text.toString().isNotBlank()
        ) {
            auth.createUserWithEmailAndPassword(
                emailField.text.toString(),
                passwordField.text.toString()
            ).addOnCompleteListener(this) { signedup ->
                if (signedup.isSuccessful) {
                    // If the signup was successful, create a new user in firebase firestore.
                    hideKeyboard()
                    val msgDisplay = Snackbar.make(
                        view,
                        getString(R.string.successful_signup),
                        Snackbar.LENGTH_SHORT
                    )
                    msgDisplay.show()
                    val userId = auth.currentUser?.uid
                    val user = User(
                        "",
                        usernameField.text.toString(),
                        0,
                        "Bronze",
                        1,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        1,
                        1
                    )
                    database.collection("users")
                        .document(userId!!)
                        .set(user, SetOptions.merge())
                        .addOnCompleteListener {
                            createTopicStatsDocuments()
                        }
                    auth.signOut()
                    update()
                    usernameField.text.clear()
                    emailField.text.clear()
                    passwordField.text.clear()
                    usernameField.requestFocus()
                } else {
                    // If the signup was not successful, display an error message.
                    hideKeyboard()
                    val msgDisplay = Snackbar.make(
                        view,
                        getString(R.string.unsuccessful_signup),
                        Snackbar.LENGTH_SHORT
                    )
                    msgDisplay.show()
                }
            }
        } else {
            val msgDisplay = Snackbar.make(
                loginBtn,
                "Email, Username and/or Password not entered.",
                Snackbar.LENGTH_SHORT
            )
            msgDisplay.show()
        }
    }

    /**
     * login is called when the user clicks the login button which will log in the user using their
     * email and password and is handled by firebase auth.
     * The library can be found at:
     * https://firebase.google.com/docs/auth,
     */
    private fun login() {
        if (emailField.text.toString().isNotBlank() && passwordField.text.toString().isNotBlank()) {
            auth.signInWithEmailAndPassword(
                emailField.text.toString(),
                passwordField.text.toString()
            ).addOnCompleteListener(this) { login ->
                if (login.isSuccessful) {
                    // If the login was successful, log the user in if they have not requested a delete.
                    hideKeyboard()
                    update()
                    if (requestedDelete) {
                        // User has requested to delete their account.
                        if (requestedDeleteEmail == loggedInUser?.email) {
                            // The account requested to be deleted is the currently logged in user.
                            deleteUserFromDatabase()
                            deleteProfile { deleted ->
                                if (deleted) {
                                    // The account has been deleted. Clear fields and display message.
                                    usernameField.text.clear()
                                    emailField.text.clear()
                                    passwordField.text.clear()
                                    usernameField.requestFocus()
                                    requestedDelete = false
                                    val snack =
                                        Snackbar.make(
                                            loginBtn, "Account deleted",
                                            Snackbar.LENGTH_LONG
                                        )
                                    snack.show()
                                }
                            }
                        } else {
                            // The account requested to be deleted is not the currently logged in user.
                            val snack =
                                Snackbar.make(
                                    loginBtn,
                                    "Incorrect details for delete",
                                    Snackbar.LENGTH_LONG
                                )
                            snack.show()
                            usernameField.text.clear()
                            emailField.text.clear()
                            passwordField.text.clear()
                            usernameField.requestFocus()
                            auth.signOut()
                        }
                    } else {
                        // User has not requested to delete their account so log them in.
                        val intent = Intent(this, HomePage::class.java)
                        loggedinmsg = getString(R.string.welcome_msg)
                        intent.putExtra("logged_in", loggedinmsg)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    // If the login was not successful, display an error message.
                    hideKeyboard()
                    val msgDisplay = Snackbar.make(
                        loginBtn,
                        getString(R.string.unsuccessful_login),
                        Snackbar.LENGTH_SHORT
                    )
                    msgDisplay.show()
                }
            }
        } else {
            val msgDisplay = Snackbar.make(
                loginBtn,
                "Email and/or Password not entered.",
                Snackbar.LENGTH_SHORT
            )

            msgDisplay.show()
        }
    }

    /**
     * update is called when the user logs in or logs out to update the currently logged in user.
     * Uses firebase auth which can be found at:
     * https://firebase.google.com/docs/auth
     */
    private fun update() {
        loggedInUser = auth.currentUser
    }

    /**
     * hideKeyboard is called when the user clicks the signup or login button to hide the keyboard.
     * This is taken from CSC306 Lecture 12 by Tom Owen found at: https://canvas.swansea.ac.uk
     * /courses/52677/pages/lecture-12-firebase?module_item_id=2984546
     */
    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            // Hide the keyboard using the input method manager.
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /**
     * onStart is called when the activity is started. If the user is already logged in, they will
     * be sent to the home page. Uses firebase auth which can be found at:
     * https://firebase.google.com/docs/auth
     */
    override fun onStart() {
        super.onStart()
        val currentEmail = loggedInUser?.email

        if (currentEmail != null && !requestedDelete) {
            // User logged in not and no delete requested. Send them to the home page.
            val intent = Intent(this, HomePage::class.java)
            startActivity(intent)
            finish()
        }
        update()
    }

    /**
     * Deletes user from the authentication system. Uses firebase auth which can be found at
     * https://firebase.google.com/docs/auth
     */
    private fun deleteProfile(callback: (Boolean) -> Unit) {
        auth.currentUser!!.delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // User account deleted successfully so send callback with true.
                    val deleted = true
                    callback(deleted)
                }
            }
    }

    /**
     * deletes user from the database and firebase storage. Uses firebase firestore, firebase auth
     * and firebase storage which can be found at:
     * https://firebase.google.com/docs/firestore,
     * https://firebase.google.com/docs/auth,
     * https://firebase.google.com/docs/storage
     */
    private fun deleteUserFromDatabase() {
        val userId = loggedInUser!!.uid
        val imageFilePathRef = storage.child("profile_pics/${userId}.jpg")

        database.collection("users").document(userId)
            .collection("responses").get()
            .addOnSuccessListener { documents ->
                val batch = database.batch()
                for (document in documents) {
                    // Delete all the user's responses
                    batch.delete(document.reference)
                }
                batch.commit()
            }

        database.collection("users").document(userId)
            .collection("topics").get()
            .addOnSuccessListener { documents ->
                val batch = database.batch()
                for (document in documents) {
                    // Delete all the user's topic stats
                    batch.delete(document.reference)
                }
                batch.commit()
            }

        database.collection("users").document(userId).delete()
        imageFilePathRef.delete()
    }

    /**
     * Creates the topic stats documents for the user. Uses firebase firestore
     * and firebase auth which can be found at:
     * https://firebase.google.com/docs/firestore,
     * https://firebase.google.com/docs/auth
     */
    private fun createTopicStatsDocuments() {
        val userId = loggedInUser!!.uid
        val topics = resources.getStringArray(R.array.topics)
        for (topic in topics) {
            // Create a stats document for each topic for the user.
            val topicsCollection =
                database.collection("users").document(userId)
                    .collection("topics").document(topic)
            val topicStats = hashMapOf(
                "amountAnswered" to 0,
                "correctAnswers" to 0,
                "percentageCorrect" to 0
            )

            topicsCollection.set(topicStats)
        }
    }
}