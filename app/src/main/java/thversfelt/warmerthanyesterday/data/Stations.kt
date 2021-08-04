package thversfelt.warmerthanyesterday.data
import com.google.gson.annotations.SerializedName

data class Stations (
	@SerializedName("EGWU") val eGWU : EGWU,
	@SerializedName("EGLC") val eGLC : EGLC,
	@SerializedName("EGLL") val eGLL : EGLL,
	@SerializedName("D5621") val d5621 : D5621
)