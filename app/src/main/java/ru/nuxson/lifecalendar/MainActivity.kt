package ru.nuxson.lifecalendar

import android.app.DatePickerDialog
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
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
import ru.nuxson.lifecalendar.ui.theme.LifeCalendarTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
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

    var showAboutDialog by remember { mutableStateOf(false) }

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
                CalendarPreview(accentColor, bgColor, futureColor, textColor, calendarType, birthDateStr, lifeExpectancy.toInt(), scale, offsetX, offsetY, showTitle, showStats)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Показывать заголовки")
                            Switch(checked = showTitle, onCheckedChange = { 
                                showTitle = it
                                prefs.edit().putBoolean("show_title", it).apply()
                            })
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Показывать статистику")
                            Switch(checked = showStats, onCheckedChange = { 
                                showStats = it
                                prefs.edit().putBoolean("show_stats", it).apply()
                            })
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
                                prefs.edit()
                                    .putFloat("scale", 1.0f)
                                    .putFloat("offset_x", 0.5f)
                                    .putFloat("offset_y", 0.5f)
                                    .apply()
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
                            Slider(
                                value = lifeExpectancy,
                                onValueChange = { 
                                    lifeExpectancy = it
                                    prefs.edit().putInt("life_expectancy", it.toInt()).apply()
                                },
                                valueRange = 50f..120f,
                                steps = 70
                            )
                        }
                    }

                    ColorPickerSection("Акцент (Прогресс)", accentColor) {
                        accentColor = it
                        prefs.edit().putString("accent_color", it).apply()
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

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("О приложении") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Автор: Nuxson")
                    Text("Это приложение помогает визуализировать время и ценить каждый момент жизни.")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Nuxson/Life365"))
                            context.startActivity(intent)
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Исходный код на GitHub")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}

@Composable
fun CalendarPreview(accentHex: String, bgHex: String, futureHex: String, textHex: String, type: CalendarType, birthDateStr: String, lifeExpectancy: Int, scale: Float, offsetX: Float, offsetY: Float, showTitle: Boolean, showStats: Boolean) {
    val today = LocalDate.now()
    
    // Reuse paint objects to reduce allocations during recomposition/re-renders
    val titlePaint = remember { Paint().apply { textAlign = Paint.Align.LEFT; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD } }
    val miniLabelPaint = remember { Paint().apply { textAlign = Paint.Align.LEFT; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD } }
    val sideLabelPaint = remember { Paint().apply { textAlign = Paint.Align.RIGHT; isAntiAlias = true } }
    val accentPaint = remember { Paint().apply { textAlign = Paint.Align.LEFT; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD } }
    val labelPaint = remember { Paint().apply { textAlign = Paint.Align.LEFT; isAntiAlias = true } }
    val livedDayPaint = remember { Paint().apply { isAntiAlias = true } }
    val futureDayPaint = remember { Paint().apply { isAntiAlias = true } }
    val todayPaint = remember { Paint().apply { color = android.graphics.Color.RED; isAntiAlias = true } }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvas = drawContext.canvas.nativeCanvas
        val width = size.width
        val height = size.height

        canvas.save()
        canvas.translate((offsetX - 0.5f) * width, (offsetY - 0.5f) * height)
        canvas.scale(scale, scale, width / 2f, height / 2f)

        val accentColor = android.graphics.Color.parseColor(accentHex)
        val futureColor = android.graphics.Color.parseColor(futureHex)
        val textColor = android.graphics.Color.parseColor(textHex)

        titlePaint.apply { color = textColor; textSize = 45f }
        miniLabelPaint.apply { color = textColor; textSize = 18f }
        sideLabelPaint.apply { color = textColor; textSize = 16f }
        accentPaint.apply { color = accentColor; textSize = 28f }
        labelPaint.apply { color = textColor; textSize = 24f }
        livedDayPaint.color = accentColor
        futureDayPaint.color = futureColor

        when (type) {
            CalendarType.MONTH -> {
                val month = today.month
                val firstDayOfMonth = LocalDate.of(today.year, month, 1)
                val daysInMonth = month.length(firstDayOfMonth.isLeapYear)
                val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value - 1
                val dotSpacing = width / 8.5f
                val dotRadius = dotSpacing / 3.2f
                val gridStartY = height * 0.35f
                val startX = (width - (6 * dotSpacing)) / 2
                
                if (showTitle) {
                    canvas.drawText(month.getDisplayName(TextStyle.FULL, Locale("ru")).uppercase(), startX, gridStartY - 100f, titlePaint)
                }
                
                var lastY = gridStartY
                for (day in 1..daysInMonth) {
                    val dayIdx = day + dayOfWeekOffset - 1
                    val dx = startX + (dayIdx % 7 * dotSpacing)
                    val dy = gridStartY + (dayIdx / 7 * dotSpacing)
                    lastY = dy
                    val date = LocalDate.of(today.year, month, day)
                    val paint = when { date.isBefore(today) -> livedDayPaint; date.isEqual(today) -> todayPaint; else -> futureDayPaint }
                    canvas.drawCircle(dx, dy, dotRadius, paint)
                }
                
                if (showStats) {
                    val progress = (today.dayOfMonth.toFloat() / daysInMonth * 100).toInt()
                    drawStatsLine(canvas, startX, lastY + dotSpacing * 1.2f, "$progress%", " ПРОЖИТО  •  ", "${daysInMonth - today.dayOfMonth}", " ДНЕЙ ОСТАЛОСЬ", accentPaint, labelPaint)
                }
            }
            CalendarType.YEAR -> {
                val mWidth = width / 3.5f
                val mHeight = height * 0.12f
                val dotSpacing = mWidth / 8.5f
                val dotRadius = dotSpacing / 3.8f
                val gridWidth = mWidth * 3
                val gridHeight = mHeight * 4
                val startX_global = (width - gridWidth) / 2
                val startY_global = (height - gridHeight) / 2
                
                if (showTitle) {
                    canvas.drawText("${today.year} ГОД", startX_global, startY_global - 80f, titlePaint)
                }
                
                for (m in 0 until 12) {
                    val col = m % 3; val row = m / 3
                    val startX = col * mWidth + startX_global; val startY = row * mHeight + startY_global
                    val month = java.time.Month.of(m + 1)
                    
                    if (showTitle) {
                        canvas.drawText(month.getDisplayName(TextStyle.SHORT, Locale("ru")).uppercase(), startX, startY - 15f, miniLabelPaint)
                    }
                    
                    val firstDay = LocalDate.of(today.year, m + 1, 1)
                    val offset = firstDay.dayOfWeek.value - 1
                    for (day in 1..month.length(firstDay.isLeapYear)) {
                        val dIdx = day + offset - 1
                        val dx = startX + (dIdx % 7 * dotSpacing); val dy = startY + (dIdx / 7 * dotSpacing)
                        val date = LocalDate.of(today.year, m + 1, day)
                        val paint = when { date.isBefore(today) -> livedDayPaint; date.isEqual(today) -> todayPaint; else -> futureDayPaint }
                        canvas.drawCircle(dx, dy, dotRadius, paint)
                    }
                }
                
                if (showStats) {
                    val totalDays = if (java.time.Year.isLeap(today.year.toLong())) 366 else 365
                    val progress = (today.dayOfYear.toFloat() / totalDays * 100).toInt()
                    drawStatsLine(canvas, startX_global, startY_global + gridHeight + 40f, "$progress%", " ГОДА ПРОШЛО  •  ", "${totalDays - today.dayOfYear}", " ДНЕЙ ОСТАЛОСЬ", accentPaint, labelPaint)
                }
            }
            CalendarType.LIFE -> {
                val birthDate = LocalDate.parse(birthDateStr)
                val totalWeeks = lifeExpectancy * 52
                val weeksLived = ChronoUnit.WEEKS.between(birthDate, today).toInt()
                val dotSpacing = (width - width * 0.2f) / 105f
                val dotRadius = dotSpacing / 2.8f
                val startX = width * 0.12f
                val startY = height * 0.2f
                
                if (showTitle) {
                    canvas.drawText("LifeDots", startX, startY - 80f, titlePaint)
                }
                
                for (week in 0 until totalWeeks) {
                    val dx = startX + (week % 104 * dotSpacing); val dy = startY + (week / 104 * dotSpacing * 1.8f)
                    if (week % 104 == 0 && (week / 52) % 10 == 0 && showTitle) {
                        canvas.drawText("${week / 52}", startX - 25f, dy + dotRadius, sideLabelPaint)
                    }
                    val paint = if (week < weeksLived) livedDayPaint else if (week == weeksLived) todayPaint else futureDayPaint
                    canvas.drawCircle(dx, dy, dotRadius, paint)
                }
                
                if (showStats) {
                    val progress = (weeksLived.toFloat() / totalWeeks * 100).toInt()
                    drawStatsLine(canvas, startX, startY + (lifeExpectancy / 2 + 2) * dotSpacing * 1.8f + 40f, "$progress%", " ЖИЗНИ  •  ", "${totalWeeks - weeksLived}", " НЕДЕЛЬ ОСТАЛОСЬ", accentPaint, labelPaint)
                }
            }
        }
        canvas.restore()
    }
}

private fun drawStatsLine(canvas: android.graphics.Canvas, startX: Float, y: Float, val1: String, lab1: String, val2: String, lab2: String, accent: Paint, label: Paint) {
    var x = startX
    canvas.drawText(val1, x, y, accent); x += accent.measureText(val1)
    canvas.drawText(lab1, x, y, label); x += label.measureText(lab1)
    canvas.drawText(val2, x, y, accent); x += accent.measureText(val2)
    canvas.drawText(lab2, x, y, label)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerSection(title: String, selectedColor: String, onColorSelected: (String) -> Unit) {
    val colors = listOf("#4CAF50", "#2196F3", "#F44336", "#FFC107", "#E91E63", "#9C27B0", "#673AB7", "#00BCD4", "#009688", "#FF9688", "#795548", "#607D8B", "#000000", "#1A1A1A", "#333333", "#FFFFFF")
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
