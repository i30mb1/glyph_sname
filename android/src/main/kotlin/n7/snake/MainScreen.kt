package n7.snake

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
                .systemBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DotHeadline(lines = listOf("GLYPH", "SNAKE"), fontSize = 56.sp)
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

            Spacer(Modifier.weight(1f))

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
