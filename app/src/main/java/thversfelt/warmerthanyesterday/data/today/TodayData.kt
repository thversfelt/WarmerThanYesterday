package thversfelt.warmerthanyesterday.data.today
import com.google.gson.annotations.SerializedName

data class TodayData (

		@SerializedName("lat") val lat : Double,
		@SerializedName("lon") val lon : Double,
		@SerializedName("timezone") val timezone : String,
		@SerializedName("timezone_offset") val timezone_offset : Int,
		@SerializedName("current") val current : Current,
		@SerializedName("minutely") val minutely : List<Minutely>,
		@SerializedName("hourly") val hourly : List<Hourly>,
		@SerializedName("daily") val daily : List<Daily>,
		@SerializedName("alerts") val alerts : List<Alerts>
)