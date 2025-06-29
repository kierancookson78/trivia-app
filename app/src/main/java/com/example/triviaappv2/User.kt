package com.example.triviaappv2

/**
 * The User class is used to create an instance of a user and then their
 * data is stored in the database.
 */
data class User(
	val profilePictureUrl: String? = null,
	val username: String? = null,
	val points: Int? = null,
	val rank: String? = null,
	val level: Int? = null,
	val xp: Int? = null,
	val numberOfQuestions: Int? = null,
	val numberOfCorrectAnswers: Int? = null,
	val streak: Int? = null,
	val lastDateAnswered: Int? = null,
	val currentStreak: Int? = null,
	val longestStreak: Int? = null,
	val selectedTopic: Int? = null,
	val selectedQuestionType: Int? = null,
	val numberOfQuestionsSelected: Int? = null
)
