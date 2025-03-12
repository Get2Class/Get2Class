package com.example.get2class

import android.util.Log
import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.util.TreeIterables
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Rule
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.ViewAction
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.UiController
import androidx.test.espresso.PerformException

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HelloWorldEspressoTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Test fun uploadScheduleTest() {
        onView(withId(R.id.login_button)).perform(click())
        Thread.sleep(4000)
        ui_click("Lucas")
        Thread.sleep(4000)
        onView(withId(R.id.schedules_button)).perform(click())
        onView(withId(R.id.fall_schedule)).perform(click())
        onView(withId(R.id.upload_schedule_button)).perform(click())
        Thread.sleep(1000)
        ui_click("View_My_Courses.xlsx")
        onView(withText("CPSC 320")).check(doesNotExist())
        onView(withText("CPEN 321")).check(doesNotExist())
        onView(withText("CPSC 322")).check(doesNotExist())
        onView(withText("Lecture")).check(doesNotExist())
        onView(withText("Laboratory")).check(doesNotExist())
        onView(withText("Discussion")).check(doesNotExist())
        pressBack()

        onView(withId(R.id.winter_schedule)).perform(click())
        onView(withId(R.id.upload_schedule_button)).perform(click())
        Thread.sleep(3000)
        ui_click("View_My_Courses.xlsx")

        Thread.sleep(3000)

        // everything above this works

// Assuming you are using a RecyclerView or similar container
        // Usage example
        onView(withId(R.id.calendarRecyclerView))  // Replace with the actual RecyclerView ID
            .perform(countViewsWithIdAndClickable(R.id.cellFrame, true))

//        pressBack()
//        uploadDone = false
//        onView(withId(R.id.winter_schedule)).perform(click())
//        Thread.sleep(3000)
//        while(!uploadDone) {
//            Thread.sleep(1000)
//        }
//        onView(withText("CPSC 320")).check(matches(isDisplayed()))
//
//        // onView(withId(R.id.cellTextView)).check(matches(isDisplayed()))
//
//        countOccurrences("CPSC 320", 4)
    }
}

// Use UIAutomator to click on the file in the system file picker
private fun ui_click(elementText: String) {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val element = device.findObject(UiSelector().text(elementText))
    if (element.exists() && element.isEnabled) {
        element.click()
    } else {
        throw AssertionError("Element $elementText not found in file picker")
    }
}


fun countOccurrences(text: String, expectedCount: Int) {
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

fun countViewsWithIdAndClickable(id: Int, isClickable: Boolean): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return allOf(withId(id), isClickable())
        }

        override fun getDescription(): String {
            return "Count views with ID $id and clickable=$isClickable"
        }

        override fun perform(uiController: UiController, view: View?) {
            val recyclerView = view as RecyclerView
            val count = recyclerView.childCount
            var clickableCount = 0

            for (i in 0 until count) {
                val itemView = recyclerView.getChildAt(i)
                if (itemView.isClickable == isClickable) {
                    clickableCount++
                }
            }

            // Log the count or perform any assertion
            println("Number of clickable items: $clickableCount")
            // Optionally, you could throw an exception or assert here
        }
    }
}


