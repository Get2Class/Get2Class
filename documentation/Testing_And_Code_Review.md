# Example M5: Testing and Code Review

## 1. Change History

| **Change Date**   | **Modified Sections** | **Rationale** |
| ----------------- | --------------------- | ------------- |
| March 30, 2025 | 2.1.1 | Added tests for deducting karma due to attendance |
| March 30, 2025 | 2.3 | Updated jest coverage image with deduct karma tests |
| March 30, 2025 | 2.4 | Updated jest coverage for unmocked tests with deduct karma test |
| March 31, 2025 | 5.2 | Fixed codacy issues so updated the issues overview screenshot |
| March 31, 2025 | 5.3 | Fixed codacy issues so updated the issues breakdown screenshot |
| March 31, 2025 | 5.4 | Updated to be N/A because there are no more issues |
| March 31, 2025 | 2.1.1 | Update Mocked Components to be more specific |
| April 2, 2025 | 3.1 | NFR locations changed due to frontend tests being split into 3 files |
| April 2, 2025 | 3.2 | Clarified how NFR tests check requirement |
| April 2, 2025 | 4.1 | Updated test instructions to reflect split into 3 files |

---

## 2. Back-end Test Specification: APIs

### 2.1. Locations of Back-end Tests and Instructions to Run Them

#### 2.1.1. Tests

| **Interface**               | **Describe Group Location, No Mocks**                | **Describe Group Location, With Mocks**            | **Mocked Components**              |
| ----------------------------- | ---------------------------------------------------- | -------------------------------------------------- | ---------------------------------- |
| **POST /user** | [`backend/tests/unmocked_tests/CreateUser.test.ts#L25`](#) | [`backend/tests/mocked_tests/CreateUser.test.ts#L26`](#) | User Database client |
| **GET /user** | [`backend/tests/unmocked_tests/GetUser.test.ts#L31`](#) | [`backend/tests/mocked_tests/GetUser.test.ts#L33`](#) | User Database client |
| **POST /tokensignin** | [`N/A`](#) | [`backend/tests/unmocked_tests/GoogleSignIn.test.ts#L27`](#) | Google-Authenticator |
| **PUT /karma** | [`backend/tests/unmocked_tests/UpdateKarma.test.ts#L31`](#) | [`backend/tests/mocked_tests/UpdateKarma.test.ts#L32`](#) | User Database client |
| **GET /notification_settings** | [`backend/tests/unmocked_tests/UserNotification.test.ts#L31`](#) | [`backend/tests/mocked_tests/UserNotification.test.ts#L32`](#) | User Database client |
| **PUT /notification_settings** | [`backend/tests/unmocked_tests/UserNotification.test.ts#L80`](#) | [`backend/tests/mocked_tests/UserNotification.test.ts#L84`](#) | User Database client |
| **DELETE /schedule** | [`backend/tests/unmocked_tests/DeleteSchedule.test.ts#L27`](#) | [`backend/tests/mocked_tests/DeleteSchedule.test.ts#L29`](#) | Schedule Database client |
| **GET /schedule** | [`backend/tests/unmocked_tests/GetSchedule.test.ts#L29`](#) | [`backend/tests/mocked_tests/GetSchedule.test.ts#L28`](#) | Schedule Database client |
| **PUT /schedule** | [`backend/tests/unmocked_tests/PutSchedule.test.ts#L25`](#) | [`backend/tests/mocked_tests/PutSchedule.test.ts#L25`](#) | Schedule Database client |
| **GET /attendance** | [`backend/tests/unmocked_tests/GetAttendance.test.ts#L27`](#) | [`backend/tests/mocked_tests/GetAttendance.test.ts#L28`](#) | Schedule Database client |
| **PUT /attendance** | [`backend/tests/unmocked_tests/PutAttendance.test.ts#L27`](#) | [`backend/tests/mocked_tests/PutAttendance.test.ts#L28`](#) | Schedule Database client |
| **Reset Attendance** | [`backend/tests/unmocked_tests/ResetAttendance.test.ts#L70`](#) | [`backend/tests/mocked_tests/ResetAttendance.test.ts#L70`](#) | Cron scheduler & Schedule Database client |
| **Deduct Karma** | [`backend/tests/unmocked_tests/DeductKarma.test.ts#L88`](#) | [`backend/tests/mocked_tests/DeductKarma.test.ts#L71`](#) | Cron scheduler & Schedule Database client & User Database client |

#### 2.1.2. Commit Hash Where Tests Run

`[d662caa9d35faf55eab29a2d8ed0833e54281c36]`

#### 2.1.3. Explanation on How to Run the Tests

1. **Clone the Repository**:

  - Open your terminal and run (if you are using https):
    ```
    git clone https://github.com/Get2Class/Get2Class.git
    ```

  - Or if you are using ssh:
    ```
    git clone git@github.com:Get2Class/Get2Class.git
    ```

2. **Checkout to `main` branch**:

  - In the terminal, after you have cloned the repository, checkout to `main` branch with the following command:
    ```
    git checkout main
    ```

3. **Change Directory to `backend`**:
  
  - In the terminal you will change directory to `backend`:
    ```
    cd backend
    ```

4. **Install Dependencies**:

  - In the terminal you will run install the dependencies by making sure you are in the `backend` directory and you run:
    ```
    npm i
    ```

5. **Setting Up the Database**:

  - Next ensure that you have your mongodb service running
  - Use MongoDB Compass to connect to your local database
  - Create a new database with the name `get2class` (needs to be exactly like this)
  - Then you will add two collections under this database: `users` and `schedules`

  - You should have something that looks like this:

    ![MongoDB Setup](./images/mongodb-setup.png)

6. **Running the Tests**:
  - In the terminal you will change directory to `tests` where the mocked and unmocked tests are located:
    ```
    cd tests
    npm test
    ```

7. **You can run the mocked and unmocked tests with the following commands below (Optional)**:
  - In the terminal, make you sure you're in the `tests` directory:
    - For `unmocked_tests`:
      ```
      npm test ./unmocked_tests/
      ```
    - For `mocked_tests`:
      ```
      npm test ./mocked_tests/
      ```

### 2.2. GitHub Actions Configuration Location

`~/.github/workflows/backend-tests.yml`

### 2.3. Jest Coverage Report Screenshots With Mocks

![Full Jest Coverage w/ Mocks](./images/jest-coverage-w-mocks-v2.png)

- #### Reason for uncovered lines in `index.ts`
  - These lines have to do with Promise errors within the server set up process not getting triggered (https://piazza.com/class/m5abcyzl23d5or/post/176).

- #### Reason for uncovered lines in `services.ts`
  - This is part of the back end tutorial and has to deal with the MongoDB Client set up logic (https://piazza.com/class/m5abcyzl23d5or/post/176).

### 2.4. Jest Coverage Report Screenshots Without Mocks

![Jest Coverage w/o Mocks](./images/jest-coverage-wo-mocks-v2.png)

- #### Reason for uncovered lines in `UserController.ts`
  - This is the `/tokensignin` route which mocks the Google OAuth2Client which gets handled within the mocked_tests

- #### Reason for uncovered lines in `index.ts`
  - This gets handled by the mocked_tests in section 2.3

---

## 3. Tests of Non-Functional Requirements

### 3.1. Test Locations in Git

| **Non-Functional Requirement**  | **Location in Git**                              |
| ------------------------------- | ----------------------------- |
| **Schedule Upload Time** | [`frontend/app/src/androidTest/java/com/example/get2class/UploadScheduleTest.kt#L77`](#) |
| **Attendance Check Time** | [`frontend/app/src/androidTest/java/com/example/get2class/AttendanceTest.kt#L124`](#) |

We directly integrated these tests into our frontend tests.<br>Instructions for running them can be found in section 4.1.

### 3.2. Test Verification and Logs

- **Schedule Usability**

  - **Verification:** This test simulates a user uploading their schedule from an xlsx file in their phone's downloads. The focus is on ensuring that the process of uploading the file, parsing it, storing it on the database, and rendering it for the user completes within the target response time of 4 seconds under normal load. The test flow is: note the current time, tap on the file to upload it, wait for the UI to finish loading, check the time again, and assert that the difference in the times was less than 4 seconds, thus failing the test if it does not meet the requirement.
  - **Log Output**
    ```
    Test 1: Successfully upload a winter schedule in 1762ms!
    ```

- **Check-in Usability**
  - **Verification:** This test simulates a user clicking on the "Check in to class" button with the help of Espresso. The focus is to ensure that the process of checking the time and location of the user, checking the starting time and location of the next class, calculating, updating and showing the Karma points the user gains completes within the target response time of 4 seconds under normal load. The test flow is: note the current time, tap on the check in to class button, wait for the success message, check the time again, and assert that the difference in the times was less than 4 seconds, thus failing the test if it does not meet the requirement.
  - **Log Output**
    ```
    Test 2: Successfully check the user in when they are late and award appropriate amount of points in 481ms!
    ```

---

## 4. Front-end Test Specification

### 4.1. Location in Git of Front-end Test Suite:

`frontend/app/src/androidTest/java/com/example/get2class/AttendanceTest.kt`
`frontend/app/src/androidTest/java/com/example/get2class/UploadScheduleTest.kt`
`frontend/app/src/androidTest/java/com/example/get2class/ViewRouteTest.kt`

#### Explanation on How to Run the Tests

1. Read get2class/frontend/README.md to set up the frontend.
2. Next, set up the back end and run it, to do this read get2class/backend/README.md
3. Additionally, on the Android Studio frontend, visit the frontend/app/src/main/AndroidManifest.xml and ensure that `android:usesCleartextTraffic="true"`
4. Make sure the settings app is on your emulated device's home screen.
5. In the device's settings, disable "Set time automatically" and, if needed, set to the date to April 2025.
6. Turn off Window animation scale, Transition animation scale, and Animator duration scale in the device's developer settings.
7. Copy the file get2class/documentation/View_My_Courses.xlsx to the device's downloads folder without changing its name.
8. Make sure when tapping upload in the app, it opens to the folder containing the file.
9. Open the test file.
10. Change the NAME variable to the name that shows up when you sign in with Google
11. Run the test. If it fails due to emulator lag, manually upload and clear a schedule then rerun the test. 


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
    ![Get2Class Use-Case Diagram](./images/frontend_t1_logs.png)

- **Use Case: Check Attendance**

  - **Expected Behaviors:**

    | **Scenario Steps** | **Test Case Steps** |
    | ------------------ | ------------------- |
    | 1. User clicks on the "Check in to class" button. | Log in and navigate to the class's info page. <br>Click on the button with ID check_attendance_button. |
    | 2. The user receives a toast telling them they got 60 Karma.<br>Note that for technical reasons, in the script this scenario is tested between 2e and 2f. | Change the phone's time to 9:55 AM<br>Click on the button with ID check_attendance_button.<br> Check that the message is visible. |
    | 2a. The class is not from this year. | Change the phone's year to 2024.<br>Click on the button with ID check_attendance_button. |
    | 2a1. The user receives a toast explaining the error. | Check that the message is visible. |
    | 2b. The class is not from this term. | Change the phone's month to May 2025.<br>Click on the button with ID check_attendance_button. |
    | 2b1. The user receives a toast explaining the error. | Check that the message is visible. |
    | 2c. The class is not on this day of the week. | Change the phone's day to Tuesday, March 4.<br>Click on the button with ID check_attendance_button. |
    | 2c1. The user receives a toast explaining the error. | Check that the message is visible. |
    | 2d. It's too early in the day to check in to the class. | Change the phone's day to Monday, March 10.<br>Change the phone's time to 9:45 AM<br>Click on the button with ID check_attendance_button. |
    | 2d1. The user receives a toast explaining the error. | Check that the message is visible. |
    | 2e. It's too late in the day to check in to the class. | Change the phone's time to 10:55 AM<br>Click on the button with ID check_attendance_button. |
    | 2e1. The user receives a toast explaining the error. | Check that the message is visible. |
    | 2f. The user already checked into class today. | Click on the button with ID check_attendance_button. |
    | 2f1. The user receives a toast explaining the error. | Check that the message is visible. |
    | 2g. The user went to class, but they were not on time. | Navigate to a different class.<br>Change the phone's time to 3:55 PM<br>Click on the button with ID check_attendance_button. |
    | 2g1. The user receives a toast telling them how late they were.<br>The user receives a toast telling them how much Karma they gained. | Check that the message is visible with the right amount of Karma. |
    | 2h. The user is in the wrong location. | Navigate to a different class.<br>Change the phone's time to 4:55 PM<br>Click on the button with ID check_attendance_button. |
    | 2h1. The user receives a toast explaining the error. | Check that the message is visible. |

  - **Test Logs:**
    ![Get2Class Use-Case Diagram](./images/frontend_t2_logs.png)

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
    ![Get2Class Use-Case Diagram](./images/frontend_t3_logs.png)

---

## 5. Automated Code Review Results

### 5.1. Commit Hash Where Codacy Ran

`[d662caa9d35faf55eab29a2d8ed0833e54281c36]`

### 5.2. Unfixed Issues per Codacy Category

![Issues Breakdown](./images/issues-breakdown-3.png)

### 5.3. Unfixed Issues per Codacy Code Pattern

![Issues Overview](./images/issues-overview-3.png)

### 5.4. Justifications for Unfixed Issues

- **Code Pattern: [N/A](#)**

  1. **Issue**

    - **Location in Git:** [`N/A`](#)
    - **Justification:** No issues