package thversfelt.warmerthanyesterday

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.text.format.DateUtils
import android.util.DisplayMetrics
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
    private lateinit var adView: AdView
    private var initialLayoutComplete = false
    private val adSize: AdSize
        get() {
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density

            var adWidthPixels = ui.adViewContainer.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

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
        initializeAds()
        initializeLocation()
    }

    private fun callAPI() {
        val lat = latitude
        val lon = longitude

        val today = System.currentTimeMillis() / MILLIS_IN_SECOND
        val yesterday = today - SECONDS_IN_DAY

        val applicationInfo = applicationContext.packageManager.getApplicationInfo(applicationContext.packageName, PackageManager.GET_META_DATA)
        val key = applicationInfo.metaData["API_KEY"]

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

        val settings = getSharedPreferences(NAME_OF_PREFERENCES, 0)
        val metric = settings.getBoolean("metric", false)
        if (metric) ui.metricSwitch.isChecked = true
        ui.metricSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = settings.edit()
            editor.putBoolean("metric", isChecked)
            editor.apply()
            if (todayFeelsLikeValue >= 0 && yesterdayFeelsLikeValue >= 0) updateUI()
        }
    }

    private fun updateUI() {
        val settings = getSharedPreferences(NAME_OF_PREFERENCES, 0)
        val metric = settings.getBoolean("metric", false)
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

    public override fun onPause() {
        adView.pause()
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        adView.resume()
    }

    public override fun onDestroy() {
        adView.destroy()
        super.onDestroy()
    }

    private fun initializeAds() {
        MobileAds.initialize(this) { }
        adView = AdView(this)
        ui.adViewContainer.addView(adView)
        ui.adViewContainer.viewTreeObserver.addOnGlobalLayoutListener {
            if (!initialLayoutComplete) {
                initialLayoutComplete = true
                loadBanner()
            }
        }
    }

    private fun loadBanner() {
        adView.adUnitId = AD_UNIT_ID
        adView.adSize = adSize
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun initializeLocation() {
        locationProvider = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 10 * 1000
        locationRequest.fastestInterval = 5 * 1000
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    if (location != null) {
                        getLocation()
                        locationProvider.removeLocationUpdates(locationCallback)
                    }
                }
            }
        }
        getLocation()
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
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permissions are granted.
                    getLocation()
                } else {
                    // Permissions are denied by user.
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun fahrenheitToCelsius(temp: Double): Double {
        return (temp - 32.0) * 5.0 / 9.0
    }
}