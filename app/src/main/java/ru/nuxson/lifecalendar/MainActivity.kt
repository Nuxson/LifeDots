package ru.nuxson.lifecalendar

import android.app.DatePickerDialog
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import ru.nuxson.lifecalendar.ui.theme.LifeCalendarTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class CalendarType { MONTH, YEAR, LIFE }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LifeCalendarTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE) }
    
    var accentColor by remember { mutableStateOf(prefs.getString("accent_color", "#4CAF50") ?: "#4CAF50") }
    var bgColor by remember { mutableStateOf(prefs.getString("bg_color", "#000000") ?: "#000000") }
    var futureColor by remember { mutableStateOf(prefs.getString("future_color", "#222222") ?: "#222222") }
    var weekendColor by remember { mutableStateOf(prefs.getString("weekend_color", "#FF5252") ?: "#FF5252") }
    var textColor by remember { mutableStateOf(prefs.getString("text_color", "#FFFFFF") ?: "#FFFFFF") }
    var calendarType by remember { 
        mutableStateOf(CalendarType.valueOf(prefs.getString("calendar_type", CalendarType.MONTH.name) ?: CalendarType.MONTH.name)) 
    }
    var birthDateStr by remember { mutableStateOf(prefs.getString("birth_date", "2000-01-01") ?: "2000-01-01") }
    var lifeExpectancy by remember { mutableFloatStateOf(prefs.getInt("life_expectancy", 80).toFloat()) }
    
    var scale by remember { mutableFloatStateOf(prefs.getFloat("scale", 1.0f)) }
    var offsetX by remember { mutableFloatStateOf(prefs.getFloat("offset_x", 0.5f)) }
    var offsetY by remember { mutableFloatStateOf(prefs.getFloat("offset_y", 0.5f)) }

    var showTitle by remember { mutableStateOf(prefs.getBoolean("show_title", true)) }
    var showStats by remember { mutableStateOf(prefs.getBoolean("show_stats", true)) }
    var showDates by remember { mutableStateOf(prefs.getBoolean("show_dates", false)) }
    var showDayNames by remember { mutableStateOf(prefs.getBoolean("show_day_names", true)) }

    var eventsJson by remember { mutableStateOf(prefs.getString("events", "[]") ?: "[]") }
    val events = remember(eventsJson) { parseEvents(eventsJson) }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("LifeDots", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "О приложении")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Box(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                putExtra(
                                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                    ComponentName(context, CalendarWallpaperService::class.java)
                                )
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(text = "Установить на обои", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(9f/16f).padding(16.dp).clip(RoundedCornerShape(28.dp))
                    .background(parseColorSafe(bgColor, Color.Black)),
                contentAlignment = Alignment.Center
            ) {
                val drawParams = CalendarDrawParams(
                    accentColor = android.graphics.Color.parseColor(accentColor),
                    bgColor = android.graphics.Color.parseColor(bgColor),
                    futureColor = android.graphics.Color.parseColor(futureColor),
                    weekendColor = android.graphics.Color.parseColor(weekendColor),
                    textColor = android.graphics.Color.parseColor(textColor),
                    birthDateStr = birthDateStr,
                    lifeExpectancy = lifeExpectancy,
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    showTitle = showTitle,
                    showStats = showStats,
                    showDates = showDates,
                    showDayNames = showDayNames
                )
                Canvas(modifier = Modifier.fillMaxSize()) {
                    CalendarRenderer.draw(
                        drawContext.canvas.nativeCanvas,
                        size.width,
                        size.height,
                        calendarType,
                        LocalDate.now(),
                        drawParams,
                        events
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text("Настройка стиля", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Тип календаря", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = calendarType == CalendarType.MONTH,
                                onClick = { 
                                    calendarType = CalendarType.MONTH
                                    prefs.edit().putString("calendar_type", CalendarType.MONTH.name).apply()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                icon = { Icon(Icons.Default.CalendarMonth, null) }
                            ) { Text("Месяц") }
                            SegmentedButton(
                                selected = calendarType == CalendarType.YEAR,
                                onClick = { 
                                    calendarType = CalendarType.YEAR
                                    prefs.edit().putString("calendar_type", CalendarType.YEAR.name).apply()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                icon = { Icon(Icons.Default.DateRange, null) }
                            ) { Text("Год") }
                            SegmentedButton(
                                selected = calendarType == CalendarType.LIFE,
                                onClick = { 
                                    calendarType = CalendarType.LIFE
                                    prefs.edit().putString("calendar_type", CalendarType.LIFE.name).apply()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                icon = { Icon(Icons.Default.History, null) }
                            ) { Text("Жизнь") }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Элементы отображения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        ToggleRow("Показывать заголовки", showTitle) {
                            showTitle = it
                            prefs.edit().putBoolean("show_title", it).apply()
                        }
                        ToggleRow("Показывать статистику", showStats) {
                            showStats = it
                            prefs.edit().putBoolean("show_stats", it).apply()
                        }
                        ToggleRow("Отображать даты на точках", showDates) {
                            showDates = it
                            prefs.edit().putBoolean("show_dates", it).apply()
                        }
                        if (calendarType == CalendarType.MONTH) {
                            ToggleRow("Дни недели (ПН-ВС)", showDayNames) {
                                showDayNames = it
                                prefs.edit().putBoolean("show_day_names", it).apply()
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Важные события", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showAddEventDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Добавить событие")
                            }
                        }
                        
                        if (events.isEmpty()) {
                            Text("Событий пока нет", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                events.forEach { event ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(Color(event.color)))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(event.label ?: "Событие", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                                Text(event.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                        IconButton(onClick = {
                                            val newEvents = events.filter { it != event }
                                            eventsJson = serializeEvents(newEvents)
                                            prefs.edit().putString("events", eventsJson).apply()
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Размер и положение", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                scale = 1.0f
                                offsetX = 0.5f
                                offsetY = 0.5f
                                prefs.edit().putFloat("scale", 1.0f).putFloat("offset_x", 0.5f).putFloat("offset_y", 0.5f).apply()
                            }) {
                                Icon(Icons.Default.RestartAlt, contentDescription = "Сброс")
                            }
                        }
                        
                        Text("Масштаб: ${(scale * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
                        Slider(value = scale, onValueChange = { scale = it; prefs.edit().putFloat("scale", it).apply() }, valueRange = 0.5f..2.0f)
                        
                        Text("Положение X", style = MaterialTheme.typography.labelLarge)
                        Slider(value = offsetX, onValueChange = { offsetX = it; prefs.edit().putFloat("offset_x", it).apply() }, valueRange = 0.0f..1.0f)
                        
                        Text("Положение Y", style = MaterialTheme.typography.labelLarge)
                        Slider(value = offsetY, onValueChange = { offsetY = it; prefs.edit().putFloat("offset_y", it).apply() }, valueRange = 0.0f..1.0f)
                    }

                    if (calendarType == CalendarType.LIFE) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Ваша дата рождения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedButton(
                                onClick = {
                                    val date = LocalDate.parse(birthDateStr)
                                    DatePickerDialog(context, { _, y, m, d ->
                                        val newDate = LocalDate.of(y, m + 1, d).toString()
                                        birthDateStr = newDate
                                        prefs.edit().putString("birth_date", newDate).apply()
                                    }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(LocalDate.parse(birthDateStr).format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ru"))))
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Ожидаемая продолжительность: ${lifeExpectancy.toInt()} лет", style = MaterialTheme.typography.titleSmall)
                            Slider(value = lifeExpectancy, onValueChange = { lifeExpectancy = it; prefs.edit().putInt("life_expectancy", it.toInt()).apply() }, valueRange = 50f..120f, steps = 70)
                        }
                    }

                    ColorPickerSection("Акцент (Прошедшие)", accentColor) {
                        accentColor = it
                        prefs.edit().putString("accent_color", it).apply()
                    }

                    ColorPickerSection("Выходные", weekendColor) {
                        weekendColor = it
                        prefs.edit().putString("weekend_color", it).apply()
                    }

                    ColorPickerSection("Фон", bgColor) {
                        bgColor = it
                        prefs.edit().putString("bg_color", it).apply()
                    }

                    ColorPickerSection("Будущие дни", futureColor) {
                        futureColor = it
                        prefs.edit().putString("future_color", it).apply()
                    }

                    ColorPickerSection("Цвет текста", textColor) {
                        textColor = it
                        prefs.edit().putString("text_color", it).apply()
                    }
                    
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showAddEventDialog) {
        var eventDate by remember { mutableStateOf(LocalDate.now()) }
        var eventLabel by remember { mutableStateOf("") }
        var eventColor by remember { mutableStateOf("#F44336") }
        
        AlertDialog(
            onDismissRequest = { showAddEventDialog = false },
            title = { Text("Новое событие") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(value = eventLabel, onValueChange = { eventLabel = it }, label = { Text("Название") }, singleLine = true)
                    OutlinedButton(onClick = {
                        DatePickerDialog(context, { _, y, m, d -> eventDate = LocalDate.of(y, m + 1, d) }, eventDate.year, eventDate.monthValue - 1, eventDate.dayOfMonth).show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(eventDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ru"))))
                    }
                    ColorPickerSection("Цвет события", eventColor) { eventColor = it }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newEvent = CalendarEvent(eventDate, android.graphics.Color.parseColor(eventColor), eventLabel)
                    val newEvents = events + newEvent
                    eventsJson = serializeEvents(newEvents)
                    prefs.edit().putString("events", eventsJson).apply()
                    showAddEventDialog = false
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAddEventDialog = false }) { Text("Отмена") } }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("О приложении") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Автор: Nuxson")
                    Text("Это приложение помогает визуализировать время и ценить каждый момент жизни.")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Nuxson/Life365"))
                        context.startActivity(intent)
                    }, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Исходный код на GitHub")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAboutDialog = false }) { Text("Закрыть") } }
        )
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun parseEvents(json: String): List<CalendarEvent> {
    return try {
        val arr = JSONArray(json)
        List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            CalendarEvent(LocalDate.parse(obj.getString("date")), obj.getInt("color"), obj.optString("label"))
        }
    } catch (e: Exception) { emptyList() }
}

private fun serializeEvents(events: List<CalendarEvent>): String {
    val arr = JSONArray()
    events.forEach { event ->
        val obj = JSONObject()
        obj.put("date", event.date.toString())
        obj.put("color", event.color)
        obj.put("label", event.label)
        arr.put(obj)
    }
    return arr.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSection(title: String, selectedColor: String, onColorSelected: (String) -> Unit) {
    val colors = listOf("#4CAF50", "#2196F3", "#F44336", "#FFC107", "#E91E63", "#9C27B0", "#673AB7", "#00BCD4", "#009688", "#FF9688", "#795548", "#607D8B", "#000000", "#FFFFFF")
    var showCustomDialog by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Surface(onClick = { showCustomDialog = true }, modifier = Modifier.size(width = 90.dp, height = 36.dp), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Box(contentAlignment = Alignment.Center) { Text(text = selectedColor.uppercase(), style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace) }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(colors) { color -> ColorCircle(color, selectedColor.uppercase() == color.uppercase()) { onColorSelected(color) } }
        }
    }
    if (showCustomDialog) {
        var customHex by remember { mutableStateOf(selectedColor.removePrefix("#")) }
        AlertDialog(onDismissRequest = { showCustomDialog = false }, title = { Text("Свой цвет (HEX)") }, text = { OutlinedTextField(value = customHex, onValueChange = { if (it.length <= 6) customHex = it.uppercase().filter { c -> c.isDigit() || c in 'A'..'F' } }, label = { Text("Например: FF5722") }, prefix = { Text("#") }, singleLine = true) }, confirmButton = { TextButton(onClick = { if (customHex.length == 6) { onColorSelected("#$customHex"); showCustomDialog = false } }) { Text("OK") } }, dismissButton = { TextButton(onClick = { showCustomDialog = false }) { Text("Отмена") } })
    }
}

@Composable
fun ColorCircle(colorHex: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = parseColorSafe(colorHex, Color.Gray), border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        if (isSelected) {
            Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = if (colorHex.uppercase() == "#FFFFFF" || colorHex.uppercase() == "#FFC107") Color.Black else Color.White, modifier = Modifier.size(24.dp)) }
        }
    }
}

fun parseColorSafe(colorHex: String, defaultColor: Color): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        defaultColor
    }
}
