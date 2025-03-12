package com.example.get2class

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.get2class.UploadScheduleActivity.Companion
import com.example.get2class.UploadScheduleActivity.Companion.MODE
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter

private const val TAG = "UploadScheduleActivity"

@Parcelize
data class Course(
    val name: String,
    val days: List<Boolean>,
    val startTime: Pair<Int, Int>,
    val endTime: Pair<Int, Int>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val location: String,
    val credits: Double,
    val format: String,
    var attended: Boolean
) : Parcelable

@Parcelize
data class Schedule(val courses: List<Course>) : Parcelable

class UploadScheduleActivity : AppCompatActivity() {

    private lateinit var schedule: Schedule

    companion object {
        private const val REQUEST_CODE = 100 // Define request code for file selection
        private const val LISTING = 1
        private const val CREDITS = 2
        private const val FORMAT = 5
        private const val MODE = 6
        private const val PATTERN = 7
        private const val START_DATE = 10
        private const val END_DATE = 11
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_upload_schedule)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Log.d(TAG, "Term: ${intent.getStringExtra("term")}")

        // Launch the file picker to select an .xlsx file
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        }
        startActivityForResult(intent, REQUEST_CODE)
    }

    // Handle the result from the file picker
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data == null || data.data == null) {
            Log.e(TAG, "No file selected")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            data.data?.let { uri ->
                schedule = readExcelFromUri(uri)
                val resultIntent = Intent().apply {
                    putExtra("schedule", schedule)
                }
                setResult(RESULT_OK, resultIntent) // Send result back to MainActivity
                finish() // Ensure the activity finishes after setting the result
            }
        } else {
            setResult(RESULT_CANCELED) // Handle case where user cancels selection
            finish()
        }
    }

    // Step 3: Read the Excel file and process its data
    private fun readExcelFromUri(uri: Uri): Schedule {
        val courses: MutableList<Course> = mutableListOf()
        val coursesAsNotCourseObject: MutableList<JSONObject> = mutableListOf()

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)

                processSheet(sheet, courses, coursesAsNotCourseObject)

                Log.d(TAG, "Courses object: $courses")
                storeSchedule(BuildConfig.BASE_API_URL + "/schedule", coursesAsNotCourseObject) { result ->
                    Log.d(TAG, "$result")
                }
                workbook.close()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error opening or reading the file", e)
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for file access", e)
        }
        return Schedule(courses)
    }

    private fun storeSchedule(url: String, courses: MutableList<JSONObject>, callback: (JSONObject) -> Unit) {
        Log.d(TAG, "Storing schedule to database")

        // Create JSONObject to send
        val jsonObject = JSONObject()
        jsonObject.put("sub", LoginActivity.GoogleIdTokenSub)
        jsonObject.put(ScheduleListActivity.term, JSONArray(courses))

        // Create RequestBody and Request for OkHttp3
        val body = RequestBody.create(ApiService.JSON, jsonObject.toString())
        val request = Request.Builder().url(url).put(body).build()

        // Make call
        ApiService.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "Error: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body()?.string()
                if (result != null) {
                    try {
                        val jsonObject = JSONObject(result)
                        callback(jsonObject)
                    } catch (_: Exception) {
                        val badJsonObject = JSONObject()
                        callback(badJsonObject)
                    }
                }
            }
        })
    }

    private fun processSheet(sheet: Sheet, courses: MutableList<Course>, coursesAsNotCourseObject: MutableList<JSONObject>) {
        for (rowIndex in 3 until sheet.physicalNumberOfRows) {
            val row = sheet.getRow(rowIndex) ?: continue

            if (!isInPerson(row)) {
                Log.e(TAG, "Class rejected with reason: Not in person")
                continue
            }

            val (fullName, credits, dateRange) = parseRow(row)
            val (startDate, endDate) = dateRange

            if (!checkTerm(startDate, endDate)) {
                Log.e(TAG, "Class rejected with reason: Not this term")
                continue
            }

            val pattern = row.getCell(PATTERN)?.toString().orEmpty()

            if (pattern.isEmpty()) continue

            val patternList = pattern.split("|")

            val daysBool = createDaysList(patternList)
            val (startTime, endTime) = createTimes(patternList)

            val location = extractLocation(patternList)
            Log.d(TAG, "Location: $location")

            val format = row.getCell(FORMAT)?.toString() ?: ""
            Log.d(TAG, "Format: $format")

            initializeCourses(
                courses,
                coursesAsNotCourseObject,
                Course(
                    fullName,
                    daysBool,
                    startTime,
                    endTime,
                    startDate,
                    endDate,
                    location,
                    credits,
                    format,
                    false
                )
            )

        }
    }

    private fun parseRow(row: Row): Triple<String, Double, Pair<LocalDate, LocalDate>> {
        val listing = row.getCell(LISTING).toString()
        val fullName = listing.substringBefore('_') + ' ' + listing.substringAfter(' ')
        Log.d(TAG, "Full name: $fullName")

        // Get the credits
        val credits = row.getCell(CREDITS).toString().toDouble()
        Log.d(TAG, "Credits: $credits")

        // Create the start date and end date
        val dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")
        val startDate = LocalDate.parse(row.getCell(START_DATE).toString(), dateFormatter)
        Log.d(TAG, "Start Date: $startDate")
        val endDate = LocalDate.parse(row.getCell(END_DATE).toString(), dateFormatter)
        Log.d(TAG, "End Date: $endDate")

        return Triple(fullName, credits, startDate to endDate)
    }

    // Check that the class is in the right term
    private fun checkTerm(startDate: LocalDate, endDate: LocalDate): Boolean {
        val term = intent.getStringExtra("term")
        return when (term) {
            "Fall" -> startDate.month == Month.SEPTEMBER && endDate.month == Month.DECEMBER
            "Winter" -> startDate.month == Month.JANUARY && endDate.month == Month.APRIL
            else -> startDate.month in listOf(Month.MAY, Month.JULY) &&
                    endDate.month in listOf(Month.JUNE, Month.AUGUST)
        }
    }

    private fun isInPerson(row: Row): Boolean {
        return row.getCell(MODE)?.toString() == "In Person Learning"
    }
}



private fun extractLocation(patternList: List<String>): String {
    val locationList = patternList.getOrNull(3)?.split("[-\n]".toRegex()) ?: return "Unknown"
    return "${locationList.getOrNull(0)?.trim()} - ${locationList.getOrNull(2)?.trim()}"
}

private fun createDaysList(patternList: List<String>): MutableList<Boolean> {
    val days = patternList[1].split(" ")
    val daysBool = mutableListOf(false, false, false, false, false)
    if (days.contains("Mon")) daysBool[0] = true
    if (days.contains("Tue")) daysBool[1] = true
    if (days.contains("Wed")) daysBool[2] = true
    if (days.contains("Thu")) daysBool[3] = true
    if (days.contains("Fri")) daysBool[4] = true
    Log.d(TAG, "Days: $daysBool")
    return daysBool
}

private fun createTimes(patternList: List<String>): Pair<Pair<Int, Int>, Pair<Int, Int>>{
    val times = patternList[2].split("-")
    val startTimeParts = times[0].split("[ :]".toRegex())
    Log.d(TAG, "Start time parts: $startTimeParts")
    var startTime = startTimeParts[1].toInt() to startTimeParts[2].toInt()
    if (startTime.first != 12 && startTimeParts[3] == "p.m.") {
        startTime = startTime.first + 12 to startTime.second
    }
    Log.d(TAG, "Start time: $startTime")

    // Create the end time
    val endTimeParts = times[1].split("[ :]".toRegex())
    var endTime = endTimeParts[1].toInt() to endTimeParts[2].toInt()
    if (endTime.first != 12 && endTimeParts[3] == "p.m.") {
        endTime = endTime.first + 12 to endTime.second
    }
    Log.d(TAG, "End time: $endTime")

    return startTime to endTime
}

private fun initializeCourses(courses: MutableList<Course>, coursesAsNotCourseObject: MutableList<JSONObject>, course: Course) {
    courses.add(course)
    coursesAsNotCourseObject.add(JSONObject()
        .put("name", course.name)
        .put("daysBool", course.days)
        .put("startTime", course.startTime)
        .put("endTime", course.endTime)
        .put("startDate", course.startDate)
        .put("endDate", course.endDate)
        .put("location", course.location)
        .put("credits", course.credits)
        .put("format", course.format)
        .put("attended", false)
    )
}