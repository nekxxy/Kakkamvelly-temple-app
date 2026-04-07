package page.kakkamvellytemple.app.data.model

data class DarshanStatus(
    val isOpen: Boolean,
    val statusText: String,
    val nextLabel: String,
    val minutesUntilChange: Int
)

data class ISTTime(val hour: Int, val minute: Int, val second: Int) {
    val totalMinutes get() = hour * 60 + minute
}
