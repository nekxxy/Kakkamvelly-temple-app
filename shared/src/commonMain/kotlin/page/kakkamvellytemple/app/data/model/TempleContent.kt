package page.kakkamvellytemple.app.data.model

data class PoojaItem(
    val nameMl: String,
    val nameEn: String,
    val time: String,
    val hour: Int,
    val minute: Int
)

data class GalleryPhoto(
    val resName: String,
    val captionMl: String,
    val captionEn: String
)

data class SunTimes(val sunrise: String, val sunset: String)
data class MoonPhase(val icon: String, val nameMl: String, val nameEn: String)

data class AnnadhanamInfo(
    val nextDate: String,
    val daysAway: Int,
    val isToday: Boolean,
    val isTomorrow: Boolean
)
