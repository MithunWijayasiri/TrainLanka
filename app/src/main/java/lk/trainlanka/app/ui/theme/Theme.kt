package lk.trainlanka.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import lk.trainlanka.app.R
import lk.trainlanka.app.data.ThemeMode

val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

/** Design tokens outside the M3 scheme. */
@Immutable
data class AppColors(
    val card: Color,
    val highlight: Color,
    val muted: Color,
    val faint: Color,
    val rail: Color,
    val amber: Color,
    val onAmber: Color,
    val star: Color,
    val banner: Color,
    val bannerBorder: Color,
    val hero: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val disabled: Color,
    val onDisabled: Color,
    val badges: Map<String, Pair<Color, Color>>,
    val badgeFallback: Pair<Color, Color>,
)

private val LightApp = AppColors(
    card = Color(0xFFEEF5F3),
    highlight = Color(0xFFE4F2EF),
    muted = Color(0xFF666666),
    faint = Color(0xFF9A9A9A),
    rail = Color(0xFFCFCFCF),
    amber = Color(0xFFF6B73C),
    onAmber = Color(0xFF3B2A00),
    star = Color(0xFFE09B12),
    banner = Color(0xFFFDEFD2),
    bannerBorder = Color(0xFFE0A43A),
    hero = Color(0xFF0F5C57),
    onHero = Color.White,
    onHeroMuted = Color(0xFFDDDDDD),
    disabled = Color(0xFFD6D6D6),
    onDisabled = Color(0xFF888888),
    badges = mapOf(
        "Express" to (Color(0xFFDBE7FB) to Color(0xFF1D4F91)),
        "Intercity Express" to (Color(0xFFEBE2FB) to Color(0xFF55309A)),
        "Slow" to (Color(0xFFEFE9D8) to Color(0xFF5F5430)),
        "Semi Express" to (Color(0xFFDDE9E7) to Color(0xFF2F5551)),
    ),
    badgeFallback = Color(0xFFE4E4E4) to Color(0xFF444444),
)

private val DarkApp = AppColors(
    card = Color(0xFF1A2322),
    highlight = Color(0xFF1F2D2C),
    muted = Color(0xFFA9B4B2),
    faint = Color(0xFF6F7876),
    rail = Color(0xFF3A4442),
    amber = Color(0xFFF6B73C),
    onAmber = Color(0xFF3B2A00),
    star = Color(0xFFF6B73C),
    banner = Color(0xFF3A2E12),
    bannerBorder = Color(0xFF8A6A24),
    hero = Color(0xFF0F5C57),
    onHero = Color.White,
    onHeroMuted = Color(0xFFCFE8E4),
    disabled = Color(0xFF2C3534),
    onDisabled = Color(0xFF6F7876),
    badges = mapOf(
        "Express" to (Color(0xFF22344F) to Color(0xFFAFC8F2)),
        "Intercity Express" to (Color(0xFF33284A) to Color(0xFFD2BFF5)),
        "Slow" to (Color(0xFF3A3526) to Color(0xFFE0D4AE)),
        "Semi Express" to (Color(0xFF24332F) to Color(0xFFB7D3CE)),
    ),
    badgeFallback = Color(0xFF2C3534) to Color(0xFFCCCCCC),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0F5C57),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFE8E4),
    onPrimaryContainer = Color(0xFF0B3F3B),
    secondaryContainer = Color(0xFFCFE8E4),
    onSecondaryContainer = Color(0xFF0B3F3B),
    background = Color.White,
    onBackground = Color(0xFF1D1D1D),
    surface = Color.White,
    onSurface = Color(0xFF1D1D1D),
    surfaceVariant = Color(0xFFEEF5F3),
    onSurfaceVariant = Color(0xFF555555),
    outline = Color(0xFF888888),
    outlineVariant = Color(0xFFE4E4E4),
    error = Color(0xFFB3261E),
    inverseSurface = Color(0xFF1F2D2C),
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFFF6B73C),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7FCFC5),
    onPrimary = Color(0xFF00332F),
    primaryContainer = Color(0xFF1F4A46),
    onPrimaryContainer = Color(0xFFCFE8E4),
    secondaryContainer = Color(0xFF1F4A46),
    onSecondaryContainer = Color(0xFFCFE8E4),
    background = Color(0xFF111716),
    onBackground = Color(0xFFE3E8E7),
    surface = Color(0xFF111716),
    onSurface = Color(0xFFE3E8E7),
    surfaceVariant = Color(0xFF1A2322),
    onSurfaceVariant = Color(0xFFA9B4B2),
    outline = Color(0xFF8A9492),
    outlineVariant = Color(0xFF2C3534),
    error = Color(0xFFF2B8B5),
    inverseSurface = Color(0xFFE3ECEA),
    inverseOnSurface = Color(0xFF1F2D2C),
    inversePrimary = Color(0xFF0F5C57),
)

private val LocalAppColors = staticCompositionLocalOf { LightApp }

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
}

private fun typography(): Typography {
    val base = Typography()
    fun TextStyle.f() = copy(fontFamily = Atkinson)
    return Typography(
        displayLarge = base.displayLarge.f(), displayMedium = base.displayMedium.f(), displaySmall = base.displaySmall.f(),
        headlineLarge = base.headlineLarge.f(), headlineMedium = base.headlineMedium.f(), headlineSmall = base.headlineSmall.f(),
        titleLarge = base.titleLarge.f(), titleMedium = base.titleMedium.f(), titleSmall = base.titleSmall.f(),
        bodyLarge = base.bodyLarge.f(), bodyMedium = base.bodyMedium.f(), bodySmall = base.bodySmall.f(),
        labelLarge = base.labelLarge.f(), labelMedium = base.labelMedium.f(), labelSmall = base.labelSmall.f(),
    )
}

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun TrainLankaTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = mode.isDark()
    CompositionLocalProvider(LocalAppColors provides if (dark) DarkApp else LightApp) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = typography(),
            content = content,
        )
    }
}
