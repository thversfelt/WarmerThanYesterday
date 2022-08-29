package thversfelt.warmerthanyesterday

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.text.Html
import android.text.format.DateUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.ads.*
import com.google.android.gms.location.*
import com.google.gson.Gson
import thversfelt.warmerthanyesterday.data.WeatherData
import thversfelt.warmerthanyesterday.databinding.ActivityMainBinding
import kotlin.math.*

class MainActivity : AppCompatActivity() {
    // Ad-related variables.
    private val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private val AD_UNIT_ID = "ca-app-pub-3934499255955132/7364274492"
    private var initialLayoutComplete = false

    // Location-related variables.
    private lateinit var locationProvider: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var latitude = 0.0
    private var longitude = 0.0

    // Permissions-related variables.
    private val PERMISSIONS_REQUEST_CODE = 999

    // Weather-related variables.
    private var lastUpdateTime = 0L
    private var descriptionText = ""
    private var yesterdayFeelsLikeValue = -1.0
    private var yesterdayIcon = ""
    private var todayFeelsLikeValue = -1.0
    private var todayIcon = ""
    private val COLD_COLOR = "#2980B9"
    private val WARM_COLOR = "#e74c3c"

    // Preferences-related variables.
    private val NAME_OF_PREFERENCES = "z5mw0ttojm"

    // Date-related variables.
    private val SECONDS_IN_DAY = 86400
    private val MILLIS_IN_SECOND = 1000

    // Miscellaneous variables.
    private lateinit var ui: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        initializeUI()
        initializeFunctionality()
        initializeAds()
    }

    private fun callAPI() {
        val lat = latitude
        val lon = longitude

        val today = System.currentTimeMillis() / MILLIS_IN_SECOND
        val yesterday = today - SECONDS_IN_DAY

        val key = getString(R.string.visual_crossing_api_key);

        // Instantiate the request queue.
        val requestQueue = Volley.newRequestQueue(this)

        // Perform the API call.
        val apiURL = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/$lat,$lon/$yesterday/$today?key=$key&include=days?iconSet=icons1"
        val apiCall = StringRequest(Request.Method.GET, apiURL, {response -> onResponseAPI(response)}, {})

        // Add the api call to the request queue.
        requestQueue.add(apiCall)
    }

    private fun initializeUI() {
        supportActionBar?.hide() // Hide the action bar.

        val preferences = getSharedPreferences(NAME_OF_PREFERENCES, 0)
        val metric = preferences.getBoolean("metric", false)
        if (metric) ui.metricSwitch.isChecked = true
        ui.metricSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = preferences.edit()
            editor.putBoolean("metric", isChecked)
            editor.apply()
            if (todayFeelsLikeValue >= 0 && yesterdayFeelsLikeValue >= 0) updateUI()
        }
    }

    private fun updateUI() {
        val preferences = getSharedPreferences(NAME_OF_PREFERENCES, 0)
        val metric = preferences.getBoolean("metric", false)
        val temperatureScale = if (metric) "C" else "F"

        val yesterdayFeelsLikeValueConverted = if (metric) fahrenheitToCelsius(yesterdayFeelsLikeValue) else yesterdayFeelsLikeValue
        val todayFeelsLikeValueConverted = if (metric) fahrenheitToCelsius(todayFeelsLikeValue) else todayFeelsLikeValue

        val yesterdayFeelsLikeValueInt = yesterdayFeelsLikeValueConverted.roundToInt()
        val todayFeelsLikeValueInt = todayFeelsLikeValueConverted.roundToInt()
        val changeFeelsLikeValueInt = todayFeelsLikeValueInt - yesterdayFeelsLikeValueInt

        val warmerThanYesterday = (changeFeelsLikeValueInt >= 0)
        if (warmerThanYesterday) {
            ui.changeFeelsLike.text = "$changeFeelsLikeValueInt° $temperatureScale Warmer"
            ui.changeFeelsLike.setBackgroundColor(Color.parseColor(WARM_COLOR))
            ui.yesterdayFeelsLike.setBackgroundColor(Color.parseColor(COLD_COLOR))
            ui.todayFeelsLike.setBackgroundColor(Color.parseColor(WARM_COLOR))
        }
        else {
            val changeFeelsLikeValueIntAbs = abs(changeFeelsLikeValueInt)
            ui.changeFeelsLike.text = "$changeFeelsLikeValueIntAbs° $temperatureScale Colder"
            ui.changeFeelsLike.setBackgroundColor(Color.parseColor(COLD_COLOR))
            ui.yesterdayFeelsLike.setBackgroundColor(Color.parseColor(WARM_COLOR))
            ui.todayFeelsLike.setBackgroundColor(Color.parseColor(COLD_COLOR))
        }

        ui.yesterdayFeelsLike.text = "$yesterdayFeelsLikeValueInt° $temperatureScale"
        ui.todayFeelsLike.text = "$todayFeelsLikeValueInt° $temperatureScale"
        ui.description.text = descriptionText
        ui.lastUpdate.text = "Last update: " + DateUtils.getRelativeTimeSpanString(lastUpdateTime)
        ui.yesterdayIcon.setImageResource(resources.getIdentifier("@drawable/$yesterdayIcon", null, packageName))
        ui.todayIcon.setImageResource(resources.getIdentifier("@drawable/$todayIcon", null, packageName))
    }

    private fun onResponseAPI(response: String) {
        val weatherData = Gson().fromJson(response, WeatherData::class.java)

        yesterdayFeelsLikeValue = weatherData.days.first().feelslikemax
        todayFeelsLikeValue = weatherData.days.last().feelslikemax
        descriptionText = weatherData.days.last().description
        yesterdayIcon = weatherData.days.first().icon.replace('-', '_')
        todayIcon = weatherData.days.last().icon.replace('-', '_')
        lastUpdateTime = System.currentTimeMillis()

        updateUI()
    }

    private fun initializeAds() {
        MobileAds.initialize(this) { }

        val adView = AdView(this)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = TEST_AD_UNIT_ID

        ui.adViewContainer.addView(adView)
        ui.adViewContainer.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialLayoutComplete) {
                initialLayoutComplete = true
                val adRequest = AdRequest.Builder().build()
                adView.loadAd(adRequest)
            }
        }
    }

    private fun initializeFunctionality() {
        val preferences = getSharedPreferences(NAME_OF_PREFERENCES, 0)
        val consent = preferences.getBoolean("consent", false)

        if (!consent) {
            // Build and show the consent dialog.
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Privacy Policy")
            builder.setMessage(Html.fromHtml("By clicking 'Accept', you agree to our <a href=https://pages.flycricket.io/warmer-than-yesterda/privacy.html>Privacy Policy</a>"))
            builder.setPositiveButton("Accept") { _, _ ->
                // Save the consent to shared preferences.
                val editor = preferences.edit()
                editor.putBoolean("consent", true)
                editor.apply()

                // Get the location.
                locationProvider = LocationServices.getFusedLocationProviderClient(this)
                getLocation()
            }
            builder.setNegativeButton("Decline") { _, _ ->
                // Do nothing.
            }
            val dialog: AlertDialog = builder.create()
            dialog.show()
        }
        else {
            // Get the location.
            locationProvider = LocationServices.getFusedLocationProviderClient(this)
            getLocation()
        }
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            val permissions = arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )

            // Permission not granted, request permissions.
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE)
        }
        else {
            // Permission already granted, request last location.
            locationProvider.lastLocation.addOnSuccessListener { location : Location? ->
                if (location != null) {
                    // Got last known location.
                    latitude = location.latitude
                    longitude = location.longitude

                    // Call the OpenWeather API.
                    callAPI()
                }
                else {
                    // Last known location is null, so request location updates.
                    locationProvider.requestLocationUpdates(locationRequest, locationCallback, null)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            // Permissions are granted.
            getLocation()
        } else {
            // Permissions are denied by user.
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun fahrenheitToCelsius(temp: Double): Double {
        return (temp - 32.0) * 5.0 / 9.0
    }
}