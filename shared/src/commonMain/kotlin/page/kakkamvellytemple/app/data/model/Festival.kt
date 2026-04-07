package page.kakkamvellytemple.app.data.model

import kotlinx.datetime.Instant

data class Festival(
    val nameMl: String,
    val nameEn: String,
    val icon: String,
    val dateUtcMs: Long,           // epoch ms — Date.UTC() equivalent
    val noteMl: String = "",
    val isSpecial: Boolean = false
) {
    val instant: Instant get() = Instant.fromEpochMilliseconds(dateUtcMs)
}
