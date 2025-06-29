package com.example.triviaappv2

import java.util.Calendar
import java.util.Date

/**
 * Past Response is used to create an instance of a past response which is used
 * to store a past response in the database.
 */
data class PastResponse(
	val question: String,
	val choices: ArrayList<String>,
	val selectedAnswer: String,
	val correctAnswer: String,
	val createdAt: Date = Calendar.getInstance().time
)
