package com.example.get2class

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.datatransport.BuildConfig
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.navigation.NavigationApi
import com.google.android.libraries.navigation.NavigationView
import com.google.android.libraries.navigation.Navigator
import com.google.android.libraries.navigation.RoutingOptions
import com.google.android.libraries.navigation.SimulationOptions
import com.google.android.libraries.navigation.Waypoint
import com.google.android.libraries.places.api.Places

class RouteActivity : AppCompatActivity() {
    // reference to NavigationView
    private lateinit var navView: NavigationView

    // used for Navigation API
    private var mNavigator: Navigator? = null

    // listeners for reroute and arrival
    private var arrivalListener: Navigator.ArrivalListener? = null
    private var routeChangedListener: Navigator.RouteChangedListener? = null

    var navigatorScope: InitializedNavScope? = null
    var pendingNavActions = mutableListOf<InitializedNavRunnable>()

    // find a Place ID that will act as your destination. Ideally this will be not too far from the user location
    // use the Google Maps Platform Place ID Finder utility or obtain a Place ID from a Places API call
    companion object{
        var endLocation = "ChIJhefcLcpyhlQRtr9X4RoR2e4" // geography bldg
        var startLocation = LatLng(49.262043, -123.248253) // mcld bldg
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_route)

        // obtain reference to NavigationView
        navView = findViewById(R.id.navigation_view)
        navView.onCreate(savedInstanceState)

        // ensure the screen stays on during nav
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.navigation_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // retrieve permission status
        val permissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }

        // checks whether the user has granted fine location permission. If not, request the permission
        if (permissions.any { !checkPermissionGranted(it) }) {

            if (permissions.any { shouldShowRequestPermissionRationale(it) }) {
                // display a dialogue explaining the required permissions.
            }

            val permissionsLauncher =
                registerForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions(),
                    { permissionResults ->
                        if (permissionResults.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) {
                            onLocationPermissionGranted()
                        } else {
                            finish()
                        }
                    },
                )

            permissionsLauncher.launch(permissions)
        } else {
            Handler(Looper.getMainLooper()).postDelayed({ onLocationPermissionGranted() }, 2000)
        }
    }

    // requests directions from the user's current location to a specific place (provided by the Place ID)
    private fun navigateToPlace(placeId: String) {
        val waypoint: Waypoint? =
            // set a destination by using a Place ID (the recommended method)
            try {
                Waypoint.builder().setPlaceIdString(placeId).build()
            } catch (e: Waypoint.UnsupportedPlaceIdException) {
                showToast("Place ID was unsupported.")
                return
            }

        // set mode to walking
        val pendingRoute = mNavigator?.setDestination(waypoint, RoutingOptions().travelMode(RoutingOptions.TravelMode.WALKING))

        // set an action to perform when a route is determined to the destination
        pendingRoute?.setOnResultListener { code ->
            when (code) {
                Navigator.RouteStatus.OK -> {
                    // hide the toolbar to maximize the navigation UI
                    supportActionBar?.hide()

                    // enable voice audio guidance (through the device speaker)
                    mNavigator?.setAudioGuidance(Navigator.AudioGuidance.VOICE_ALERTS_AND_GUIDANCE)

                    // simulate vehicle progress along the route (for demo/debug builds)
                    if (BuildConfig.DEBUG) {
                        mNavigator?.simulator?.simulateLocationsAlongExistingRoute(
                            SimulationOptions().speedMultiplier(5f)
                        )
                    }
                }

                Navigator.RouteStatus.ROUTE_CANCELED -> showToast("Route guidance canceled.")
                Navigator.RouteStatus.NO_ROUTE_FOUND,
                Navigator.RouteStatus.NETWORK_ERROR ->
                    showToast("Error starting guidance: $code")

                else -> showToast("Error starting guidance: $code")
            }
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        navView.onSaveInstanceState(savedInstanceState)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        navView.onTrimMemory(level)
    }

    override fun onStart() {
        super.onStart()
        navView.onStart()
    }

    override fun onResume() {
        super.onResume()
        navView.onResume()
    }

    override fun onPause() {
        navView.onPause()
        super.onPause()
    }

    override fun onConfigurationChanged(configuration: Configuration) {
        super.onConfigurationChanged(configuration)
        navView.onConfigurationChanged(configuration)
    }

    override fun onStop() {
        navView.onStop()
        super.onStop()
    }

    override fun onDestroy() {
        navView.onDestroy()
        withNavigatorAsync {
            // unregister event listeners to avoid memory leaks.
            if (arrivalListener != null) {
                navigator.removeArrivalListener(arrivalListener)
            }
            if (routeChangedListener != null) {
                navigator.removeRouteChangedListener(routeChangedListener)
            }

            navigator.simulator?.unsetUserLocation()
            navigator.cleanup()
        }
        super.onDestroy()
    }

    // runs block once navigator is initialized. Block is ignored if the navigator is never initialized (error, etc.)
    // ensures that calls using the navigator before the navigator is initialized gets executed after the navigator has been initialized
    private fun withNavigatorAsync(block: InitializedNavRunnable) {
        val navigatorScope = navigatorScope
        if (navigatorScope != null) {
            navigatorScope.block()
        } else {
            pendingNavActions.add(block)
        }
    }

    // registers a number of example event listeners that show an on screen message when certain navigation events occur (e.g. the driver's route changes or the destination is reached)
    private fun registerNavigationListeners() {
        withNavigatorAsync {
            arrivalListener =
                Navigator.ArrivalListener {
                    // show an onscreen message
                    showToast("You have arrived at the destination!")
                    mNavigator?.clearDestinations()

                    // stop simulating vehicle movement
                    if (BuildConfig.DEBUG) {
                        mNavigator?.simulator?.unsetUserLocation()
                    }
                }
            mNavigator?.addArrivalListener(arrivalListener)

            routeChangedListener =
                Navigator.RouteChangedListener {
                    // show an onscreen message when the route changes
                    showToast("onRouteChanged: the user's route changed")
                }
            mNavigator?.addRouteChangedListener(routeChangedListener)
        }
    }

    // starts the Navigation API, capturing a reference when ready
    @SuppressLint("MissingPermission")
    private fun initializeNavigationApi() {
        NavigationApi.getNavigator(
            this,
            object : NavigationApi.NavigatorListener {
                override fun onNavigatorReady(navigator: Navigator) {
                    val scope = InitializedNavScope(navigator)
                    navigatorScope = scope
                    pendingNavActions.forEach { block -> scope.block() }
                    pendingNavActions.clear()

                    // disables the guidance notifications and shuts down the app and background service when the user dismisses/swipes away the app from Android's recent tasks
                    navigator.setTaskRemovedBehavior(Navigator.TaskRemovedBehavior.QUIT_SERVICE)

                    // store a reference to the Navigator object
                    mNavigator = navigator

                    // for testing only
                    mNavigator?.simulator?.setUserLocation(RouteActivity.startLocation)

                    // listen for events en route
                    registerNavigationListeners()

                    // specify a CameraPerspective
                    navView.getMapAsync {
                        googleMap -> googleMap.followMyLocation(GoogleMap.CameraPerspective.TILTED)
                    }

                    // delay navigation to ensure the navigator has the user location
                    android.os.Handler(Looper.getMainLooper()).postDelayed({
                        navigateToPlace(RouteActivity.endLocation)
                    }, 3000) // 3-second delay
                }

                override fun onError(@NavigationApi.ErrorCode errorCode: Int) {
                    when (errorCode) {
                        NavigationApi.ErrorCode.NOT_AUTHORIZED -> {
                            showToast(
                                "Error loading Navigation API: Your API key is " +
                                        "invalid or not authorized to use Navigation."
                            )
                        }
                        NavigationApi.ErrorCode.TERMS_NOT_ACCEPTED -> {
                            showToast(
                                "Error loading Navigation API: User did not " +
                                        "accept the Navigation Terms of Use."
                            )
                        }
                        else -> showToast("Error loading Navigation API: $errorCode")
                    }
                }
            },
        )

    }

    // show feedback to the user
    private fun showToast(errorMessage: String) {
        Toast.makeText(this@RouteActivity, errorMessage, Toast.LENGTH_LONG).show()
    }

    private fun onLocationPermissionGranted() {
        initializeNavigationApi()
    }

    private fun checkPermissionGranted(permissionToCheck: String): Boolean =
        ContextCompat.checkSelfPermission(this, permissionToCheck) == PackageManager.PERMISSION_GRANTED
}

open class InitializedNavScope(val navigator: Navigator)

typealias InitializedNavRunnable = InitializedNavScope.() -> Unit