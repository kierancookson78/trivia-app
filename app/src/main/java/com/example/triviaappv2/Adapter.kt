package com.example.triviaappv2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

/**
 * The Adapter class binds the leaderboard data its RecyclerView. This class makes use of Glide to
 * load the profile pictures from cloud storage into the profile picture column of the leaderboard.
 * Glide is found at: https://github.com/bumptech/glide. The code for this class is an adapted
 * version of the code from the recyclerview skeleton example from lecture 7 of CSC306 by Tom Owen
 * which can be found at: https://canvas.swansea.ac.uk/courses/52677/pages
 * /recyclerview-skeleton-example?module_item_id=2984525 under MyAdapter.kt
 */
class Adapter(private val leaderBoardList: ArrayList<ArrayList<String>>) :
	RecyclerView.Adapter<Adapter.ViewHolder>() {

	/**
	 * Used to inflate the views into the leaderboard layout.
	 * @param parent The parent view group.
	 * @param viewType The view type.
	 *
	 * @return A ViewHolder with the inflated view.
	 */
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(
			R.layout.leaderboard_layout,
			parent, false
		)
		return ViewHolder(view)
	}

	/**
	 * Gets the amount of rows in the leaderboard.
	 * @return The amount of rows in the leaderboard.
	 */
	override fun getItemCount(): Int {
		return leaderBoardList.size
	}

	/**
	 * Binds the data to the views in the ViewHolder.
	 * @param holder The ViewHolder.
	 * @param position The position of the row within the leaderboard.
	 */
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val row = leaderBoardList[position]
		loadProfilePic(holder, row[0])
		holder.userCol.text = row[1]
		holder.pointsCol.text = row[2]
		holder.rankCol.text = row[3]
	}

	/**
	 * The ViewHolder which acts as a parent class to the rows of the leaderboard which are
	 * represented in the child views.
	 */
	inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		val picCol: ImageView = itemView.findViewById(R.id.pic_col)
		val userCol: TextView = itemView.findViewById(R.id.user_col)
		val pointsCol: TextView = itemView.findViewById(R.id.points_col)
		val rankCol: TextView = itemView.findViewById(R.id.rank_col)
	}

	/**
	 * loadProfilePic loads the profile picture from cloud storage into the profile picture column
	 * and uses Glide to do this. If there is no profile picture, a default profile picture is used.
	 * Glide is found at: https://github.com/bumptech/glide
	 *
	 * @param holder The ViewHolder that contains the leaderboard row.
	 * @param profilePicUrl The URL of the profile picture.
	 */
	private fun loadProfilePic(holder: ViewHolder, profilePicUrl: String) {
		if (profilePicUrl != "") {
			// The user has a custom profile picture so load it into the profile picture column.
			Glide.with(holder.picCol).load(profilePicUrl).into(holder.picCol)
		} else {
			// Use the default profile picture.
			holder.picCol.setImageResource(R.mipmap.default_profile_pic)
		}
	}
}