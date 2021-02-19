package thversfelt.warmerthanyesterday.data.today
import com.google.gson.annotations.SerializedName

data class Alerts (

	@SerializedName("sender_name") val sender_name : String,
	@SerializedName("event") val event : String,
	@SerializedName("start") val start : Int,
	@SerializedName("end") val end : Int,
	@SerializedName("description") val description : String
)