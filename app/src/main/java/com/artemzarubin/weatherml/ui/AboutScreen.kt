package com.artemzarubin.weatherml.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artemzarubin.weatherml.R
import com.artemzarubin.weatherml.ui.theme.DefaultPixelFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // Визначаємо стилі для різних частин тексту
    val titleStyle = SpanStyle(
        fontFamily = DefaultPixelFontFamily,
        fontSize = 24.sp, // Більший для заголовка
        fontWeight = FontWeight.Bold, // Жирний
        color = MaterialTheme.colorScheme.primary // Яскравий колір
    )
    val subtitleStyle = SpanStyle(
        fontFamily = DefaultPixelFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold, // Жирний для підзаголовків
        color = MaterialTheme.colorScheme.secondary
    )
    val normalTextStyle = SpanStyle(
        fontFamily = DefaultPixelFontFamily,
        fontSize = 18.sp,
        color = MaterialTheme.colorScheme.onBackground
    )
    val creditsTextStyle = SpanStyle( // Для секції Credits
        fontFamily = DefaultPixelFontFamily,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    val labelStyle = creditsTextStyle.copy(fontWeight = FontWeight.Bold)

    // Створюємо AnnotatedString
    val annotatedAboutText = buildAnnotatedString {
        withStyle(titleStyle) { append("Pixel Weather\n\n") }
        withStyle(normalTextStyle) { append("A Pixel Art Weather Application\n\n") }

        withStyle(subtitleStyle) { append("Developed by:\n") }
        withStyle(
            normalTextStyle.copy(
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.tertiary
            )
        ) { // Кастомний для імені
            append("Artem Zarubin\n\n")
        }

        withStyle(subtitleStyle) { append("Diploma Project\n") }
        withStyle(normalTextStyle) {
            append("Zaporizhzhia National University\n")
            append("Faculty of Mathematics\n")
            append("Software Engineering Department\n")
            append("Group 6.1211-2pi\n\n")
        }

        withStyle(subtitleStyle) { append("Supervisor:\n") }
        withStyle(normalTextStyle) { append("Vitaliy I. Gorbenko\n\n\n") }


        withStyle(subtitleStyle.copy(fontSize = 18.sp)) { append("--- SPECIAL THANKS & CREDITS ---\n\n") }

        withStyle(ParagraphStyle(textAlign = TextAlign.Justify, lineHeight = 24.sp)) {
            // Weather Data API:
            withStyle(labelStyle) { append("Weather Data API: ") } // Жирная метка
            withStyle(creditsTextStyle) { append("OpenWeatherMap (openweathermap.org)\n") } // Обычный текст

            // City Search:
            withStyle(labelStyle) { append("City Search: ") }
            withStyle(creditsTextStyle) { append("Geoapify (geoapify.com)\n") }

            // Pixel Font:
            withStyle(labelStyle) { append("Pixel Font: ") }
            withStyle(creditsTextStyle) { append("VCR OSD Mono (by Riciery Leal)\n") }

            // Weather Icons:
            withStyle(labelStyle) { append("Weather Icons: ") }
            withStyle(creditsTextStyle) { append("Pixel icons created by Freepik - Flaticon (flaticon.com)\n") }

            // Arrow Icons:
            withStyle(labelStyle) { append("Arrow Icons: ") }
            withStyle(creditsTextStyle) { append("Arrow Icons created by Disha Vaghasiya - Flaticon (flaticon.com)\n") }

            // Settings Icons:
            withStyle(labelStyle) { append("Settings Icons: ") }
            withStyle(creditsTextStyle) { append("Settings icons created by Pixel perfect - Flaticon (flaticon.com)\n") }

            // Pixel Icons:
            withStyle(labelStyle) { append("Pixel Icons: ") }
            withStyle(creditsTextStyle) { append("Pixel icons created by frelayasia - Flaticon (flaticon.com)\n") }

            // Loading Animation:
            withStyle(labelStyle) { append("Loading Animation: ") }
            withStyle(creditsTextStyle) { append("Generated using loading.io (loading.io)\n") }

            // Color Palette:
            withStyle(labelStyle) { append("Color Palette: ") }
            withStyle(creditsTextStyle) { append("\"TY - Fictional Computer OS - 32\" by Toby_Yasha (lospec.com)\n") }

            // App Icon (Launcher Icon):
            withStyle(labelStyle) { append("App Icon: ") }
            withStyle(creditsTextStyle) { append("Original art by Olga Baranova (from Vecteezy.com), modified.\n\n") }
        }
        // --- КОНЕЦ ИЗМЕНЕНИЯ для CREDITS --

        withStyle(subtitleStyle.copy(fontSize = 18.sp)) { append("--- TECHNOLOGIES ---\n\n") }

        // --- НАЧАЛО ИЗМЕНЕНИЯ для TECHNOLOGIES ---
        withStyle(
            ParagraphStyle(
                textAlign = TextAlign.Justify,
                lineHeight = 28.sp
            )
        ) { // Добавляем textAlign.Justify
            withStyle(normalTextStyle) {
                append("Kotlin, Jetpack Compose, MVVM, Hilt, Room, Retrofit, Kotlinx.Serialization, TensorFlow Lite, Coroutines & Flow\n\n\n")
                // Примечание: для Justify лучше, чтобы текст был одной длинной строкой или несколькими,
                // которые могут быть перенесены автоматически. Я объединил ваши строки для технологий в одну,
                // чтобы Justify сработал лучше. Вы можете оставить переносы \n, если хотите явные разрывы строк,
                // но тогда Justify будет применяться к каждой такой "подстроке" как к отдельному абзацу,
                // и эффект может быть не таким, как вы ожидаете для всего блока.
            }
        }
        // --- КОНЕЦ ИЗМЕНЕНИЯ для TECHNOLOGIES ---

        withStyle(normalTextStyle.copy(fontSize = 17.sp)) { append("Thank you for using Pixel Weather!\n") }
        withStyle(titleStyle.copy(fontSize = 20.sp)) { append("May the Pixels Be With You.\n\n") }
        withStyle(normalTextStyle.copy(fontSize = 16.sp)) { append("[2025]") }
    }

    // Загальний стиль для вирівнювання тексту, який буде переданий в measure
    val overallTextStyle = remember {
        TextStyle(
            textAlign = TextAlign.Center,
            lineHeight = 28.sp // Например, если это основной lineHeight для всего текста
        )
    }

    var yOffset by remember { mutableFloatStateOf(0f) }
    var canvasHeightPx by remember { mutableFloatStateOf(0f) }

    // measuredText та textHeightPx тепер будуть розраховуватися всередині BoxWithConstraints
    var measuredTextLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val textHeightPx = measuredTextLayoutResult?.size?.height?.toFloat() ?: 0f

    // --- ИЗМЕНЕНИЕ: Используем Animatable ---
    val animatableYOffset = remember { Animatable(0f) } // Начальное значение 0f

    LaunchedEffect(canvasHeightPx, textHeightPx, measuredTextLayoutResult, onNavigateBack) {
        if (canvasHeightPx > 0f && textHeightPx > 0f && measuredTextLayoutResult != null) {
            val initialY = canvasHeightPx
            val targetY = -textHeightPx

            // Устанавливаем начальное значение для Animatable без анимации
            animatableYOffset.snapTo(initialY)

            if (initialY > targetY) {
                // Анимируем к targetY
                animatableYOffset.animateTo(
                    targetValue = targetY,
                    animationSpec = tween(
                        durationMillis = ((initialY - targetY) / (density.run { 0.6.dp.toPx() }) * 15).toInt(), // Рассчитываем длительность на основе желаемой скорости
                        // или просто фиксированная длительность: durationMillis = 15000, // например, 15 секунд
                        easing = LinearEasing // Линейное изменение для равномерной прокрутки
                    )
                )
            } else {
                animatableYOffset.snapTo(targetY) // Если текст уже наверху или короче
            }

            // Логика авто-возврата
            if (isActive) {
                delay(2500L) // Ждем 2.5 секунды
                if (isActive) {
                    onNavigateBack()
                }
            }
        }
    }
// Применяем анимированное значение к yOffset, который используется в Canvas
    yOffset = animatableYOffset.value
// --- КОНЕЦ ИЗМЕНЕНИЯ ---

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "About Pixel Weather",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Image(
                            painter = painterResource(id = R.drawable.arrow_left),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp), // Встанови розмір іконки
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        BoxWithConstraints( // <--- ВИКОРИСТОВУЄМО BoxWithConstraints
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 24.dp) // Відступи з боків для тексту
                .onSizeChanged { newSize -> canvasHeightPx = newSize.height.toFloat() }
        ) { // this: BoxWithConstraintsScope

            // Вимірюємо текст тут, використовуючи максимальну ширину контейнера
            val currentMeasuredText =
                remember(annotatedAboutText, overallTextStyle, constraints.maxWidth) {
                    textMeasurer.measure(
                        text = annotatedAboutText,
                        style = overallTextStyle,
                        constraints = Constraints(maxWidth = constraints.maxWidth) // Обмежуємо ширину
                    )
                }
            measuredTextLayoutResult = currentMeasuredText // Оновлюємо стан для LaunchedEffect

            Canvas(modifier = Modifier.fillMaxSize()) {
                if (measuredTextLayoutResult != null) { // Малюємо, тільки якщо текст виміряно
                    drawIntoCanvas { composeCanvas ->
                        composeCanvas.save()
                        // Горизонтальне центрування тексту відносно Canvas
                        val xPosition = (size.width - currentMeasuredText.size.width) / 2f
                        composeCanvas.translate(xPosition, yOffset)
                        currentMeasuredText.multiParagraph.paint(composeCanvas)
                        composeCanvas.restore()
                    }
                }
            }
        }
    }
}