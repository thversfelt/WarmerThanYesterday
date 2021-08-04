package thversfelt.warmerthanyesterday.data
import com.google.gson.annotations.SerializedName

data class WeatherData (
	@SerializedName("queryCost") val queryCost : Int,
	@SerializedName("latitude") val latitude : Double,
	@SerializedName("longitude") val longitude : Double,
	@SerializedName("resolvedAddress") val resolvedAddress : String,
	@SerializedName("address") val address : String,
	@SerializedName("timezone") val timezone : String,
	@SerializedName("tzoffset") val tzoffset : Int,
	@SerializedName("description") val description : String,
	@SerializedName("days") val days : List<Days>,
	@SerializedName("alerts") val alerts : List<String>,
	@SerializedName("stations") val stations : Stations,
	@SerializedName("currentConditions") val currentConditions : CurrentConditions
)