# Example M5: Testing and Code Review

## 1. Change History

| **Change Date**   | **Modified Sections** | **Rationale** |
| ----------------- | --------------------- | ------------- |
| _Nothing to show_ |

---

## 2. Back-end Test Specification: APIs

### 2.1. Locations of Back-end Tests and Instructions to Run Them

#### 2.1.1. Tests

| **Interface**                 | **Describe Group Location, No Mocks**                | **Describe Group Location, With Mocks**            | **Mocked Components**              |
| ----------------------------- | ---------------------------------------------------- | -------------------------------------------------- | ---------------------------------- |
| **POST /user/login**          | [`tests/unmocked/authenticationLogin.test.js#L1`](#) | [`tests/mocked/authenticationLogin.test.js#L1`](#) | Google Authentication API, User DB |
| **POST /study-groups/create** | ...                                                  | ...                                                | Study Group DB                     |
| ...                           | ...                                                  | ...                                                | ...                                |
| ...                           | ...                                                  | ...                                                | ...                                |

#### 2.1.2. Commit Hash Where Tests Run

`[Insert Commit SHA here]`

#### 2.1.3. Explanation on How to Run the Tests

1. **Clone the Repository**:

   - Open your terminal and run:
     ```
     git clone https://github.com/example/your-project.git
     ```

2. **...**

### 2.2. GitHub Actions Configuration Location

`~/.github/workflows/backend-tests.yml`

### 2.3. Jest Coverage Report Screenshots With Mocks

_(Placeholder for Jest coverage screenshot with mocks enabled)_

### 2.4. Jest Coverage Report Screenshots Without Mocks

_(Placeholder for Jest coverage screenshot without mocks)_

---

## 3. Tests of Non-Functional Requirements

### 3.1. Test Locations in Git

| **Non-Functional Requirement**  | **Location in Git**                              |
| ------------------------------- | ----------------------------- |
| **Schedule Upload Time** | [`frontend/app/src/androidTest/java/com/example/get2class/ExampleInstrumentedTest.kt:78`](#) |
| **Attendance Check Time** | [`frontend/app/src/androidTest/java/com/example/get2class/ExampleInstrumentedTest.kt:157`](#) |

### 3.2. Test Verification and Logs

- **Schedule Upload Time**

  - **Verification:** This test simulates a user uploading their schedule from an xlsx file in their phone's downloads. The focus is on ensuring that the process of uploading the file, parsing it, storing it on the database, and rendering it for the user completes within the target response time of 5 seconds under normal load. We use Espresso's onView().check() to ensure the timer does not stop until the component is displayed for the user. We use Espresso's onView().perform(click()) to ensure the timer does not stop until the component is displayed for the user. The test logs let us know if the system meets our requirement. 
  - **Log Output**
    ```
    Schedule upload passed in less than 5 seconds!
    ```

- **Attendance Check Time**
  - **Verification:** This test simulates a user clicking on the "Check in to class" button with the help of Espresso. The focus is to ensure that the process of checking the time and location of the user, checking the starting time and location of the next class, calculating, updating and showing the Karma points the user gains completes within the target response time of 5 seconds under normal load. We use Espresso's onView().perform(click()) and onView().check() to perform the click action and check if the user receives the response from the app. The test logs capture the processing time and let us know if the system meets our requirement.
  - **Log Output**
    ```
    Attendance check passed in less than 5 seconds!
    ```

---

## 4. Front-end Test Specification

### 4.1. Location in Git of Front-end Test Suite:

`frontend/src/androidTest/java/com/studygroupfinder/`

### 4.2. Tests

- **Use Case: Upload Schedule**

  - **Expected Behaviors:**
    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user chooses which schedule they want to set (Fall, Winter, Summer). | Log in and navigate to the schedule. |
    | 2. The user will then click on the Upload Schedule button. | Click on the button with ID upload_schedule_button. |
    | 4. A page reroute will occur requesting the user to upload the .xlsx file they got from Workday. | Click on the file titled "View_My_Courses.xlsx". |
    | 4a. The schedule was not for this term. | Check none of the classes were uploaded. |
    | 4a1. The user received a toast telling them to upload it to the correct term. | Check that the message is visible. |
    | 5. The schedule was properly uploaded. | Check that the classes display the right number of lectures, labs, and discussions. <br>Check classes without a meeting time are not displayed. |

  - **Test Logs:**
    ```
    2025-03-11 16:57:00.554  7783-7800  TestRunner              com.example.get2class                I  started: t1_uploadScheduleTest(com.example.get2class.E2EEspressoTest)
    2025-03-11 16:57:01.348  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 1: t1_uploadScheduleTest is starting ......
    2025-03-11 16:57:13.700  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 1: Successfully log in and navigate to the schedule list!
    2025-03-11 16:57:24.035  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 1: Successfully get an empty schedule after uploading a winter schedule to the fall schedule!
    2025-03-11 16:57:32.125  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 1: Successfully upload a winter schedule!
    2025-03-11 16:57:33.161  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 1: Successfully load the winter schedule after pressing back and clicking Winter Schedule again!
    2025-03-11 16:57:34.120  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 1: Successfully clear the winter schedule!
    2025-03-11 16:57:34.122  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 1: t1_uploadScheduleTest is finishing ......
    2025-03-11 16:57:34.127  7783-7800  TestRunner              com.example.get2class                I  finished: t1_uploadScheduleTest(com.example.get2class.E2EEspressoTest)
    ```

- **Use Case: Check Attendance**

  - **Expected Behaviors:**

    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. User clicks on the "Check in to class" button. | Log in and navigate to the class's info page. <br>Click on the button with ID check_attendance_button. |
    | 1a. The class is not from this year. | Change the phone's year to 2024.<br>Click on the button with ID check_attendance_button. |
    | 1a1. The user receives a toast explaining the error. | Check that the message is visible. |
    | 1b. The class is not from this term. | Change the phone's month to May 2025.<br>Click on the button with ID check_attendance_button. |
    | 1b1. The user receives a toast explaining the error. | Check that the message is visible. |
    | 1c. The class is not on this day of the week. | Change the phone's day to Tuesday, March 4.<br>Click on the button with ID check_attendance_button. |
    | 1c1. The user receives a toast explaining the error. | Check that the message is visible. |
    | 1d. It's too early in the day to check in to the class. | Change the phone's day to Monday, March 10.<br>Change the phone's time to 9:45 AM<br>Click on the button with ID check_attendance_button. |
    | 1d1. The user receives a toast explaining the error. | Check that the message is visible. |
    | 1e. It's too late in the day to check in to the class. | Change the phone's time to 10:55 AM<br>Click on the button with ID check_attendance_button. |
    | 1e1. The user receives a toast explaining the error. | Check that the message is visible. |
    | 1f. The user already checked into class today. | Click on the button with ID check_attendance_button. |
    | 1f1. The user receives a toast explaining the error. | Check that the message is visible. |
    | 1g. The user went to class, but they were not on time. | Navigate to a different class.<br>Change the phone's time to 3:55 PM<br>Click on the button with ID check_attendance_button. |
    | 1g1. The user receives a toast telling them how late they were.<br>The user receives a toast telling them how much Karma they gained. | Check that the message is visible with the right amount of Karma. |
    | 1h. The user is in the wrong location. | Navigate to a different class.<br>Change the phone's time to 4:55 PM<br>Click on the button with ID check_attendance_button. |
    | 1h1. The user receives a toast explaining the error. | Check that the message is visible. |
    | 2. The user was in their class on time. | Change the phone's time to 9:55 AM<br>Click on the button with ID check_attendance_button.<br>The user receives a toast telling them they got 60 Karma.<br> Check that the message is visible. |

  - **Test Logs:**
    ```
    2025-03-11 16:57:34.542  7783-7800  TestRunner              com.example.get2class                I  started: t2_attendanceTest(com.example.get2class.E2EEspressoTest)
    2025-03-11 16:57:34.794  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 2: t2_attendanceTest is starting ......
    2025-03-11 16:57:46.563  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 2: Successfully log in and navigate to the winter schedule!
    2025-03-11 16:57:52.596  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 2: Successfully upload a winter schedule!
    2025-03-11 16:57:53.229  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 2: Successfully find and click CPSC 320!
    2024-12-01 16:58:07.567  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 2: Successfully detect that the user tries to check in in a wrong year!
    2025-05-01 16:58:24.757  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 2: Successfully detect that the user tries to check in in a wrong term!
    2025-03-04 16:58:39.215  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 2: Successfully detect that the user tries to check in in a wrong day!
    2025-03-10 09:45:03.929  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 2: Successfully detect that the user tries to check in when it is too early!
    2025-03-10 10:55:04.018  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 2: Successfully detect that the user tries to check in when it is too late!
    2025-03-10 09:55:13.472  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 2: Successfully check the user in and award appropriate amount of points when everything is right!
    2025-03-10 09:55:13.767  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 2: Successfully detect that the user tries to check in more than once!
    2025-03-10 15:55:08.312  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 2: Successfully check the user in when they are late and award appropriate amount of points!
    2025-03-11 16:55:04.986  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 2: Successfully detect that the user tries to check in when they are too far away from the class!
    2025-03-11 16:55:04.986  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 2: t2_attendanceTest is finishing ......
    2025-03-11 16:55:04.996  7783-7800  TestRunner              com.example.get2class                I  finished: t2_attendanceTest(com.example.get2class.E2EEspressoTest)
    ```

- **Use Case: View Route To class**

  - **Expected Behaviors:**

    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. The user clicks on View Route. | Log in and navigate to the class's info page. <br> Click the button labelled "View route to class". |
    | 2. The app prompts the user to grant location permissions if not already granted. | Check if "While using the app" option from the permission request dialog is present on the screen. |
    | 2a. The user does not grant location permissions. | Click the option labelled "Don’t allow" in the dialog. |
    | 2a1. If the user denies, the app shows a toast to tell the user to enable location permissions in the settings first. | Check if the text "Please grant Location permissions in Settings to view your routes :/" is present on the screen. |
    | 2a2. The app routes the user back to the previous screen. | Check if the buttons labelled "View route to class" and "Check in to class" are present on the screen. |
    | 3. The user sees their current location and destination location together with the optimal route on the screen. | Check if the navigation layout is present on the screen. <br> Swipe the screen up and then to the right. <br> Check if the button labelled "Re-center" is present on the screen. <br> Click the button labelled "Re-center". |

  - **Test Logs:**
    ```
    2025-03-11 16:55:05.268  7783-7800  TestRunner              com.example.get2class                I  started: t3_viewRouteTest(com.example.get2class.E2EEspressoTest)
    2025-03-11 16:55:05.531  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 3: t3_viewRouteTest is starting ......
    2025-03-11 16:55:22.692  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 3: Successfully log in and upload a winter schedule!
    2025-03-11 16:55:29.493  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 3: Successfully click CPEN 321 and the button labelled "View route to class"!
    2025-03-11 16:55:30.038  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 3: Successfully agree on Google Maps navigation terms and conditions!
    2025-03-11 16:55:47.455  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 3: Successfully see, swiping and re-centering the navigation view!
    2025-03-11 16:55:47.455  7783-7800  E2EEspressoTest         com.example.get2class                D  Test 3: t3_viewRouteTest is finishing ......
    2025-03-11 16:55:47.460  7783-7800  TestRunner              com.example.get2class                I  finished: t3_viewRouteTest(com.example.get2class.E2EEspressoTest)

---

## 5. Automated Code Review Results

### 5.1. Commit Hash Where Codacy Ran

`[Insert Commit SHA here]`

### 5.2. Unfixed Issues per Codacy Category

_(Placeholder for screenshots of Codacyâ€™s Category Breakdown table in Overview)_

### 5.3. Unfixed Issues per Codacy Code Pattern

_(Placeholder for screenshots of Codacyâ€™s Issues page)_

### 5.4. Justifications for Unfixed Issues

- **Code Pattern: [Usage of Deprecated Modules](#)**

  1. **Issue**

     - **Location in Git:** [`src/services/chatService.js#L31`](#)
     - **Justification:** ...

  2. ...

- ...