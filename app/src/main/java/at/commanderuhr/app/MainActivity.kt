package at.commanderuhr.app

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val AppBackground = Color(0xFF10131A)
private val Panel = Color(0xFF1B202B)
private val Accent = Color(0xFF9FE870)
private val Danger = Color(0xFFFF5C68)
private val TextMain = Color(0xFFF4F6FA)
private val TextMuted = Color(0xFFABB3C2)

private data class PlayerStats(
    val life: Int = 40,
    val poison: Int = 0
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { CommanderClockApp() }
    }
}

@Composable
fun CommanderClockApp() {
    var playerCount by remember { mutableIntStateOf(4) }
    var turnSeconds by remember { mutableIntStateOf(120) }
    var names by remember { mutableStateOf((1..6).map { "Spieler $it" }) }
    var gameStarted by remember { mutableStateOf(false) }
    var activePlayer by remember { mutableIntStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }
    var remainingMillis by remember { mutableLongStateOf(120_000L) }
    var expired by remember { mutableStateOf(false) }
    var previousPlayer by remember { mutableIntStateOf(0) }
    var stats by remember { mutableStateOf((1..6).map { PlayerStats() }) }
    // commanderDamage[source][target] = damage dealt by source to target
    var commanderDamage by remember { mutableStateOf(List(6) { List(6) { 0 } }) }
    var toolMessage by remember { mutableStateOf("Würfle einen W6 oder wirf eine Münze.") }

    LaunchedEffect(isRunning, activePlayer, gameStarted) {
        if (isRunning && gameStarted && !expired) {
            var last = SystemClock.elapsedRealtime()
            while (isRunning && remainingMillis > 0L) {
                delay(100L)
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - last
                last = now
                remainingMillis = (remainingMillis - elapsed).coerceAtLeast(0L)
                if (remainingMillis == 0L) {
                    expired = true
                    isRunning = false
                }
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent, background = AppBackground, surface = Panel,
        onPrimary = AppBackground, onBackground = TextMain, onSurface = TextMain
    )) {
        Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
            if (!gameStarted) {
                SetupScreen(
                    playerCount = playerCount,
                    onPlayerCount = { count ->
                        playerCount = count
                        names = names.toMutableList().also { list ->
                            while (list.size < count) list.add("Spieler ${list.size + 1}")
                        }
                    },
                    turnSeconds = turnSeconds,
                    onTurnSeconds = { turnSeconds = it; remainingMillis = it * 1000L },
                    names = names,
                    onNameChange = { index, value -> names = names.toMutableList().also { it[index] = value } },
                    onStart = {
                        activePlayer = 0
                        previousPlayer = 0
                        remainingMillis = turnSeconds * 1000L
                        expired = false
                        stats = List(6) { PlayerStats() }
                        commanderDamage = List(6) { List(6) { 0 } }
                        gameStarted = true
                        isRunning = false
                        toolMessage = "Würfle einen W6 oder wirf eine Münze."
                    }
                )
            } else {
                GameScreen(
                    names = names.take(playerCount),
                    activePlayer = activePlayer,
                    remainingMillis = remainingMillis,
                    turnSeconds = turnSeconds,
                    isRunning = isRunning,
                    expired = expired,
                    stats = stats.take(playerCount),
                    commanderDamage = commanderDamage,
                    toolMessage = toolMessage,
                    onStartPause = {
                        if (expired) {
                            remainingMillis = turnSeconds * 1000L
                            expired = false
                        }
                        isRunning = !isRunning
                    },
                    onNext = {
                        previousPlayer = activePlayer
                        activePlayer = (activePlayer + 1) % playerCount
                        remainingMillis = turnSeconds * 1000L
                        expired = false
                        isRunning = true
                    },
                    onUndo = {
                        activePlayer = previousPlayer
                        remainingMillis = turnSeconds * 1000L
                        expired = false
                        isRunning = false
                    },
                    onLifeChange = { index, delta ->
                        stats = stats.toMutableList().also { list ->
                            list[index] = list[index].copy(life = list[index].life + delta)
                        }
                    },
                    onPoisonChange = { index, delta ->
                        stats = stats.toMutableList().also { list ->
                            list[index] = list[index].copy(poison = (list[index].poison + delta).coerceAtLeast(0))
                        }
                    },
                    onCommanderDamageChange = { source, target, delta ->
                        if (source != target) {
                            commanderDamage = commanderDamage.mapIndexed { s, row ->
                                if (s == source) row.mapIndexed { t, value ->
                                    if (t == target) (value + delta).coerceAtLeast(0) else value
                                } else row
                            }
                        }
                    },
                    onRollDice = {
                        val result = Random.nextInt(1, 7)
                        toolMessage = "🎲 W6-Ergebnis: $result"
                    },
                    onFlipCoin = {
                        toolMessage = if (Random.nextBoolean()) "🪙 Münzwurf: KOPF" else "🪙 Münzwurf: ZAHL"
                    },
                    onReset = {
                        isRunning = false
                        gameStarted = false
                        activePlayer = 0
                        remainingMillis = turnSeconds * 1000L
                        expired = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SetupScreen(
    playerCount: Int,
    onPlayerCount: (Int) -> Unit,
    turnSeconds: Int,
    onTurnSeconds: (Int) -> Unit,
    names: List<String>,
    onNameChange: (Int, String) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("COMMANDER-UHR", color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Spiel einrichten", color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Zugzeit, Spieler und Namen festlegen.", color = TextMuted, fontSize = 15.sp)

        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("SPIELER", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    (2..6).forEach { count ->
                        val selected = playerCount == count
                        Button(
                            onClick = { onPlayerCount(count) }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) Accent else Color(0xFF303746),
                                contentColor = if (selected) AppBackground else TextMain
                            ), contentPadding = PaddingValues(vertical = 12.dp)
                        ) { Text("$count", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                    }
                }
                Text("ZEIT PRO ZUG", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { onTurnSeconds((turnSeconds - 30).coerceAtLeast(30)) }) { Text("− 30 s") }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(formatDuration(turnSeconds * 1000L), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextMain)
                        Text("pro Zug", color = TextMuted, fontSize = 12.sp)
                    }
                    Button(onClick = { onTurnSeconds((turnSeconds + 30).coerceAtMost(900)) }) { Text("+ 30 s") }
                }
                Text("Bei jedem Spielerwechsel beginnt die Zugzeit neu.", color = TextMuted, fontSize = 12.sp)
            }
        }

        Text("SPIELERNAMEN", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        (0 until playerCount).forEach { index ->
            OutlinedTextField(
                value = names.getOrElse(index) { "Spieler ${index + 1}" },
                onValueChange = { onNameChange(index, it) },
                label = { Text("Spieler ${index + 1}") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent, focusedLabelColor = Accent,
                    unfocusedBorderColor = Color(0xFF3B4352)
                )
            )
        }
        Button(
            onClick = onStart, modifier = Modifier.fillMaxWidth().height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AppBackground),
            shape = RoundedCornerShape(16.dp)
        ) { Text("SPIEL STARTEN", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
    }
}

@Composable
private fun GameScreen(
    names: List<String>,
    activePlayer: Int,
    remainingMillis: Long,
    turnSeconds: Int,
    isRunning: Boolean,
    expired: Boolean,
    stats: List<PlayerStats>,
    commanderDamage: List<List<Int>>,
    toolMessage: String,
    onStartPause: () -> Unit,
    onNext: () -> Unit,
    onUndo: () -> Unit,
    onLifeChange: (Int, Int) -> Unit,
    onPoisonChange: (Int, Int) -> Unit,
    onCommanderDamageChange: (Int, Int, Int) -> Unit,
    onRollDice: () -> Unit,
    onFlipCoin: () -> Unit,
    onReset: () -> Unit
) {
    val lowTime = remainingMillis <= 15_000L
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("COMMANDER-UHR", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text("Multiplayer · ${names.size} Spieler", color = TextMuted, fontSize = 13.sp)
            }
            TextButton(onClick = onReset) { Text("Spiel beenden", color = TextMuted) }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (expired) Color(0xFF401D26) else Panel),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(if (expired) "ZEIT ABGELAUFEN" else "AM ZUG", color = if (expired) Danger else Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(names.getOrElse(activePlayer) { "Spieler ${activePlayer + 1}" }, color = TextMain, fontSize = 25.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(formatDuration(remainingMillis), color = if (expired || lowTime) Danger else TextMain, fontSize = 58.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Text(if (isRunning) "Uhr läuft" else if (expired) "Zeit abgelaufen" else "Uhr pausiert", color = TextMuted, fontSize = 13.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onNext, modifier = Modifier.weight(1f).height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AppBackground), shape = RoundedCornerShape(14.dp)) {
                Text("NÄCHSTER →", fontWeight = FontWeight.ExtraBold)
            }
            OutlinedButton(onClick = onStartPause, modifier = Modifier.weight(1f).height(58.dp), shape = RoundedCornerShape(14.dp)) {
                Text(if (isRunning) "Ⅱ PAUSE" else "▶ START", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onUndo, modifier = Modifier.height(58.dp), shape = RoundedCornerShape(14.dp)) {
                Text("↶")
            }
        }
        Text("Die Zugzeit wird bei jedem Wechsel auf ${formatDuration(turnSeconds * 1000L)} gesetzt.", color = TextMuted, fontSize = 11.sp)

        SectionTitle("LEBENSPUNKTE & GIFTMARKEN")
        names.forEachIndexed { index, name ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(name.ifBlank { "Spieler ${index + 1}" }, modifier = Modifier.weight(1f), color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("☠ ${stats[index].poison}", color = if (stats[index].poison >= 10) Danger else TextMuted, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CounterButton("−", { onLifeChange(index, -1) }, Modifier.weight(1f))
                        Column(Modifier.weight(1.5f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${stats[index].life}", color = if (stats[index].life <= 0) Danger else Accent, fontSize = 28.sp, fontWeight = FontWeight.Black)
                            Text("LEBEN", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.sp)
                        }
                        CounterButton("+", { onLifeChange(index, 1) }, Modifier.weight(1f))
                        CounterButton("− Gift", { onPoisonChange(index, -1) }, Modifier.weight(1.1f))
                        CounterButton("+ Gift", { onPoisonChange(index, 1) }, Modifier.weight(1.1f))
                    }
                }
            }
        }

        SectionTitle("COMMANDER-SCHADEN")
        Text("Schaden, den ein Commander einem anderen Spieler zugefügt hat.", color = TextMuted, fontSize = 12.sp)
        names.forEachIndexed { source, sourceName ->
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Von: $sourceName", color = Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    names.forEachIndexed { target, targetName ->
                        if (source != target) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("→ $targetName", modifier = Modifier.weight(1f), color = TextMain, fontSize = 13.sp)
                                CounterButton("−", { onCommanderDamageChange(source, target, -1) }, Modifier.width(52.dp))
                                Text("${commanderDamage[source][target]}", modifier = Modifier.width(42.dp), color = if (commanderDamage[source][target] >= 21) Danger else TextMain, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                CounterButton("+", { onCommanderDamageChange(source, target, 1) }, Modifier.width(52.dp))
                            }
                        }
                    }
                }
            }
        }

        SectionTitle("WÜRFEL & MÜNZE")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onRollDice, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = TextMain)) { Text("🎲 W6 würfeln") }
            Button(onClick = onFlipCoin, modifier = Modifier.weight(1f).height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Panel, contentColor = TextMain)) { Text("🪙 Münze werfen") }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF263222)), shape = RoundedCornerShape(12.dp)) {
            Text(toolMessage, modifier = Modifier.fillMaxWidth().padding(14.dp), color = Accent, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Accent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp)
}

@Composable
private fun CounterButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        shape = RoundedCornerShape(10.dp)
    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = ((milliseconds.coerceAtLeast(0L) + 999L) / 1000L).toInt()
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
