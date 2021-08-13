package thversfelt.warmerthanyesterday.data
import com.google.gson.annotations.SerializedName

data class EGWU (
	@SerializedName("distance") val distance : String,
	@SerializedName("latitude") val latitude : String,
	@SerializedName("longitude") val longitude : String,
	@SerializedName("useCount") val useCount : String,
	@SerializedName("id") val id : String,
	@SerializedName("name") val name : String,
	@SerializedName("quality") val quality : String,
	@SerializedName("contribution") val contribution : String
)