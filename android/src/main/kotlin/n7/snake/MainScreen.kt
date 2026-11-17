package n7.snake

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import n7.glyph.ui.components.DotHeadline
import n7.glyph.ui.components.GlyphMatrix
import n7.glyph.ui.components.N7Button
import n7.glyph.ui.components.N7ButtonVariant
import n7.glyph.ui.components.SectionLabel
import n7.glyph.ui.components.SignatureLabel
import n7.glyph.ui.family.MoreFromN7
import n7.glyph.ui.components.N7Card
import n7.glyph.ui.support.SupporterSection
import n7.glyph.ui.support.rememberSupporter
import n7.snake.game.SnakeSkin
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import n7.glyph.ui.matrix.Cell
import n7.glyph.ui.matrix.Frames
import n7.glyph.ui.theme.N7Colors
import n7.glyph.ui.theme.N7Type

@Composable
internal fun MainScreen() {
    val context = LocalContext.current

    Surface(modifier = Modifier.fillMaxSize(), color = N7Colors.Ground) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // The status bar stays outside the scroll, so scrolled content never runs under the clock.
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                // The gesture bar is inside it: the content scrolls under it, and the end of the page clears it.
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom))
                .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // The name and, right under it, one line that says what the app is: the same pair as on the store pictures.
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DotHeadline(lines = listOf("GLYPH", "SNAKE"), fontSize = 56.sp)
                Text(stringResource(R.string.tagline), style = N7Type.Body, color = N7Colors.InkMuted)
                SectionLabel(stringResource(R.string.main_hint))
            }

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GlyphMatrix(
                    cells = rememberDemoSnake(),
                    modifier = Modifier.size(224.dp),
                    contentDescription = stringResource(R.string.main_preview_description),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Step("01", stringResource(R.string.main_step_activate))
                Step("02", stringResource(R.string.main_step_start))
                Step("03", stringResource(R.string.main_step_play))
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                N7Button(
                    text = stringResource(R.string.activate_toy),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val intent = Intent().apply {
                            component = ComponentName(
                                "com.nothing.thirdparty",
                                "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity",
                            )
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Older system versions have no Glyph Toys manager
                        }
                    },
                )
                N7Button(
                    text = stringResource(R.string.app_settings),
                    variant = N7ButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    },
                )
            }

            val supporter = rememberSupporter()
            val supporterOwned by supporter.owned.collectAsState()
            SkinCard(supporterOwned)

            SupporterSection(perks = listOf(stringResource(R.string.supporter_perk)), supporter = supporter)

            MoreFromN7(Modifier.fillMaxWidth())

            SignatureLabel(Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun Step(number: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(number, style = N7Type.BodyS.copy(fontFamily = N7Type.Label.fontFamily), color = N7Colors.InkSubtle)
        Text(text, style = N7Type.BodyS, color = N7Colors.InkMuted)
    }
}

/** A snake of 8 cells running around a square loop, with blinking food in the middle. */
@Composable
private fun rememberDemoSnake(): List<Cell> {
    val loop = remember {
        (Frames.line(6, 6, 18, 6) + Frames.line(18, 7, 18, 18) + Frames.line(17, 18, 6, 18) + Frames.line(6, 17, 6, 7))
    }
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(140)
            step++
        }
    }
    val length = 8
    val body = (0 until length).map { i -> loop[(step + i).mod(loop.size)] }
    val food = Cell(12, 12, if (step % 6 < 3) 1f else 0.5f)
    return Frames.snake(body) + food
}

/** The snake's look: CLASSIC for everyone, three more for supporters, each with a small preview on a matrix. */
@Composable
private fun SkinCard(owned: Boolean) {
    val context = LocalContext.current
    var skin by remember(owned) { mutableStateOf(SnakeSkin.current(context)) }
    N7Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionLabel(stringResource(R.string.skin_title))
            for (s in SnakeSkin.entries) {
                val unlocked = !s.supporter || owned
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .selectable(selected = s == skin, enabled = unlocked, role = Role.RadioButton) {
                            skin = s
                            SnakeSkin.save(context, s)
                        }
                        .alpha(if (unlocked) 1f else 0.45f)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        Modifier
                            .size(20.dp)
                            .border(2.dp, if (s == skin) N7Colors.Ink else N7Colors.ControlBorder, CircleShape)
                            .padding(5.dp),
                    ) {
                        if (s == skin) Box(Modifier.fillMaxSize().background(N7Colors.Ink, CircleShape))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(skinName(s)), style = N7Type.Title, color = N7Colors.Ink)
                        if (!unlocked) Text(stringResource(R.string.skin_locked), style = N7Type.BodyS, color = N7Colors.InkMuted)
                    }
                    GlyphMatrix(cells = skinPreview(s), modifier = Modifier.size(56.dp), glow = false)
                }
            }
        }
    }
}

/** A snake of ten segments across the middle, head on the right, in [skin]. */
private fun skinPreview(skin: SnakeSkin): List<Cell> = (0 until 10).map { i ->
    Cell(17 - i, 12, (skin.level(i, 10) / 2000f).coerceIn(0.12f, 1f))
}

private fun skinName(skin: SnakeSkin): Int = when (skin) {
    SnakeSkin.CLASSIC -> R.string.skin_classic
    SnakeSkin.GRADIENT -> R.string.skin_gradient
    SnakeSkin.BEADS -> R.string.skin_beads
    SnakeSkin.COMET -> R.string.skin_comet
}

