
package com.example.get2class

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CalendarAdapter(private val context: Context,
                      private val cells: List<Pair<Int, Int>>,
                      private val eventsMap: Map<Pair<Int, Int>, Course?>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "CalendarAdapter"
        private const val TYPE_DAY_HEADER = 1
        private const val TYPE_TIME_LABEL = 2
        private const val TYPE_CELL = 3
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DAY_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.day_header, parent, false)
                view.layoutParams.height = 30.dpToPx(parent.context) // Enforce a fixed height
                DayHeaderViewHolder(view)
            }
            TYPE_TIME_LABEL -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.time_label, parent, false)
                view.layoutParams.height = 30.dpToPx(parent.context) // Enforce a fixed height
                TimeLabelViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.calendar_cell, parent, false)
                view.layoutParams.height = 30.dpToPx(parent.context) // Enforce a fixed height
                CalendarCellViewHolder(view)
            }
        }
    }
    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    override fun getItemViewType(position: Int): Int {
        val (day, index) = cells[position]
        return when {
            index == -1 -> TYPE_DAY_HEADER // First row (headers)
            day == 0 -> TYPE_TIME_LABEL    // First column (time labels)
            else -> TYPE_CELL              // Regular calendar cell
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (dayOfWeek, halfHourIndex) = cells[position]
        when (holder) {
            is DayHeaderViewHolder -> {
                val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
                holder.dayHeaderText.text = if (dayOfWeek in 1..5) dayNames[dayOfWeek - 1] else ""
            }
            is TimeLabelViewHolder -> {
                val hour = 8 + (halfHourIndex / 2) // Example: Start at 8 AM
                val minute = if (halfHourIndex % 2 == 0) "00" else "30"
                holder.timeLabelText.text = if (hour < 10) "  $hour:$minute" else "$hour:$minute"
            }
            is CalendarCellViewHolder -> {

                val course = eventsMap[dayOfWeek to halfHourIndex]

                if (course != null) {
                    createCourseCell(holder, course, position, dayOfWeek, halfHourIndex)
                } else {
                    holder.cellText.text = ""
                    holder.cellFrame.setBackgroundColor(Color.WHITE)
                    holder.cellFrame.setOnClickListener(null)
                }
            }
        }
    }

    private fun createCourseCell(holder: CalendarCellViewHolder, course: Course, position: Int, dayOfWeek: Int, halfHourIndex: Int) {
        holder.cellText.text = course.name.substringBefore("-")
        if (position > 0) {
            val prevCourse = eventsMap[dayOfWeek to halfHourIndex - 1]
            if (prevCourse != null && prevCourse.name == course.name && prevCourse.format == course.format) {
                setCellText(holder, course, position, dayOfWeek, halfHourIndex)
            }
        }
        holder.bottomBorder.visibility = View.GONE
        holder.cellFrame.setBackgroundColor(Color.parseColor("#B2FF59"))
        holder.cellFrame.setOnClickListener {
            Log.d(TAG, "Course was clicked: $course")

            val intent = Intent(context, ClassInfoActivity::class.java)
            intent.putExtra("course", course)
            context.startActivity(intent)
        }
    }

    private fun setCellText(holder: CalendarCellViewHolder, course: Course, position: Int, dayOfWeek: Int, halfHourIndex: Int) {
        holder.cellText.text = course.format
        holder.topBorder.visibility = View.GONE
        if (position > 1) {
            val prevPrevCourse = eventsMap[dayOfWeek to halfHourIndex - 2]
            if (prevPrevCourse != null && prevPrevCourse.name == course.name && prevPrevCourse.format == course.format) {
                holder.cellText.text = ""
                holder.topBorder.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = cells.size
}

class CalendarCellViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val cellText: TextView = itemView.findViewById(R.id.cellTextView)
    val cellFrame: View = itemView.findViewById(R.id.cellFrame) // Added this line
    val bottomBorder: View = itemView.findViewById(R.id.bottomBorder) // Added border reference
    val topBorder: View = itemView.findViewById(R.id.topBorder) // Added border reference
}

class DayHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val dayHeaderText: TextView = itemView.findViewById(R.id.dayHeaderText)
}

class TimeLabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val timeLabelText: TextView = itemView.findViewById(R.id.timeLabelText)
}
