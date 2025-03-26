package com.example.get2class

import android.util.Log
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.util.TreeIterables
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertNotNull
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.action.ViewActions.*
import androidx.test.uiautomator.UiObjectNotFoundException
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters


private const val NAME = "Lucas"
private const val FILENAME = "View_My_Courses.xlsx"
private const val LAG: Long = 8000
private const val TAG = "E2EEspressoTest"

/**
 * Instrumented test, which will execute on the emulated Pixel 9.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class E2EEspressoTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Test
    fun t1_uploadScheduleTest() {
        // Log in and navigate to schedules
        onView(withId(R.id.login_button)).perform(click())
        waitForUIClick(NAME)
        try{
            waitForUIClick("Agree and share", 2000)
        }catch (_: AssertionError){}
        waitForUIClick("Schedules")
        Log.d(TAG, "Test 1: Successfully log in and navigate to the schedule list!")

        // Test that uploading to the wrong term returns an empty schedule
        onView(withId(R.id.fall_schedule)).perform(click())
        onView(withId(R.id.upload_schedule_button)).perform(click())
        waitForUIClick(FILENAME)
        testScheduleLoaded(false)
        onView(withText("This schedule is not for this term!")).check(matches(isDisplayed()))
        Log.d(TAG, "Test 1: Successfully get an empty schedule after uploading a winter schedule to the fall schedule!")

        pressBack()

        // Test that uploading to the right term works
        onView(withId(R.id.winter_schedule)).perform(click())
        onView(withId(R.id.upload_schedule_button)).perform(click())
        waitForUI(FILENAME)
        val t1  = System.currentTimeMillis()
        ui_click(FILENAME)
        waitForUI("CPSC 320")
        testScheduleLoaded(true)
        onView(withText("Successfully uploaded schedule!")).check(matches(isDisplayed()))
        val t2  = System.currentTimeMillis()
        assertTrue("Schedule upload took more than 4s", t2 - t1 < 4000)
        Log.d(TAG, "Test 1: Successfully upload a winter schedule in ${t2 - t1}ms!")

        pressBack()

        // Test that the schedule is loaded when returning to the page
        onView(withId(R.id.winter_schedule)).perform(click())
        testScheduleLoaded(true)
        Log.d(TAG, "Test 1: Successfully load the winter schedule after pressing back and clicking Winter Schedule again!")

        // Test that clearing the schedule works
        onView(withId(R.id.clear_schedule_button)).perform(click())
        testScheduleLoaded(false)
        onView(withText("Successfully cleared schedule!")).check(matches(isDisplayed()))
        Log.d(TAG, "Test 1: Successfully clear the winter schedule!")
    }
    
    @Test
    fun t2_attendanceTest() {
        // Log in and navigate to winter schedule
        onView(withId(R.id.login_button)).perform(click())
        waitForUIClick(NAME)
        try{
            waitForUIClick("Agree and share", 2000)
        }catch (_: AssertionError){}
        waitForUIClick("Schedules")
        onView(withId(R.id.winter_schedule)).perform(click())

        // Upload the schedule
        onView(withId(R.id.upload_schedule_button)).perform(click())
        waitForUIClick(FILENAME)

        // Select CPSC 320
        onView(withIndex(withText("CPSC 320"), 0)).perform(click())

        // Case where year is wrong
        incrementMonth(-3, 1)
        onView(withId(R.id.check_attendance_button)).perform(click())
        onView(withText("You don't have this class this year")).check(matches(isDisplayed()))
        Log.d(TAG, "Test 2: Successfully detect that the user tries to check in in a wrong year!")

        // Case where term is wrong
        incrementMonth(5, 1)
        onView(withId(R.id.check_attendance_button)).perform(click())
        onView(withText("You don't have this class this term")).check(matches(isDisplayed()))
        Log.d(TAG, "Test 2: Successfully detect that the user tries to check in in a wrong term!")

        // Case where day is wrong
        incrementMonth(-2, 4)
        onView(withId(R.id.check_attendance_button)).perform(click())
        onView(withText("You don't have this class today")).check(matches(isDisplayed()))
        Log.d(TAG, "Test 2: Successfully detect that the user tries to check in in a wrong day!")

        // Case where too early
        incrementMonth(0, 10)
        setTime(9, 45, "AM")
        onView(withId(R.id.check_attendance_button)).perform(click())
        onView(withText("You are too early to check into this class!")).check(matches(isDisplayed()))
        Log.d(TAG, "Test 2: Successfully detect that the user tries to check in when it is too early!")

        // Case where too late
        setTime(10, 55, "AM")
        onView(withId(R.id.check_attendance_button)).perform(click())
        onView(withText("You missed your class!")).check(matches(isDisplayed()))
        Log.d(TAG, "Test 2: Successfully detect that the user tries to check in when it is too late!")

        // Case where everything is right
        setTime(9, 55, "AM")
        onView(withId(R.id.check_attendance_button)).perform(click())
        grantLocationPermissions()
        waitForUI("You gained 60 Karma!")
        Log.d(TAG, "Test 2: Successfully check the user in and award appropriate amount of points when everything is right!")

        // Case where you've already signed in
        onView(withId(R.id.check_attendance_button)).perform(click())
        onView(withText("You already checked into this class today!")).check(matches(isDisplayed()))
        Log.d(TAG, "Test 2: Successfully detect that the user tries to check in more than once!")

        pressBack()

        // Case where you were late
        onView(withIndex(withText("CPEN 321"), 1)).perform(click())
        setTime(3, 55, "PM")
        val t1  = System.currentTimeMillis()
        onView(withId(R.id.check_attendance_button)).perform(click())
        waitForUI("You were late by 24 minutes!")
        val t2  = System.currentTimeMillis()
        assertTrue("Attendance check took more than 4s", t2 - t1 < 4000)
        waitForUI("You gained 34 Karma!")
        pressBack()
        Log.d(TAG, "Test 2: Successfully check the user in when they are late and award appropriate amount of points in ${t2 - t1}ms!")

        // Case where location is wrong
        onView(withIndex(withText("CPSC 322"), 0)).perform(click())
        incrementMonth(0, 11)
        setTime(4, 55, "PM")
        onView(withId(R.id.check_attendance_button)).perform(click())
        waitForUI("You're too far from your class!")
        Log.d(TAG, "Test 2: Successfully detect that the user tries to check in when they are too far away from the class!")
    }

    @Test fun t3_viewRouteTest(){
        logInAndLoadWinterSchedule()
        Log.d(TAG, "Test 3: Successfully log in and upload a winter schedule!")

        // 1. The user clicks on View Route
        waitForUIClick("CPEN 321")
        waitForUIClick("View route to class")
        Log.d(TAG, "Test 3: Successfully click CPEN 321 and the button labelled \"View route to class\"!")

        // 2. The app prompts the user to grant location permissions if not already granted
        try{
            waitForUI("While using the app", 5000)

            Log.d(TAG, "Test 3: Successfully detect the permission request dialog!")

            // 2a. The user does not grant location permissions
            ui_click("Don’t allow")
            Log.d(TAG, "Test 3: Successfully deny the permission request!")

            // 2a1. If the user denies, the app shows a toast to tell the user to enable location permissions in the settings first
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val e = device.findObject(UiSelector().text("Please grant Location permissions in Settings to view your routes :/"))
            assertNotNull("Toast should show up", e)
            Log.d(TAG, "Test 3: Successfully warn the user to enable location permissions!")

            // 2a2. The app routes the user back to the previous screen
            onView(withId(R.id.route_button)).check(matches(isDisplayed()))
            onView(withId(R.id.check_attendance_button)).check(matches(isDisplayed()))
            Log.d(TAG, "Test 3: Successfully go back to the CPEN 321 info screen!")

            // retry step 1 for the success scenario
            ui_click("View route to class")
            Log.d(TAG, "Test 3: Successfully click the button labelled \"View route to class\" again!")

            // retry step 2
            waitForUIClick("Only this time")
            Thread.sleep(5000)
            Log.d(TAG, "Test 3: Successfully pop up the request dialog again and grant location permissions!")
        } catch (_: AssertionError){}

        // a navigation dialog will show up if this is the first run
        try {
            waitForUIClick("GOT IT", 10000)
            Log.d(TAG, "Test 3: Successfully agree on Google Maps navigation terms and conditions!")
        } catch (_: AssertionError) {}

        // 3. The user sees their current location and destination location together with the optimal route on the screen
        waitForUI("", 10000, R.id.navigation_view)
        Thread.sleep(LAG)
        onView(withId(R.id.navigation_view)).perform(swipeUp())
        onView(withId(R.id.navigation_view)).perform(swipeRight())
        waitForUIClick("Re-center", 5000)
        Thread.sleep(2000)
        pressBack()
        Log.d(TAG, "Test 3: Successfully see, swiping and re-centering the navigation view!")
    }
}

// Waits for expectedText to appear, then clicks on it
private fun waitForUIClick(expectedText: String, timeout: Long = LAG) {
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < timeout) {
        try {
            ui_click(expectedText)
            return // Exit if click succeeded
        } catch (e: AssertionError) {
            Thread.sleep(100) // Wait and retry
        } catch (e: UiObjectNotFoundException){
            Thread.sleep(100) // Wait and retry
        }
    }
    throw AssertionError("Text was not found within timeout")
}

// Waits for expectedText to appear
private fun waitForUI(expectedText: String, timeout: Long = LAG, expectedId: Int? = null) {
    val startTime = System.currentTimeMillis()
    while (System.currentTimeMillis() - startTime < timeout) {
        if(expectedId != null){
            try {
                onView(withId(expectedId)).check(matches(isDisplayed()))
                return
            } catch (e: NoMatchingViewException) {
                Thread.sleep(100)
            }
        }else{
            if (uiExistWithText(expectedText)) {
                return // Exit if found
            } else {
                Thread.sleep(100) // Wait and retry
            }
        }
    }
    throw AssertionError("Text was not found within timeout")
}

// Check if expectedText exists
private fun uiExistWithText(expectedText: String): Boolean{
    // get UI element with the given text
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val e = device.findObject(UiSelector().text(expectedText))

    return e.exists()
}

private fun logInAndLoadWinterSchedule(){
    // log in
    onView(withId(R.id.login_button)).perform(click())
    waitForUIClick(NAME)
    try{
        waitForUIClick("Agree and share", 2000)
    }catch (_: AssertionError){}
    waitForUIClick("Schedules")

    // upload schedule
    onView(withId(R.id.winter_schedule)).perform(click())
    onView(withId(R.id.upload_schedule_button)).perform(click())
    waitForUIClick(FILENAME)
}

// Use UIAutomator to click on the file in the system file picker
private fun ui_click(elementText: String) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val element = device.findObject(UiSelector().text(elementText))
    Log.d(
        "ui_click on $elementText",
        "ui_click: exist - ${element.exists()}; enable - ${element.isEnabled}"
    )
    if (element.exists() && element.isEnabled) {
        element.click()
    } else {
        throw AssertionError("Element $elementText not found in file picker")
    }
}

private fun countOccurrences(text: String, expectedCount: Int) {
    var actualCount = 0
    try {
        // Iterate over all views on the screen
        onView(isRoot()).check { view, _ ->
            // Use TreeIterables to get all views in the hierarchy
            val views = TreeIterables.breadthFirstViewTraversal(view)
            actualCount = views.count { it is android.widget.TextView && it.text == text }
        }
    } catch (e: NoMatchingViewException) {
        actualCount = 0 // No matching views found
    }

    // Compare the count and throw an error if it doesn't match the expected count
    if (actualCount != expectedCount) {
        throw AssertionError("Expected '$text' to appear $expectedCount times, but found $actualCount")
    }
}

private fun testScheduleLoaded(loaded: Boolean) {
    if (loaded) {
        countOccurrences("CPSC 320", 4)
        countOccurrences("CPEN 321", 3)
        countOccurrences("CPSC 322", 2)
        countOccurrences("Lecture", 7)
        countOccurrences("Laboratory", 1)
        countOccurrences("Discussion", 1)
        onView(withText("APSC 450")).check(doesNotExist())
    } else {
        onView(withText("CPSC 320")).check(doesNotExist())
        onView(withText("CPEN 321")).check(doesNotExist())
        onView(withText("CPSC 322")).check(doesNotExist())
        onView(withText("APSC 450")).check(doesNotExist())
        onView(withText("Lecture")).check(doesNotExist())
        onView(withText("Laboratory")).check(doesNotExist())
        onView(withText("Discussion")).check(doesNotExist())
    }
}

// Custom Matcher
private fun withIndex(matcher: Matcher<View>, index: Int): Matcher<View> {
    return object : TypeSafeMatcher<View>() {
        var currentIndex = 0
        override fun describeTo(description: Description) {
            description.appendText("with index: $index ")
            matcher.describeTo(description)
        }

        override fun matchesSafely(view: View): Boolean {
            return matcher.matches(view) && currentIndex++ == index
        }
    }
}

private fun incrementMonth(months: Int, day: Int) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.pressHome() // Ensure we're starting from the home screen
    device.findObject(UiSelector().text("Settings")).click()

    // Navigate to "Date & time"
    for (i in 0..5) {
        try {
            waitForUIClick("System", 1000)
            break
        } catch (e: AssertionError) {
            device.swipe(
                device.displayWidth / 2,      // Start X: middle of screen
                device.displayHeight * 3 / 4, // Start Y: 3/4 down the screen
                device.displayWidth / 2,      // End X: same horizontal position
                device.displayHeight / 4,     // End Y: 1/4 down the screen
                4                           // Steps: controls speed/smoothness
            )
        }
    }

    device.findObject(UiSelector().text("Date & time")).click()
    device.findObject(UiSelector().text("Date")).click()
    if (months < 0) {
        val leftArrow = device.findObject(UiSelector().resourceId("android:id/prev"))
        for (i in 1..-months) {
            leftArrow.click()
        }
        device.findObject(UiSelector().text(day.toString())).click()
        device.findObject(UiSelector().text("OK")).click()
    } else {
        val rightArrow = device.findObject(UiSelector().resourceId("android:id/next"))
        for (i in 1..months) {
            rightArrow.click()
        }
        device.findObject(UiSelector().text(day.toString())).click()
        device.findObject(UiSelector().text("OK")).click()
    }

    device.pressRecentApps()
    device.pressRecentApps()
    waitForUI("Check in to class")
}

private fun setTime(hour: Int, minute: Int, specifier: String) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    device.pressHome() // Ensure we're starting from the home screen
    device.findObject(UiSelector().text("Settings")).click()

    // Navigate to "Date & time"
    for (i in 0..5) {
        try {
            waitForUIClick("System", 1000)
            break
        } catch (e: AssertionError) {
            device.swipe(
                device.displayWidth / 2,      // Start X: middle of screen
                device.displayHeight * 3 / 4, // Start Y: 3/4 down the screen
                device.displayWidth / 2,      // End X: same horizontal position
                device.displayHeight / 4,     // End Y: 1/4 down the screen
                4                           // Steps: controls speed/smoothness
            )
        }
    }
    device.findObject(UiSelector().text("Date & time")).click()
    device.findObject(UiSelector().text("Time")).click()
    Thread.sleep(250)
    Log.d("test", "setting time")
    if (hour == 9) {
        device.click(300, 1261)
    } else if (hour == 10) {
        device.click(334, 1139)
    } else if (hour == 3) {
        device.click(780, 1255)
    } else if (hour == 4) {
        device.click(736, 1377)
    }
    Thread.sleep(250)
    if (minute == 45) {
        device.click(300, 1261)
    } else if (minute == 55) {
        device.click(419, 1057)
    } else if (minute == 25) {
        device.click(658, 1465)
    }

    device.findObject(UiSelector().text(specifier)).click()
    device.findObject(UiSelector().text("OK")).click()

    device.pressRecentApps()
    device.pressRecentApps()
    waitForUI("Check in to class")
}

private fun grantLocationPermissions() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    try {
        waitForUI("Only this time", 4000)
        device.findObject(UiSelector().text("Only this time")).click()
        onView(withId(R.id.check_attendance_button)).perform(click())
    } catch (e: AssertionError) {
        // Pass
    }
}
