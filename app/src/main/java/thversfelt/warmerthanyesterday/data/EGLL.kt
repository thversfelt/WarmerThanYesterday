package thversfelt.warmerthanyesterday.data
import com.google.gson.annotations.SerializedName

data class EGLL (
	@SerializedName("distance") val distance : Int,
	@SerializedName("latitude") val latitude : Double,
	@SerializedName("longitude") val longitude : Double,
	@SerializedName("useCount") val useCount : Int,
	@SerializedName("id") val id : String,
	@SerializedName("name") val name : String,
	@SerializedName("quality") val quality : Int,
	@SerializedName("contribution") val contribution : Int
)