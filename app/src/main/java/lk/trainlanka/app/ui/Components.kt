package lk.trainlanka.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import lk.trainlanka.app.R
import lk.trainlanka.app.ui.theme.AppTheme

@Composable
fun TopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    titleSize: TextUnit = 18.sp,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(start = if (onBack == null) 16.dp else 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = titleSize, fontWeight = FontWeight.Bold, color = titleColor, maxLines = 1)
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        actions()
    }
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp)) {
        Text(text, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (trailing != null) Text(trailing, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TypeBadge(type: String) {
    val (bg, fg) = AppTheme.colors.badges[type] ?: AppTheme.colors.badgeFallback
    Text(
        type,
        Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp),
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
    )
}

@Composable
fun AmberChip(text: String) {
    Text(
        text,
        Modifier.clip(RoundedCornerShape(12.dp)).background(AppTheme.colors.amber).padding(horizontal = 10.dp, vertical = 3.dp),
        color = AppTheme.colors.onAmber,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )
}

/** Clock time with a smaller AM/PM suffix. */
@Composable
fun TimeText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight = FontWeight.Bold,
    strike: Boolean = false,
) {
    val split = text.lastIndexOf(' ')
    Text(
        buildAnnotatedString {
            if (split < 0) {
                append(text)
            } else {
                append(text.substring(0, split))
                withStyle(SpanStyle(fontSize = 0.6.em)) { append(text.substring(split)) }
            }
        },
        modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        textDecoration = if (strike) TextDecoration.LineThrough else null,
        maxLines = 1,
    )
}

@Composable
fun PrimaryButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (enabled) MaterialTheme.colorScheme.primary else colors.disabled)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary else colors.onDisabled,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

@Composable
fun OutlinedPill(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val color = if (enabled) MaterialTheme.colorScheme.primary else AppTheme.colors.onDisabled
    Box(
        Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, color, RoundedCornerShape(24.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** Text action used in banners and snackbars. */
@Composable
fun TextAction(
    text: String,
    underline: Boolean = false,
    color: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    Text(
        text,
        Modifier
            .clip(
                RoundedCornerShape(8.dp),
            ).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        textDecoration = if (underline) TextDecoration.Underline else null,
    )
}

const val SNACKBAR_MILLIS = 6_000L

/** Dark notice overlaid at the bottom of a screen. */
@Composable
fun Snackbar(
    text: String,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 40.dp)
            .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(6.dp))
            .padding(start = 16.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text,
            Modifier.weight(1f),
            color = MaterialTheme.colorScheme.inverseOnSurface,
            fontSize = 14.sp,
            lineHeight = 19.sp,
        )
        action()
    }
}

/** Amber notice with actions, pushed inline (not overlaid). */
@Composable
fun Banner(
    icon: @Composable () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().background(AppTheme.colors.banner).padding(start = 12.dp, end = 4.dp, top = 10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(end = 8.dp)) {
            icon()
            Text(text, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, content = actions)
    }
}

/** App icon on its amber tile, used in empty states. */
@Composable
fun AppIconTile() {
    Box(
        Modifier.size(width = 120.dp, height = 96.dp).clip(RoundedCornerShape(12.dp)).background(AppTheme.colors.card),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(AppTheme.colors.amber)) {
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, Modifier.size(72.dp))
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIconTile()
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(
            body,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun HolidayNote(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Info, contentDescription = null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text("Times may differ on Poya/public holidays", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
