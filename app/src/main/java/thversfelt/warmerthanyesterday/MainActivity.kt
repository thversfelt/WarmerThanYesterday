package thversfelt.warmerthanyesterday

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import thversfelt.warmerthanyesterday.data.today.TodayData
import thversfelt.warmerthanyesterday.data.yesterday.YesterdayData
import com.google.android.gms.ads.*
import com.google.android.gms.location.*
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.math.*

class MainActivity : AppCompatActivity() {
    // Ad-related variables.
    private val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    private val AD_UNIT_ID = "ca-app-pub-3934499255955132/7364274492"
    private lateinit var adView: AdView
    private var initialLayoutComplete = false
    private val adSize: AdSize get() {
        val display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density

        var adWidthPixels = ad_view_container.width.toFloat()
        if (adWidthPixels == 0f) {
            adWidthPixels = outMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationBannerAdSizeWithWidth(this, adWidth)
    }

    // Location-related variables.
    private lateinit var locationProvider: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    // Permissions-related variables.
    private val PERMISSIONS_REQUEST_CODE = 999

    // Temperature-related variables.
    private val API_KEY = "1bdf4bc9ba804590eb9e692abfbbaadd"
    private var yesterdayFeelsLikeValue = -1.0
    private var todayFeelsLikeValue = -1.0
    private val COLD_COLOR = "#2980B9"
    private val WARM_COLOR = "#e74c3c"

    // Preferences-related variables.
    private val NAME_OF_PREFERENCES = "z5mw0ttojm"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the UI.
        initializeUI()

        // Initialize ads.
        initializeAds()

        // Initialize location.
        initializeLocation()
    }

    private fun callAPI() {
        val dt = millisToUnix(getYesterdayTimeInMillis())
        val lat = latitude
        val lon = longitude

        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(this)

        // Request a string response from the provided URL.
        val yesterdayDataURL = "https://api.openweathermap.org/data/2.5/onecall/timemachine?lat=$lat&lon=$lon&dt=$dt&appid=$API_KEY"
        val yesterdayDataRequest = StringRequest( Request.Method.GET, yesterdayDataURL,
                { response ->
                    val yesterdayData = Gson().fromJson(response, YesterdayData::class.java)
                    val yesterdayFeelsLike = yesterdayData.hourly.maxBy { it.feels_like }
                    yesterdayFeelsLikeValue = yesterdayFeelsLike?.feels_like!!
                    if (todayFeelsLikeValue >= 0) updateUI()
                }, {}
        )

        val todayDataURL = "https://api.openweathermap.org/data/2.5/onecall?lat=$lat&lon=$lon&appid=$API_KEY"
        val todayDataRequest = StringRequest( Request.Method.GET, todayDataURL,
                { response ->
                    val todayData = Gson().fromJson(response, TodayData::class.java)
                    val todayFeelsLike = todayData.hourly.subList(0, 23).maxBy { it.feels_like }
                    todayFeelsLikeValue = todayFeelsLike?.feels_like!!
                    if (yesterdayFeelsLikeValue >= 0) updateUI()
                }, {}
        )

        // Add the request to the RequestQueue.
        queue.add(yesterdayDataRequest)
        queue.add(todayDataRequest)
    }

    private fun initializeUI() {
        // Hide the action bar.
        supportActionBar?.hide()
        val metricSwitch = findViewById<Switch>(R.id.metric_switch)

        val settings = getSharedPreferences(NAME_OF_PREFERENCES, 0)
        val metric = settings.getBoolean("metric", false)
        if (metric) metricSwitch.isChecked = true

        metricSwitch.setOnCheckedChangeListener { _, isChecked ->
            val editor = settings.edit()
            editor.putBoolean("metric", isChecked)
            editor.apply()
            if (todayFeelsLikeValue >= 0 && yesterdayFeelsLikeValue >= 0) updateUI()
        }
    }

    private fun updateUI() {
        val changeFeelsLikeText = findViewById<TextView>(R.id.change_feels_like)
        val yesterdayFeelsLikeText = findViewById<TextView>(R.id.yesterday_feels_like)
        val todayFeelsLikeText = findViewById<TextView>(R.id.today_feels_like)

        val settings = getSharedPreferences(NAME_OF_PREFERENCES, 0)
        val metric = settings.getBoolean("metric", false)
        val temperatureScale = if (metric) "C" else "F"

        var yesterdayFeelsLikeValueInt = kelvinToTemperatureScale(yesterdayFeelsLikeValue, metric).roundToInt()
        var todayFeelsLikeValueInt = kelvinToTemperatureScale(todayFeelsLikeValue, metric).roundToInt()
        val changeFeelsLikeValueInt = todayFeelsLikeValueInt - yesterdayFeelsLikeValueInt

        val warmerThanYesterday = (changeFeelsLikeValueInt >= 0)
        if (warmerThanYesterday) {
            changeFeelsLikeText.text = "$changeFeelsLikeValueInt° $temperatureScale Warmer"
            changeFeelsLikeText.setBackgroundColor(Color.parseColor(WARM_COLOR))
            yesterdayFeelsLikeText.setBackgroundColor(Color.parseColor(COLD_COLOR))
            todayFeelsLikeText.setBackgroundColor(Color.parseColor(WARM_COLOR))
        }
        else {
            val changeFeelsLikeValueIntAbs = abs(changeFeelsLikeValueInt)
            changeFeelsLikeText.text = "$changeFeelsLikeValueIntAbs° $temperatureScale Colder"
            changeFeelsLikeText.setBackgroundColor(Color.parseColor(COLD_COLOR))
            yesterdayFeelsLikeText.setBackgroundColor(Color.parseColor(WARM_COLOR))
            todayFeelsLikeText.setBackgroundColor(Color.parseColor(COLD_COLOR))
        }
        yesterdayFeelsLikeText.text = "$yesterdayFeelsLikeValueInt° $temperatureScale"
        todayFeelsLikeText.text = "$todayFeelsLikeValueInt° $temperatureScale"
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
        ad_view_container.addView(adView)
        ad_view_container.viewTreeObserver.addOnGlobalLayoutListener {
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
        locationProvider = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY;
        locationRequest.interval = 10 * 1000;
        locationRequest.fastestInterval = 5 * 1000;
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult == null) {
                    return
                }
                for (location in locationResult.locations) {
                    if (location != null) {
                        getLocation()
                        if (locationProvider != null) {
                            locationProvider.removeLocationUpdates(locationCallback)
                        }
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
                    locationProvider.requestLocationUpdates(locationRequest, locationCallback, null);
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
    }

    private fun getYesterdayTimeInMillis(): Long {
        val midnightCalender = Calendar.getInstance()
        midnightCalender[Calendar.HOUR_OF_DAY] = 0
        midnightCalender[Calendar.MINUTE] = 0
        midnightCalender[Calendar.SECOND] = 0
        return midnightCalender.timeInMillis - 1000;
    }

    private fun millisToUnix(timeInMillis: Long): Long {
        return timeInMillis / 1000L
    }

    private fun kelvinToTemperatureScale(temp: Double, metric: Boolean): Double {
        if (metric) return kelvinToCelcius(temp) else return kelvinToFahrenheit(temp)
    }

    private fun kelvinToCelcius(temp: Double): Double {
        return temp - 273.15
    }

    private fun kelvinToFahrenheit(tempInKelvin: Double): Double {
        return (tempInKelvin - 273.15) * 9.0 / 5.0 + 32
    }
}