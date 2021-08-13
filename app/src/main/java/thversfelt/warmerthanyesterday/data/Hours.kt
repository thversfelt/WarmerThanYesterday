package thversfelt.warmerthanyesterday.data
import com.google.gson.annotations.SerializedName

data class Hours (
	@SerializedName("datetime") val datetime : String,
	@SerializedName("datetimeEpoch") val datetimeEpoch : String,
	@SerializedName("temp") val temp : String,
	@SerializedName("feelslike") val feelslike : String,
	@SerializedName("humidity") val humidity : String,
	@SerializedName("dew") val dew : String,
	@SerializedName("precip") val precip : String,
	@SerializedName("precipprob") val precipprob : String,
	@SerializedName("snow") val snow : String,
	@SerializedName("snowdepth") val snowdepth : String,
	@SerializedName("preciptype") val preciptype : List<String>,
	@SerializedName("windgust") val windgust : String,
	@SerializedName("windspeed") val windspeed : String,
	@SerializedName("winddir") val winddir : String,
	@SerializedName("pressure") val pressure : String,
	@SerializedName("visibility") val visibility : String,
	@SerializedName("cloudcover") val cloudcover : String,
	@SerializedName("solarradiation") val solarradiation : String,
	@SerializedName("solarenergy") val solarenergy : String,
	@SerializedName("uvindex") val uvindex : String,
	@SerializedName("conditions") val conditions : String,
	@SerializedName("icon") val icon : String,
	@SerializedName("stations") val stations : List<String>,
	@SerializedName("source") val source : String
)