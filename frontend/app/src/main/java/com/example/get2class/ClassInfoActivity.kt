package com.example.get2class

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.Manifest
import android.app.Activity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.*
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.view.View
import android.widget.Toast
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.snackbar.Snackbar
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.Month
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val TAG = "ClassInfoActivity"

// For accessing the current location
private lateinit var fusedLocationClient: FusedLocationProviderClient
private const val LOCATION_PERMISSION_REQUEST_CODE = 666
private lateinit var locationManager: LocationManager
private var current_location: Pair<Double, Double>? = null
private var isOnCreate: Boolean = true

// for places api
private lateinit var placesClient: PlacesClient
private lateinit var destinationPlaceId: String

class ClassInfoActivity : AppCompatActivity(), LocationListener {

    companion object {
        private const val MINUTES = 1.0 / 60.0
    }
    lateinit var mainView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_class_info)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        mainView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)

        // Get the course from the intent and return if it's null
        val course: Course = intent.getParcelableExtra("course") ?: return

        // Set the values of the text fields
        findViewById<TextView>(R.id.course_name).text = course.name
        findViewById<TextView>(R.id.course_format).text = course.format
        findViewById<TextView>(R.id.course_time).text = "${course.startTime.to12HourTime(false)} - ${course.endTime.to12HourTime(true)}"
        findViewById<TextView>(R.id.course_days).text = daysToString(course)
        findViewById<TextView>(R.id.course_location).text = "Location: ${course.location}"
        findViewById<TextView>(R.id.course_credits).text = "Credits: ${course.credits}"

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // initialize the Places Client
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, com.example.get2class.BuildConfig.MAPS_API_KEY)
        }
        placesClient = Places.createClient(this)

        // Get the current "attended" value from the DB
        getAttendance(BuildConfig.BASE_API_URL + "/attendance?sub=" + LoginActivity.GoogleIdTokenSub + "&className=" + course.name + "&classFormat=" + course.format + "&term=" + ScheduleListActivity.term) { result ->
            Log.d(TAG, "$result")
            course.attended = result.getBoolean("attended")
        }

        // Route to class Button
        findViewById<Button>(R.id.route_button).setOnClickListener {
            Log.d(TAG, "Route to class button clicked")
            val building = course.location.split("-")[0].trim()
            Log.d(TAG, "Building: $building")
            val intent = Intent(this, RouteActivity::class.java)
            intent.putExtra("building", building)
            startActivity(intent)
        }

        // Check attendance Button
        setCheckAttendanceButton(course)
    }

    private fun setCheckAttendanceButton(course: Course) {
        findViewById<Button>(R.id.check_attendance_button).setOnClickListener {
            Log.d(TAG, "Check attendance button clicked")

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000, 0f, this)
                Log.d(TAG, "OnCreate: Location updates requested")
            }

            // Format the current date and time and the class time
            val clientDate = getCurrentTime().split(" ") // day of week, hour, minute
            val clientDay = clientDate[0].toInt()
            val clientTime = clientDate[1].toDouble().plus(clientDate[2].toDouble() / 60)
            val classStartTime = course.startTime.first.toDouble() + course.startTime.second.toDouble() / 60
            val classEndTime = course.endTime.first.toDouble() + (course.endTime.second.toDouble() - 10) / 60

            Log.d(TAG, "Start: $classStartTime, end: $classEndTime, client: $clientTime")

            // Check that the current term and year match the term and year of the course
            if (checkTermAndYear(course)) {
                // Check if the course is today
                if (clientDay < 1 || clientDay > 5 || !course.days[clientDay - 1]) {
                    Log.d(TAG, "You don't have this class today")
                    Snackbar.make(mainView, "You don't have this class today", Snackbar.LENGTH_SHORT).show()
                } else if (checkTime(course, clientTime, classStartTime, classEndTime)) {
                    lifecycleScope.launch {
                        if (checkLocation(course)) {
                            // Check if you're late
                            if (classStartTime < clientTime - 2 * MINUTES) {
                                calculateKarma(arrayOf(clientTime, classStartTime, classEndTime), course, true)
                            } else {
                                calculateKarma(arrayOf(clientTime, classStartTime, classEndTime), course, false)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkTime(
        course: Course,
        clientTime: Double,
        classStartTime: Double,
        classEndTime: Double
    ): Boolean {
        // Check if the course has been attended yet
        if (course.attended) {
            Log.d(TAG, "You already checked into this class today!")
            Snackbar.make(mainView, "You already checked into this class today!", Snackbar.LENGTH_SHORT).show()
            return false
        }

        // Check if it's too early
        if (clientTime < classStartTime - 10 * MINUTES) {
            Log.d(TAG, "You are too early to check into this class!")
            Snackbar.make(mainView, "You are too early to check into this class!", Snackbar.LENGTH_SHORT).show()
            return false
        }

        // Check if it's too late
        if (classEndTime <= clientTime) {
            Log.d(TAG, "You missed your class!")
            Snackbar.make(mainView, "You missed your class!", Snackbar.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private suspend fun checkLocation(course: Course): Boolean {
        val clientLocation = requestCurrentLocation(this@ClassInfoActivity)
        val classLocation : Pair<Double?, Double?> = suspendCoroutine { continuation ->
            findLatLngFromPlaceId(course.location.split("-")[0].trim(), placesClient) { placeId ->
                if (placeId != null) {
                    // get the place id
                    destinationPlaceId = placeId
                } else {
                    // handle the case where no Place ID was found
                    Log.e(
                        "RouteActivity",
                        "findPlaceId: some errors occur ..."
                    )
                    // the default place id will then be life building
                    destinationPlaceId = "ChIJLzhNG7dyhlQRyzvy3GCiuT4"
                    Toast.makeText(
                        this,
                        "The class location is invalid; directing you to LIFE Building ...",
                        Toast.LENGTH_LONG
                    ).show()
                }

                val url = "https://maps.googleapis.com/maps/api/place/details/json?place_id=$placeId&key=${BuildConfig.MAPS_API_KEY}"

                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "Failed to fetch details: ${e.message}")
                        continuation.resume(Pair(null, null))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (!response.isSuccessful) {
                                Log.e(TAG, "Unexpected code $response")
                                continuation.resume(Pair(null, null))
                                return
                            }

                            val responseBody = response.body()?.string()
                            if (responseBody != null) {
                                val jsonResponse = JSONObject(responseBody)
                                if (jsonResponse.getString("status") == "OK") {
                                    val result = jsonResponse.getJSONObject("result")
                                    val geometry = result.getJSONObject("geometry")
                                    val location = geometry.getJSONObject("location")
                                    val lat = location.getDouble("lat")
                                    val lng = location.getDouble("lng")
                                    Log.d(TAG, "get classLocation: lat=$lat, lng=$lng")
                                    continuation.resume(Pair(lat, lng))
                                } else {
                                    Log.e(
                                        TAG,
                                        "Error: ${jsonResponse.getString("status")}"
                                    )
                                    continuation.resume(Pair(null, null))
                                }
                            } else {
                                continuation.resume(Pair(null, null))
                            }
                        }
                    }
                })
            }
        }

        if (clientLocation.first == null) {
            Snackbar.make(mainView, "Location data not available", Snackbar.LENGTH_SHORT).show()
            return false
        }

        if (coordinatesToDistance(clientLocation, classLocation) > 75) {
            Log.d(TAG, "You're too far from your class!")
            Snackbar.make(mainView, "You're too far from your class!", Snackbar.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        ) // Keep this at the beginning

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1_000,
                    0f,
                    this
                )
                Log.d(TAG, "onRequestPermissionsResult: Location updates requested")
            }

            lifecycleScope.launch {
                val location = getLastLocation(this@ClassInfoActivity)
                Log.d(TAG, "onRequestPermissionsResult: Location received: $location")
            }
        } else {
            Snackbar.make(mainView, "Please grant Location permissions in Settings to view your routes :/", Snackbar.LENGTH_SHORT).show()
            Log.d(TAG, "onRequestPermissionsResult: Permission denied")
        }
    }

    override fun onLocationChanged(p0: Location) {
        current_location = p0.latitude to p0.longitude
    }

    private fun checkTermAndYear(course: Course): Boolean {
        val term = ScheduleListActivity.term
        val start = course.startDate
        val end = course.endDate
        val curr = LocalDate.now()

        // Ensure the current year matches the course's start year
        if (curr.year != start.year) {
            Log.d("ClassInfoActivity", "You don't have this class this year")
            Snackbar.make(mainView, "You don't have this class this year", Snackbar.LENGTH_SHORT).show()
            return false
        }

        val ret = when (term) {
            "fallCourseList" -> curr.month in Month.SEPTEMBER..Month.DECEMBER
            "winterCourseList" -> curr.month in Month.JANUARY..Month.APRIL
            else -> curr.month in listOf(Month.MAY, Month.JUNE, Month.JULY, Month.AUGUST)
        } && curr in start..end
        if (ret) return true

        Log.d("ClassInfoActivity", "You don't have this class this term")
        Snackbar.make(mainView, "You don't have this class this term", Snackbar.LENGTH_SHORT).show()
        return false
    }

    fun calculateKarma(times: Array<Double>, course: Course, late: Boolean) {
        val karma: Int
        if (late) {
            val clientTime = times[0]
            val classStartTime = times[1]
            val classEndTime = times[2]
            val lateness = clientTime - classStartTime
            Log.d(TAG, "Your time: $clientTime. Class Start: $classStartTime.")
            Log.d(TAG, "You were late by ${(lateness * 60).toInt()} minutes!")
            val firstSnackbar = Snackbar.make(mainView, "You were late by ${(lateness * 60).toInt()} minutes!", Snackbar.LENGTH_SHORT)
            val classLength = classEndTime - classStartTime
            karma = (10 * (1 - lateness / classLength) * (course.credits + 1)).toInt()
            firstSnackbar.addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (event == DISMISS_EVENT_TIMEOUT || event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_ACTION) {
                        Snackbar.make(mainView, "You gained $karma Karma!", Snackbar.LENGTH_SHORT).show()
                    }
                }
            })
            firstSnackbar.show()
            updateKarma(BuildConfig.BASE_API_URL + "/karma", karma) { result ->
                Log.d(TAG, "$result")
            }
            updateAttendance(
                BuildConfig.BASE_API_URL + "/attendance",
                course.name,
                course.format
            ) { result ->
                Log.d(TAG, "$result")
                course.attended = true
            }
        } else {
            Log.d(TAG, "All checks passed")

            karma = (15 * (course.credits + 1)).toInt()
            updateKarma(BuildConfig.BASE_API_URL + "/karma", karma) { result ->
                Log.d(TAG, "$result")
            }
            updateAttendance(
                BuildConfig.BASE_API_URL + "/attendance",
                course.name,
                course.format
            ) { result ->
                Log.d(TAG, "$result")
                course.attended = true
            }
            Snackbar.make(mainView, "You gained $karma Karma!", Snackbar.LENGTH_SHORT).show()
        }
        Log.d(TAG, "You gained $karma Karma!")
    }
}

fun daysToString(course: Course): String {
    var days = ""
    var first = true
    if (course.days[0]) {
        days += "Mon"
        first = false
    }
    if (course.days[1]) {
        if (first) {
            days += "Tue"
            first = false
        } else {
            days += ", Tue"
        }
    }
    if (course.days[2]) {
        if (first) {
            days += "Wed"
            first = false
        } else {
            days += ", Wed"
        }
    }
    if (course.days[3]) {
        if (first) {
            days += "Thu"
            first = false
        } else {
            days += ", Thu"
        }
    }
    if (course.days[4]) {
        if (first) {
            days += "Fri"
        } else {
            days += ", Fri"
        }
    }
    return days
}

fun updateKarma(url: String, karma: Int, callback: (JSONObject) -> Unit) {
    // Create JSONObject to send
    val jsonObject = JSONObject()
    jsonObject.put("sub", LoginActivity.GoogleIdTokenSub)
    jsonObject.put("karma", karma)

    // Create RequestBody and Request for OkHttp3
    val body = RequestBody.create(ApiService.JSON, jsonObject.toString())
    val request = Request.Builder().url(url).put(body).build()

    // Make call
    ApiService.client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.d("ClassInfoActivity", "Error: $e")
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

fun coordinatesToDistance(
    coord1: Pair<Double?, Double?>,
    coord2: Pair<Double?, Double?>
): Double {
    val r = 6378.137 // Radius of Earth in km
    val lat1 = coord1.first
    val lon1 = coord1.second
    val lat2 = coord2.first
    val lon2 = coord2.second
    if (lat1 == null) {
        return Double.MAX_VALUE
    }
    if (lon1 == null) {
        return Double.MAX_VALUE
    }
    if (lat2 == null) {
        return Double.MAX_VALUE
    }
    if (lon2 == null) {
        return Double.MAX_VALUE
    }

    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    val distance = r * c * 1000 // Convert km to meters

    Log.d("ClassInfoActivity", "Distance from you to the class: $distance")

    return distance
}

fun getAttendance(url: String, callback: (JSONObject) -> Unit) {
    // Create GET request for OkHttp3
    val request = Request.Builder().url(url).get().build()

    // Make call
    ApiService.client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.d(TAG, "Error: $e")
        }

        override fun onResponse(call: Call, response: Response) {
            val result = response.body()?.string()
            if (result != null) {
                try {
                    val jsonObject = JSONObject()
                    jsonObject.put("attended", JSONObject(result).getBoolean("attended"))
                    callback(jsonObject)
                } catch (_: Exception) {
                    val badJsonObject = JSONObject()
                    callback(badJsonObject)
                }
            }
        }
    })
}

fun updateAttendance(
    url: String,
    className: String,
    classFormat: String,
    callback: (JSONObject) -> Unit
) {
    // Create JSONObject to send
    val jsonObject = JSONObject()
    jsonObject.put("sub", LoginActivity.GoogleIdTokenSub)
    jsonObject.put("className", className)
    jsonObject.put("classFormat", classFormat)
    jsonObject.put("term", ScheduleListActivity.term)

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

// use the Place Autocomplete API to search for places based on the acronym
private fun findLatLngFromPlaceId(acronym: String, placesClient: PlacesClient, callback: (String?) -> Unit) {
    val request = FindAutocompletePredictionsRequest.builder()
        .setQuery("UBC " + acronym)
        .build()

    placesClient.findAutocompletePredictions(request)
        .addOnSuccessListener { response ->
            val predictions = response.autocompletePredictions
            if (predictions.isNotEmpty()) {
                val placeId = predictions[0].placeId
                callback(placeId)
            } else {
                callback(null) // no predictions found
            }
        }
        .addOnFailureListener { exception ->
            exception.printStackTrace()
            callback(null) // error occurred
        }
}

private fun getCurrentTime(): String {
    val currentTime = LocalDateTime.now()
    val dayOfWeek = currentTime.dayOfWeek.value // 1 = Monday, ..., 7 = Sunday
    val formatter = DateTimeFormatter.ofPattern("HH mm")
    val current_time = "$dayOfWeek ${currentTime.format(formatter)}"

    Log.d("ClassInfoActivity", "getCurrentTime: $current_time")
    return current_time
}

fun Pair<Int, Int>.to12HourTime(end: Boolean): String {
    var (hour, minute) = this
    if (end) {
        if (minute == 30) minute = 20
        else {
            minute = 50
            hour--
        }
    }
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = when (hour % 12) {
        0 -> 12  // 12-hour format should show 12 instead of 0 for AM/PM
        else -> hour % 12
    }
    return String.format("%d:%02d %s", hour12, minute, amPm)
}

private suspend fun requestCurrentLocation(context: Activity): Pair<Double?, Double?> {
    return if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        == PackageManager.PERMISSION_GRANTED
    ) {
        getLastLocation(context)
    } else {
        ActivityCompat.requestPermissions(
            context,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
        Log.d(TAG, "requestCurrentLocation: Permission requested, returning null until granted")
        Pair(null, null) // Cannot proceed until user grants permission
    }
}

private suspend fun getLastLocation(context: Context): Pair<Double?, Double?> {
    return if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        try {
            val location: Location

            // call getCurrentLocation() for the first time, and use the updated location afterwards
            if (isOnCreate) {
                val cancellationTokenSource = CancellationTokenSource()
                // request the current location with high accuracy
                location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).await()
                isOnCreate = false
            } else {
                location = Location("gps")
                location.latitude = current_location?.first!!
                location.longitude = current_location?.second!!
            }

            val latitude = location.latitude
            val longitude = location.longitude
            Log.d(TAG, "getLastLocation: lastLocation is ($latitude, $longitude)")
            Pair(latitude, longitude)

        } catch (e: SecurityException) {
            Log.e(TAG, "getLastLocation: Location permission not granted", e)
            Pair(null, null)
        } catch (e: IllegalStateException) {
            Log.e(
                TAG,
                "getLastLocation: Illegal state encountered while retrieving location",
                e
            )
            Pair(null, null)
        } catch (e: IOException) {
            Log.e(TAG, "getLastLocation: IO error while retrieving location", e)
            Pair(null, null)
        }
    } else {
        Log.d(TAG, "getLastLocation: Permission denied")
        Pair(null, null)
    }
}