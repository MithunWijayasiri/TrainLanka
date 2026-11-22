package lk.trainlanka.app.ui

import androidx.annotation.Keep
import kotlinx.serialization.Serializable
import lk.trainlanka.app.data.Place

// Navigation resolves enum route arguments by class name; R8 must not rename it.
@Keep
enum class PickerField { FROM, TO }

@Serializable
object HomeDest

@Serializable
data class PickerDest(
    val field: PickerField,
)

@Serializable
data class PlacePickerDest(
    val place: Place,
)

@Serializable
data class ResultsDest(
    val from: Int,
    val to: Int,
)

@Serializable
data class StopsDest(
    val trainId: Int,
    val from: Int,
    val to: Int,
)

@Serializable
object SettingsDest
