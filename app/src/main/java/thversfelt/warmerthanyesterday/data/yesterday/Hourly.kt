package thversfelt.warmerthanyesterday.data.yesterday
import com.google.gson.annotations.SerializedName

data class Hourly (

	@SerializedName("dt") val dt : Int,
	@SerializedName("temp") val temp : Double,
	@SerializedName("feels_like") val feels_like : Double,
	@SerializedName("pressure") val pressure : Int,
	@SerializedName("humidity") val humidity : Int,
	@SerializedName("dew_point") val dew_point : Double,
	@SerializedName("clouds") val clouds : Int,
	@SerializedName("wind_speed") val wind_speed : Double,
	@SerializedName("wind_deg") val wind_deg : Int,
	@SerializedName("weather") val weather : List<Weather>
)