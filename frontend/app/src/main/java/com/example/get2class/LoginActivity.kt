package com.example.get2class

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
        var GoogleIdTokenCredentialName = ""
        var GoogleIdTokenCredentialEmail = ""
        var GoogleIdTokenSub = ""
        var credentialManager: CredentialManager? = null
    }

    private val activityScope = CoroutineScope(Dispatchers.Main)
    lateinit var mainView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        mainView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.login)

        findViewById<Button>(R.id.login_button).setOnClickListener {
            Log.d(TAG, "Login button clicked")

            Log.d(TAG, "WEB CLIENT ID: ${BuildConfig.WEB_CLIENT_ID}")

            credentialManager = CredentialManager.create(this)

            Log.d(TAG, "LoginActivity credentialManager: ${credentialManager}")

            val signInWithGoogleOption: GetSignInWithGoogleOption = GetSignInWithGoogleOption
                .Builder(BuildConfig.WEB_CLIENT_ID)
                .setNonce(generatedHashedNonce())
                .build()

            val request: GetCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            activityScope.launch {
                try {
                    val result = credentialManager!!.getCredential(
                        request = request,
                        context = this@LoginActivity
                    )
                    handleSignIn(result)
                } catch (e: GetCredentialException) {
                    handleFailure(e)
                }
            }
        }
    }

    fun verifyToken(url: String, idToken: String, webClientId: String, callback: (JSONObject) -> Unit) {
        // Create JSONObject to send
        val jsonObject = JSONObject()
        jsonObject.put("idToken", idToken)
        jsonObject.put("audience", webClientId)

        // Create RequestBody and Request for OkHttp3
        val body = RequestBody.create(ApiService.JSON, jsonObject.toString())
        val request = Request.Builder().url(url).post(body).build()

        // Make call
        ApiService.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "Error: $e")
                Snackbar.make(mainView, "Error verifying token", Snackbar.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body()?.string()
                if(result != null) {
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

    fun findExistingUser(url: String, sub: String, callback: (JSONObject) -> Unit) {
        // Create Request for OkHttp3
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
                        val jsonObject = JSONObject(result)
                        callback(jsonObject)
                    } catch(_: Exception) {
                        Log.d(TAG, "Creating new user")
                        createNewUser(BuildConfig.BASE_API_URL + "/user", sub) { result ->
                            callback(result)
                        }
                    }
                }
            }
        })
    }

    fun createNewUser(url: String, sub: String, callback: (JSONObject) -> Unit) {
        // Create JSONObject to send
        val jsonObject = JSONObject()
        jsonObject.put("email", GoogleIdTokenCredentialEmail)
        jsonObject.put("sub", sub)
        jsonObject.put("name", GoogleIdTokenCredentialName)

        // Create RequestBody and Request for OkHttp3
        val body = RequestBody.create(ApiService.JSON, jsonObject.toString())
        val request = Request.Builder().url(url).post(body).build()

        // Make call
        ApiService.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "Error: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.body()?.string()
                if(result != null) {
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

    private fun generatedHashedNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        // Handle the successfully returned credential.
        val credential = result.credential

        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract id to validate and
                        // authenticate on your server
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        Log.d(TAG, "Entire Google ID Token: ${googleIdTokenCredential.idToken}")

                        // Store Name and Email of user into this class
                        GoogleIdTokenCredentialName = googleIdTokenCredential.displayName.toString()
                        GoogleIdTokenCredentialEmail = googleIdTokenCredential.id

                        // Log the Name and Email of the user
                        Log.d(TAG, "GoogleIdTokenCredentialName: ${GoogleIdTokenCredentialName}")
                        Log.d(TAG, "GoogleIdTokenCredentialEmail: ${GoogleIdTokenCredentialEmail}")

                        // Verify the Google ID Token on the back end
                        verifyToken(BuildConfig.BASE_API_URL + "/tokensignin", googleIdTokenCredential.idToken, BuildConfig.WEB_CLIENT_ID) { result ->
                            Log.d(TAG, "Verifying Google ID Token")

                            GoogleIdTokenSub = result.getString("sub")

                            // Finds an existing user and if there does not exist a particular user, create one
                            findExistingUser(BuildConfig.BASE_API_URL + "/user?sub=" + result.getString("sub"), result.getString("sub")) { result ->
                                Log.d(TAG, "Finding existing user: ${result}")

                                val intent = Intent(this, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                startActivity(intent)
                                finish()
                            }
                        }

                        // Finds an existing user and creates one if existing user does not exist
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                    }
                }
                else {
                    // Catch any unrecognized credential type here.
                    Log.e(TAG, "Unexpected type of credential")
                }
            }
            else -> {
                // Catch any unrecognized credential type here.
                Log.e(TAG, "Unexpected type of credential")
            }
        }
    }

    private fun handleFailure(e: GetCredentialException) {
        Log.e(TAG, "Error getting credential", e)
        Snackbar.make(mainView, "Error getting credential", Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel()
    }
}