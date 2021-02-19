package thversfelt.warmerthanyesterday.data.today
import com.google.gson.annotations.SerializedName

data class Feels_like (

	@SerializedName("day") val day : Double,
	@SerializedName("night") val night : Double,
	@SerializedName("eve") val eve : Double,
	@SerializedName("morn") val morn : Double
)