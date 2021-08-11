package thversfelt.warmerthanyesterday.data
import com.google.gson.annotations.SerializedName

data class Hours (
	@SerializedName("datetime") val datetime : String,
	@SerializedName("datetimeEpoch") val datetimeEpoch : Int,
	@SerializedName("temp") val temp : Double,
	@SerializedName("feelslike") val feelslike : Double,
	@SerializedName("humidity") val humidity : Double,
	@SerializedName("dew") val dew : Double,
	@SerializedName("precip") val precip : Double,
	@SerializedName("precipprob") val precipprob : String,
	@SerializedName("snow") val snow : String,
	@SerializedName("snowdepth") val snowdepth : String,
	@SerializedName("preciptype") val preciptype : List<String>,
	@SerializedName("windgust") val windgust : String,
	@SerializedName("windspeed") val windspeed : Double,
	@SerializedName("winddir") val winddir : Double,
	@SerializedName("pressure") val pressure : Double,
	@SerializedName("visibility") val visibility : Double,
	@SerializedName("cloudcover") val cloudcover : Double,
	@SerializedName("solarradiation") val solarradiation : String,
	@SerializedName("solarenergy") val solarenergy : String,
	@SerializedName("uvindex") val uvindex : Int,
	@SerializedName("conditions") val conditions : String,
	@SerializedName("icon") val icon : String,
	@SerializedName("stations") val stations : List<String>,
	@SerializedName("source") val source : String
)