package thversfelt.warmerthanyesterday

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.github.mikephil.charting.data.BarData
import com.google.android.gms.ads.*
import com.google.android.gms.location.*
import com.google.gson.Gson
import thversfelt.warmerthanyesterday.data.WeatherData
import thversfelt.warmerthanyesterday.databinding.ActivityMainBinding
import kotlin.math.*
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry;
import java.util.*
import kotlin.collections.ArrayList
import com.github.mikephil.charting.components.XAxis.XAxisPosition

import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter








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

    // Temperature-related variables.
    private var yesterdayFeelsLikeValue = -1.0
    private var todayFeelsLikeValue = -1.0
    private val COLD_COLOR = "#2980B9"
    private val WARM_COLOR = "#e74c3c"

    // Precipitation-related variables.
    private val precipitation = ArrayList<Double>()
    private val LIGHT_RAIN_MAX = 0.098 // Inch/hour
    private val MODERATE_RAIN_MAX = 0.300 // Inch/hour
    private val HEAVY_RAIN_MAX = 2.000 // Inch/hour

    // Preferences-related variables.
    private val NAME_OF_PREFERENCES = "z5mw0ttojm"

    // Date-related variables.
    private val SECONDS_IN_DAY = 86400

    // Miscellaneous variables.
    private lateinit var ui: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityMainBinding.inflate(layoutInflater)
        setContentView(ui.root)

        // Initialize the UI.
        initializeUI()

        // Initialize ads.
        initializeAds()

        // Initialize location.
        initializeLocation()
    }

    private fun callAPI() {
        val lat = 55.860916 // latitude
        val lon = -4.251433 // longitude
        val today = System.currentTimeMillis() / 1000L
        val yesterday = today - SECONDS_IN_DAY
        val applicationInfo = applicationContext.packageManager.getApplicationInfo(applicationContext.packageName, PackageManager.GET_META_DATA)
        val key = applicationInfo.metaData["API_KEY"]

        // Instantiate the request queue.
        val requestQueue = Volley.newRequestQueue(this)

        // Perform the API call.
        val apiURL = "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/$lat,$lon/$yesterday/$today?key=$key"
        val apiCall = StringRequest(Request.Method.GET, apiURL, {response -> onResponseAPI(response)}, {})

        // Add the api call to the request queue.
        requestQueue.add(apiCall)
    }

    private fun initializeUI() {
        supportActionBar?.hide() // Hide the action bar.
        initializeMetricSwitch()
        initializePrecipitationChart()
    }

    private fun initializeMetricSwitch() {
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

    private fun initializePrecipitationChart() {
        ui.precipitationChart.description.isEnabled = false
        ui.precipitationChart.legend.isEnabled = false

        ui.precipitationChart.setPinchZoom(false)
        ui.precipitationChart.setTouchEnabled(false)
        ui.precipitationChart.isDoubleTapToZoomEnabled = false

        ui.precipitationChart.setDrawBarShadow(false)
        ui.precipitationChart.setDrawGridBackground(false)

        ui.precipitationChart.xAxis.isEnabled = true
        ui.precipitationChart.xAxis.labelCount = 24
        ui.precipitationChart.xAxis.position = XAxisPosition.BOTTOM
        ui.precipitationChart.xAxis.setDrawGridLines(false)

        ui.precipitationChart.axisLeft.isEnabled = true
        ui.precipitationChart.axisLeft.setDrawGridLines(true)
        ui.precipitationChart.axisLeft.setDrawLabels(true)
        ui.precipitationChart.axisLeft.setDrawAxisLine(false)

        val settings = getSharedPreferences(NAME_OF_PREFERENCES, 0)
        val metric = settings.getBoolean("metric", false)
        val formatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val percip = if (metric) millimetersToInches(value.toDouble()).toFloat() else value
                if (percip < LIGHT_RAIN_MAX) return "Light"
                else if (percip >= LIGHT_RAIN_MAX && percip < MODERATE_RAIN_MAX) return "Moderate"
                else if (percip >= MODERATE_RAIN_MAX && percip < HEAVY_RAIN_MAX) return "Heavy"
                else return "Violent"
            }
        }
        ui.precipitationChart.axisLeft.valueFormatter = formatter;

        ui.precipitationChart.axisRight.isEnabled = false
        ui.precipitationChart.axisRight.setDrawGridLines(false)
        ui.precipitationChart.axisRight.setDrawLabels(false)

        ui.precipitationChart.invalidate()
    }

    private fun updateUI() {
        updateTemperatureUI()
        updatePrecipitationUI()
    }

    private fun updateTemperatureUI() {
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
    }

    private fun updatePrecipitationUI() {
        val settings = getSharedPreferences(NAME_OF_PREFERENCES, 0)
        val metric = settings.getBoolean("metric", false)
        val values = ArrayList<BarEntry>()
        precipitation.forEachIndexed { index, value ->
            val valueConverted = if (metric) inchesToMillimeters(value) else value
            values.add(BarEntry(index.toFloat(), valueConverted.toFloat()))
        }
        val dataset = BarDataSet(values, "")
        dataset.color = Color.parseColor(COLD_COLOR);
        dataset.setDrawValues(false)

        val data = BarData(dataset)
        ui.precipitationChart.data = data
        ui.precipitationChart.invalidate() // Redraws the chart.
    }

    private fun onResponseAPI(response: String) {
        val weatherData = Gson().fromJson(response, WeatherData::class.java)
        yesterdayFeelsLikeValue = weatherData.days.first().feelslike
        todayFeelsLikeValue = weatherData.days.last().feelslike
        precipitation.clear()
        for (hour in weatherData.days.last().hours) {
            precipitation.add(hour.precip)
        }
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
        adView.adUnitId = TEST_AD_UNIT_ID
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

    private fun inchesToMillimeters(inches: Double): Double {
        return inches * 25.4
    }

    private fun millimetersToInches(millimeters: Double): Double {
        return millimeters / 25.4
    }
}