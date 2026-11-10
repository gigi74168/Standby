package com.standbyme.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.standbyme.app.R

// Instrument Sans et Nunito sont des polices variables : on fixe le poids via FontVariation.
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variable(res: Int, weight: FontWeight) =
    Font(res, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

/** Police des textes courants et des libellés. */
val InstrumentSans = FontFamily(
    variable(R.font.instrument_sans, FontWeight.Normal),
    variable(R.font.instrument_sans, FontWeight.Medium),
    variable(R.font.instrument_sans, FontWeight.SemiBold),
    variable(R.font.instrument_sans, FontWeight.Bold),
)

/** Police arrondie du design « Apple ». */
val Nunito = FontFamily(
    variable(R.font.nunito, FontWeight.Medium),
    variable(R.font.nunito, FontWeight.SemiBold),
    variable(R.font.nunito, FontWeight.Bold),
    variable(R.font.nunito, FontWeight.ExtraBold),
    variable(R.font.nunito, FontWeight.Black),
)

/** Police condensée des grands chiffres de l'horloge. */
val Anton = FontFamily(Font(R.font.anton_regular, FontWeight.Normal))

val StandByMeTypography = Typography(
    titleLarge = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    bodyLarge = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = InstrumentSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
)
