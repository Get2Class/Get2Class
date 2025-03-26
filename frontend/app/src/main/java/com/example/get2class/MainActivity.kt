package com.example.get2class

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.ClearCredentialStateRequest
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val activityScope = CoroutineScope(Dispatchers.Main)
    lateinit var mainView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        mainView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)

        // Override Android back button to perform leaving app logic
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Exit?")
                    .setMessage("Are you sure you want to leave?")
                    .setPositiveButton("Yes") { _, _ -> finish() }
                    .setNegativeButton("No", null)
                    .show()
            }
        })

        val welcomeText: TextView = findViewById(R.id.welcome_text)
        welcomeText.text = "Welcome " + LoginActivity.GoogleIdTokenCredentialName + "!"

        findViewById<Button>(R.id.schedules_button).setOnClickListener() {
            Log.d(TAG, "Schedules button clicked")
            val intent = Intent(this@MainActivity, ScheduleListActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.karma_button).setOnClickListener() {
            Log.d(TAG, "Karma button clicked")
            val intent = Intent(this@MainActivity, KarmaActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.settings_button).setOnClickListener() {
            Log.d(TAG, "Settings button clicked")
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.sign_out_button).setOnClickListener() {
            Log.d(TAG, "Sign out button clicked")

            activityScope.launch {
                try {
                    // Log the user's credentialManager information before clearing
                    Log.d(TAG, "before clear: ${LoginActivity.credentialManager}")

                    // Clear user's credentialManager state
                    LoginActivity.credentialManager?.clearCredentialState(ClearCredentialStateRequest())
                    Snackbar.make(mainView, "Logged Out Successfully", Snackbar.LENGTH_SHORT).show()

                    // Log the user's credentialManager information after clearing
                    Log.d(TAG, "after clear: ${LoginActivity.credentialManager}")

                    // Route back to LoginActivity page
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                    finish()
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security error while clearing credential state", e)
                    Snackbar.make(mainView, "Security error occurred", Snackbar.LENGTH_SHORT).show()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Credential manager is in an invalid state", e)
                    Snackbar.make(mainView, "Invalid credential state", Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}