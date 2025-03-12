package com.example.get2class

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ScheduleListActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ScheduleListActivity"
        var term: String = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_schedule_list)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.schedule_list)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Fall Schedule Button
        findViewById<Button>(R.id.fall_schedule).setOnClickListener {
            Log.d(TAG, "Fall schedule button clicked")
            term = "fallCourseList"
            val intent = Intent(this, ViewScheduleActivity::class.java)
            intent.putExtra("term", "Fall")
            startActivity(intent)
        }

        // Winter Schedule Button
        findViewById<Button>(R.id.winter_schedule).setOnClickListener {
            Log.d(TAG, "Winter schedule button clicked")
            term = "winterCourseList"
            val intent = Intent(this, ViewScheduleActivity::class.java)
            intent.putExtra("term", "Winter")
            startActivity(intent)
        }

        // Summer Schedule Button
        findViewById<Button>(R.id.summer_schedule).setOnClickListener {
            Log.d(TAG, "Summer schedule button clicked")
            term = "summerCourseList"
            val intent = Intent(this, ViewScheduleActivity::class.java)
            intent.putExtra("term", "Summer")
            startActivity(intent)
        }
    }
}
