package thversfelt.warmerthanyesterday.data
import com.google.gson.annotations.SerializedName

data class WeatherData (
	@SerializedName("queryCost") val queryCost : String,
	@SerializedName("latitude") val latitude : String,
	@SerializedName("longitude") val longitude : String,
	@SerializedName("resolvedAddress") val resolvedAddress : String,
	@SerializedName("address") val address : String,
	@SerializedName("timezone") val timezone : String,
	@SerializedName("tzoffset") val tzoffset : String,
	@SerializedName("description") val description : String,
	@SerializedName("days") val days : List<Days>,
	@SerializedName("alerts") val alerts : List<String>,
	@SerializedName("stations") val stations : Stations,
	@SerializedName("currentConditions") val currentConditions : CurrentConditions
)