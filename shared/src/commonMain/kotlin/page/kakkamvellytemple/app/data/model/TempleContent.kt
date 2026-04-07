package page.kakkamvellytemple.app.data.model

data class PoojaItem(val nameMl: String, val nameEn: String, val time: String, val hour: Int, val minute: Int)
data class GalleryPhoto(val resName: String, val captionMl: String, val captionEn: String)
data class SunTimes(val sunrise: String, val sunset: String)
data class MoonPhase(val icon: String, val nameMl: String, val nameEn: String)
data class AnnadhanamInfo(val nextDate: String, val daysAway: Int, val isToday: Boolean, val isTomorrow: Boolean)

data class TodayEvent(
    val icon: String,
    val titleMl: String,
    val titleEn: String,
    val timeMl: String,
    val timeEn: String,
    val highlightColor: Long = 0xFFFFD700
)

data class VazhipadItem(
    val nameMl: String,
    val nameEn: String,
    val amount: Int,
    val description: String = ""
)
