package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.model.ColorStyle

// Base Neutral Colors
val SurfaceDark = Color(0xFF141316)
val SurfaceLight = Color(0xFFFAF9FD)
val SurfaceContainerDark = Color(0xFF1E1D22)
val SurfaceContainerLight = Color(0xFFF0EFF4)
val SurfaceContainerHighDark = Color(0xFF28272D)
val SurfaceContainerHighLight = Color(0xFFE8E7EC)

// Forge (Ambre / Cuivre Héphaïstos)
val ForgePrimaryLight = Color(0xFF8E4A00)
val ForgeOnPrimaryLight = Color(0xFFFFFFFF)
val ForgePrimaryContainerLight = Color(0xFFFFDCC3)
val ForgeOnPrimaryContainerLight = Color(0xFF2F1500)
val ForgeSecondaryLight = Color(0xFF745945)
val ForgePrimaryDark = Color(0xFFFFB77D)
val ForgeOnPrimaryDark = Color(0xFF4C2400)
val ForgePrimaryContainerDark = Color(0xFF6C3700)
val ForgeOnPrimaryContainerDark = Color(0xFFFFDCC3)
val ForgeSecondaryDark = Color(0xFFE4BFAB)

// Cobalt Moderne
val CobaltPrimaryLight = Color(0xFF005FB0)
val CobaltOnPrimaryLight = Color(0xFFFFFFFF)
val CobaltPrimaryContainerLight = Color(0xFFD5E3FF)
val CobaltOnPrimaryContainerLight = Color(0xFF001B3C)
val CobaltSecondaryLight = Color(0xFF555F71)
val CobaltPrimaryDark = Color(0xFFA6C8FF)
val CobaltOnPrimaryDark = Color(0xFF003060)
val CobaltPrimaryContainerDark = Color(0xFF004786)
val CobaltOnPrimaryContainerDark = Color(0xFFD5E3FF)
val CobaltSecondaryDark = Color(0xFFBEC7DC)

// Émeraude Cyber
val EmeraldPrimaryLight = Color(0xFF006C4C)
val EmeraldOnPrimaryLight = Color(0xFFFFFFFF)
val EmeraldPrimaryContainerLight = Color(0xFF8EF7C6)
val EmeraldOnPrimaryContainerLight = Color(0xFF002114)
val EmeraldSecondaryLight = Color(0xFF4D6356)
val EmeraldPrimaryDark = Color(0xFF71DAAB)
val EmeraldOnPrimaryDark = Color(0xFF003825)
val EmeraldPrimaryContainerDark = Color(0xFF005138)
val EmeraldOnPrimaryContainerDark = Color(0xFF8EF7C6)
val EmeraldSecondaryDark = Color(0xFFB4CCBC)

// Terracotta Cuivrée
val TerracottaPrimaryLight = Color(0xFFA04033)
val TerracottaOnPrimaryLight = Color(0xFFFFFFFF)
val TerracottaPrimaryContainerLight = Color(0xFFFFDAD4)
val TerracottaOnPrimaryContainerLight = Color(0xFF3F0401)
val TerracottaSecondaryLight = Color(0xFF775651)
val TerracottaPrimaryDark = Color(0xFFFFB4A9)
val TerracottaOnPrimaryDark = Color(0xFF61130B)
val TerracottaPrimaryContainerDark = Color(0xFF802A20)
val TerracottaOnPrimaryContainerDark = Color(0xFFFFDAD4)
val TerracottaSecondaryDark = Color(0xFFE7BDB6)

// Améthyste Profonde
val AmethystPrimaryLight = Color(0xFF6E4EA1)
val AmethystOnPrimaryLight = Color(0xFFFFFFFF)
val AmethystPrimaryContainerLight = Color(0xFFECDCFF)
val AmethystOnPrimaryContainerLight = Color(0xFF270057)
val AmethystSecondaryLight = Color(0xFF645A6F)
val AmethystPrimaryDark = Color(0xFFD5BAFF)
val AmethystOnPrimaryDark = Color(0xFF3E1C70)
val AmethystPrimaryContainerDark = Color(0xFF563588)
val AmethystOnPrimaryContainerDark = Color(0xFFECDCFF)
val AmethystSecondaryDark = Color(0xFFCEBFD8)

fun getColorScheme(style: ColorStyle, isDark: Boolean) = when (style) {
    ColorStyle.FORGE -> if (isDark) {
        darkColorScheme(
            primary = ForgePrimaryDark,
            onPrimary = ForgeOnPrimaryDark,
            primaryContainer = ForgePrimaryContainerDark,
            onPrimaryContainer = ForgeOnPrimaryContainerDark,
            secondary = ForgeSecondaryDark,
            surface = SurfaceDark,
            surfaceContainer = SurfaceContainerDark,
            surfaceContainerHigh = SurfaceContainerHighDark,
            background = SurfaceDark
        )
    } else {
        lightColorScheme(
            primary = ForgePrimaryLight,
            onPrimary = ForgeOnPrimaryLight,
            primaryContainer = ForgePrimaryContainerLight,
            onPrimaryContainer = ForgeOnPrimaryContainerLight,
            secondary = ForgeSecondaryLight,
            surface = SurfaceLight,
            surfaceContainer = SurfaceContainerLight,
            surfaceContainerHigh = SurfaceContainerHighLight,
            background = SurfaceLight
        )
    }
    ColorStyle.COBALT -> if (isDark) {
        darkColorScheme(
            primary = CobaltPrimaryDark,
            onPrimary = CobaltOnPrimaryDark,
            primaryContainer = CobaltPrimaryContainerDark,
            onPrimaryContainer = CobaltOnPrimaryContainerDark,
            secondary = CobaltSecondaryDark,
            surface = SurfaceDark,
            surfaceContainer = SurfaceContainerDark,
            surfaceContainerHigh = SurfaceContainerHighDark,
            background = SurfaceDark
        )
    } else {
        lightColorScheme(
            primary = CobaltPrimaryLight,
            onPrimary = CobaltOnPrimaryLight,
            primaryContainer = CobaltPrimaryContainerLight,
            onPrimaryContainer = CobaltOnPrimaryContainerLight,
            secondary = CobaltSecondaryLight,
            surface = SurfaceLight,
            surfaceContainer = SurfaceContainerLight,
            surfaceContainerHigh = SurfaceContainerHighLight,
            background = SurfaceLight
        )
    }
    ColorStyle.EMERALD -> if (isDark) {
        darkColorScheme(
            primary = EmeraldPrimaryDark,
            onPrimary = EmeraldOnPrimaryDark,
            primaryContainer = EmeraldPrimaryContainerDark,
            onPrimaryContainer = EmeraldOnPrimaryContainerDark,
            secondary = EmeraldSecondaryDark,
            surface = SurfaceDark,
            surfaceContainer = SurfaceContainerDark,
            surfaceContainerHigh = SurfaceContainerHighDark,
            background = SurfaceDark
        )
    } else {
        lightColorScheme(
            primary = EmeraldPrimaryLight,
            onPrimary = EmeraldOnPrimaryLight,
            primaryContainer = EmeraldPrimaryContainerLight,
            onPrimaryContainer = EmeraldOnPrimaryContainerLight,
            secondary = EmeraldSecondaryLight,
            surface = SurfaceLight,
            surfaceContainer = SurfaceContainerLight,
            surfaceContainerHigh = SurfaceContainerHighLight,
            background = SurfaceLight
        )
    }
    ColorStyle.TERRACOTTA -> if (isDark) {
        darkColorScheme(
            primary = TerracottaPrimaryDark,
            onPrimary = TerracottaOnPrimaryDark,
            primaryContainer = TerracottaPrimaryContainerDark,
            onPrimaryContainer = TerracottaOnPrimaryContainerDark,
            secondary = TerracottaSecondaryDark,
            surface = SurfaceDark,
            surfaceContainer = SurfaceContainerDark,
            surfaceContainerHigh = SurfaceContainerHighDark,
            background = SurfaceDark
        )
    } else {
        lightColorScheme(
            primary = TerracottaPrimaryLight,
            onPrimary = TerracottaOnPrimaryLight,
            primaryContainer = TerracottaPrimaryContainerLight,
            onPrimaryContainer = TerracottaOnPrimaryContainerLight,
            secondary = TerracottaSecondaryLight,
            surface = SurfaceLight,
            surfaceContainer = SurfaceContainerLight,
            surfaceContainerHigh = SurfaceContainerHighLight,
            background = SurfaceLight
        )
    }
    ColorStyle.AMETHYST -> if (isDark) {
        darkColorScheme(
            primary = AmethystPrimaryDark,
            onPrimary = AmethystOnPrimaryDark,
            primaryContainer = AmethystPrimaryContainerDark,
            onPrimaryContainer = AmethystOnPrimaryContainerDark,
            secondary = AmethystSecondaryDark,
            surface = SurfaceDark,
            surfaceContainer = SurfaceContainerDark,
            surfaceContainerHigh = SurfaceContainerHighDark,
            background = SurfaceDark
        )
    } else {
        lightColorScheme(
            primary = AmethystPrimaryLight,
            onPrimary = AmethystOnPrimaryLight,
            primaryContainer = AmethystPrimaryContainerLight,
            onPrimaryContainer = AmethystOnPrimaryContainerLight,
            secondary = AmethystSecondaryLight,
            surface = SurfaceLight,
            surfaceContainer = SurfaceContainerLight,
            surfaceContainerHigh = SurfaceContainerHighLight,
            background = SurfaceLight
        )
    }
}
