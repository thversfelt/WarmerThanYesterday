package thversfelt.warmerthanyesterday.data
import com.google.gson.annotations.SerializedName

data class CurrentConditions (
	@SerializedName("datetime") val datetime : String,
	@SerializedName("datetimeEpoch") val datetimeEpoch : Int,
	@SerializedName("temp") val temp : Double,
	@SerializedName("feelslike") val feelslike : Double,
	@SerializedName("humidity") val humidity : Double,
	@SerializedName("dew") val dew : Double,
	@SerializedName("precip") val precip : Int,
	@SerializedName("precipprob") val precipprob : String,
	@SerializedName("snow") val snow : String,
	@SerializedName("snowdepth") val snowdepth : Int,
	@SerializedName("preciptype") val preciptype : String,
	@SerializedName("windgust") val windgust : Double,
	@SerializedName("windspeed") val windspeed : Double,
	@SerializedName("winddir") val winddir : Double,
	@SerializedName("pressure") val pressure : Double,
	@SerializedName("visibility") val visibility : Double,
	@SerializedName("cloudcover") val cloudcover : Int,
	@SerializedName("solarradiation") val solarradiation : String,
	@SerializedName("solarenergy") val solarenergy : String,
	@SerializedName("uvindex") val uvindex : String,
	@SerializedName("conditions") val conditions : String,
	@SerializedName("icon") val icon : String,
	@SerializedName("stations") val stations : List<String>,
	@SerializedName("sunrise") val sunrise : String,
	@SerializedName("sunriseEpoch") val sunriseEpoch : Int,
	@SerializedName("sunset") val sunset : String,
	@SerializedName("sunsetEpoch") val sunsetEpoch : Int,
	@SerializedName("moonphase") val moonphase : Double
)