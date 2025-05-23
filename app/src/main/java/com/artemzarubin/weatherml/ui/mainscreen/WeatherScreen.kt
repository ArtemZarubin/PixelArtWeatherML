package com.artemzarubin.weatherml.ui.mainscreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.artemzarubin.weatherml.R
import com.artemzarubin.weatherml.data.preferences.TemperatureUnit
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle
import com.artemzarubin.weatherml.ui.CurrentWeatherDetailsSection
import com.artemzarubin.weatherml.ui.CurrentWeatherMainSection
import com.artemzarubin.weatherml.ui.PixelatedSunLoader
import com.artemzarubin.weatherml.ui.SimpleHourlyForecastItemView
import com.artemzarubin.weatherml.ui.SimplifiedDailyForecastItemView
import com.artemzarubin.weatherml.ui.common.PixelArtCard
import com.artemzarubin.weatherml.ui.theme.DefaultPixelFontFamily
import com.artemzarubin.weatherml.util.Resource
import kotlinx.coroutines.isActive
import kotlin.coroutines.cancellation.CancellationException

// Функцію виносимо на рівень файлу
private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

// Кастомний Composable для PageIndicator
@Composable
fun PageIndicator(
    numberOfPages: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
    indicatorSize: Dp = 8.dp,
    spacing: Dp = 8.dp
) {
    Row(
        modifier = modifier.wrapContentHeight(), // Висота за вмістом
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until numberOfPages) {
            Box(
                modifier = Modifier
                    .size(indicatorSize)
                    .clip(CircleShape) // Робимо індикатори круглими
                    .background(if (i == currentPage) activeColor else inactiveColor)
            )
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterialApi::class
)
@Composable
fun WeatherScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToManageCities: () -> Unit,
    onNavigateToSettings: () -> Unit // <--- ДОДАНО НОВИЙ ПАРАМЕТР
) {
    val pagerItemsList by viewModel.pagerItems.collectAsState()
    val currentPagerIndexFromVM by viewModel.currentPagerIndex.collectAsState()
    val weatherDataMap by viewModel.weatherDataStateMap.collectAsState() // Збираємо всю мапу станів

    val userPreferences by viewModel.userPreferencesFlow.collectAsState() // <--- ОТРИМУЄМО НАЛАШТУВАННЯ

    val pagerState = rememberPagerState(
        initialPage = currentPagerIndexFromVM.coerceIn(
            0,
            (pagerItemsList.size - 1).coerceAtLeast(0)
        ),
        pageCount = { pagerItemsList.size }
    )

    val programmaticScrollInProgress = remember { mutableStateOf(false) }

    val context = LocalContext.current
    var showPermissionRationale by rememberSaveable { mutableStateOf(false) }
    var permissionsRequestedThisSession by rememberSaveable { mutableStateOf(false) }
    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val weatherDataMapFromVM by viewModel.weatherDataStateMap.collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            // permissionsRequestedThisSession = true
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineLocationGranted || coarseLocationGranted) {
                showPermissionRationale = false // Дозволи надано, ховаємо UI помилки
                permissionsRequestedThisSession = true // Позначимо, що взаємодія була
                viewModel.handlePermissionGranted()
            } else {
                permissionsRequestedThisSession = true // Позначимо, що взаємодія була (відмова)
                val activity = context as? ComponentActivity
                val canRequestAgain = locationPermissions.any { perm ->
                    activity?.shouldShowRequestPermissionRationale(perm)
                        ?: false // Якщо false, то "don't ask again"
                }
                val isPermanentlyDenied = !canRequestAgain

                viewModel.setPermissionError(
                    if (isPermanentlyDenied) "Location permission permanently denied. Please enable it in app settings."
                    else "Location permission denied. Click to try again."
                )
                showPermissionRationale = true // Показуємо UI помилки
            }
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                Log.d(
                    "WeatherScreen",
                    "ON_START. PermRequestedThisSession: $permissionsRequestedThisSession, Current ShowRationale: $showPermissionRationale"
                )
                val allGranted = hasLocationPermission(context)
                if (allGranted) {
                    Log.d("WeatherScreen", "Permissions GRANTED on ON_START.")
                    if (showPermissionRationale) { // Если UI ошибки разрешений был виден
                        Log.d(
                            "WeatherScreen",
                            "Hiding permission rationale UI as permissions are now granted."
                        )
                        showPermissionRationale = false // Скрываем его
                    }
                    // Всегда вызываем handlePermissionGranted, если разрешения есть при старте,
                    // чтобы ViewModel мог переинициализировать загрузку геолокации или очистить ошибки.
                    viewModel.handlePermissionGranted()
                } else {
                    // ... остальная логика для случая, когда разрешений НЕТ ...
                    // Важно: если разрешения были "permanently denied", а пользователь только что вернулся из настроек,
                    // этот блок (else) не должен выполняться, если allGranted стало true.
                    Log.d("WeatherScreen", "Permissions NOT GRANTED on ON_START.")
                    val activity = context as? ComponentActivity
                    val canRequestAgain = locationPermissions.any { perm ->
                        activity?.shouldShowRequestPermissionRationale(perm) ?: false
                    }
                    // permissionsRequestedThisSession здесь важно, чтобы не показывать "permanently denied" до первого запроса
                    val isPermanentlyDenied = !canRequestAgain && permissionsRequestedThisSession

                    if (isPermanentlyDenied) {
                        Log.d("WeatherScreen", "ON_START: Permissions seem permanently denied.")
                        viewModel.setPermissionError("Location permission permanently denied. Please enable it in app settings.")
                        showPermissionRationale = true
                    } else if (permissionsRequestedThisSession && !allGranted) { // Была попытка запроса, но отказали (не перманентно)
                        Log.d(
                            "WeatherScreen",
                            "ON_START: Permissions denied, but not permanently (or rationale pending)."
                        )
                        viewModel.setPermissionError("Location permission denied. Click to try again.")
                        showPermissionRationale = true
                    } else if (!permissionsRequestedThisSession && !allGranted) { // Еще не запрашивали в этой сессии
                        Log.d(
                            "WeatherScreen",
                            "ON_START: Requesting permissions for the first time this session."
                        )
                        locationPermissionLauncher.launch(locationPermissions)
                    }
                    // Если showPermissionRationale уже true из-за предыдущего состояния, и разрешения все еще не даны,
                    // то UI ошибки останется видимым, что корректно.
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }



    LaunchedEffect(currentPagerIndexFromVM, pagerItemsList.size, pagerState.pageCount) {
        val targetPage = currentPagerIndexFromVM
        // Используем актуальный размер списка pagerItemsList на момент запуска эффекта
        val currentListSize = pagerItemsList.size

        if (currentListSize > 0 && targetPage >= 0 && targetPage < currentListSize) {
            // Проверяем, что pageCount в PagerState уже соответствует актуальному списку
            // Это важно, чтобы избежать ошибок при скролле к индексу, которого еще нет в PagerState
            if (pagerState.pageCount == currentListSize) {
                // Проверяем, нужно ли вообще скроллить
                if (pagerState.currentPage != targetPage || pagerState.settledPage != targetPage) {
                    Log.d(
                        "WeatherScreen",
                        "VM wants page $targetPage. Pager at ${pagerState.currentPage} (settled: ${pagerState.settledPage}). Programmatic scroll starting."
                    )
                    programmaticScrollInProgress.value =
                        true // Устанавливаем флаг ПЕРЕД началом скролла
                    try {
                        if (isActive) { // Убедимся, что корутина все еще активна
                            pagerState.animateScrollToPage(targetPage)
                            // Анимация завершена (или прервана), settledPage должен обновиться
                            Log.d(
                                "WeatherScreen",
                                "Programmatic scroll animateScrollToPage($targetPage) attempt finished."
                            )
                        } else {
                            Log.w(
                                "WeatherScreen",
                                "Programmatic scroll to $targetPage skipped: coroutine not active."
                            )
                        }
                    } catch (e: CancellationException) {
                        Log.w("WeatherScreen", "Scroll animation to $targetPage was CANCELLED.")
                        // Флаг будет сброшен в finally
                    } catch (e: Exception) {
                        Log.e(
                            "WeatherScreen",
                            "Error animating scroll to page $targetPage: ${e.message}"
                        )
                        // Попытка неанимированного скролла в случае ошибки анимации
                        if (isActive && targetPage < pagerState.pageCount) { // Проверяем isActive и границы
                            try {
                                Log.d(
                                    "WeatherScreen",
                                    "Attempting non-animated scroll to $targetPage due to animation error."
                                )
                                pagerState.scrollToPage(targetPage)
                                Log.d(
                                    "WeatherScreen",
                                    "Fallback non-animated scroll to $targetPage successful."
                                )
                            } catch (e2: Exception) {
                                Log.e(
                                    "WeatherScreen",
                                    "Fallback scrollToPage to $targetPage also failed: ${e2.message}"
                                )
                            }
                        }
                    } finally {
                        // Важно: сбрасываем флаг ПОСЛЕ того, как PagerState имел шанс "устаканиться"
                        // Однако, если animateScrollToPage отменяется, settledPage может не сразу обновиться.
                        // Логика в другом LaunchedEffect (на settledPage) должна это учесть.
                        // Здесь мы просто сигнализируем, что *попытка* программного скролла завершена.
                        if (programmaticScrollInProgress.value) { // Сбрасываем, только если мы его установили
                            Log.d(
                                "WeatherScreen",
                                "Programmatic scroll attempt to $targetPage ended. Resetting flag: ${programmaticScrollInProgress.value} -> false"
                            )
                            programmaticScrollInProgress.value = false
                        }
                    }
                } else {
                    Log.d(
                        "WeatherScreen",
                        "VM wants page $targetPage. Pager already at $targetPage (currentPage: ${pagerState.currentPage}, settledPage: ${pagerState.settledPage}). No scroll needed."
                    )
                    // Если мы уже на нужной странице, и флаг был установлен ранее (маловероятно, но для чистоты), сбросим его.
                    if (programmaticScrollInProgress.value) {
                        programmaticScrollInProgress.value = false
                        Log.d(
                            "WeatherScreen",
                            "Resetting programmatic scroll flag as already on target page."
                        )
                    }
                }
            } else {
                Log.w(
                    "WeatherScreen",
                    "VM wants page $targetPage. pagerState.pageCount (${pagerState.pageCount}) != currentListSize ($currentListSize). Scroll deferred or might fail."
                )
                // Если pageCount не совпадает, скролл не будет выполнен, поэтому флаг не должен оставаться true.
                if (programmaticScrollInProgress.value) {
                    programmaticScrollInProgress.value = false
                    Log.d(
                        "WeatherScreen",
                        "Resetting programmatic scroll flag due to pageCount mismatch."
                    )
                }
            }
        } else if (currentListSize == 0) {
            Log.d(
                "WeatherScreen",
                "VM wants page $targetPage, but pagerItemsList is empty. No scroll action."
            )
            if (programmaticScrollInProgress.value) { // Если список стал пустым во время попытки скролла
                programmaticScrollInProgress.value = false
                Log.d("WeatherScreen", "Resetting programmatic scroll flag as list became empty.")
            }
        } else { // targetPage за пределами currentListSize
            Log.w(
                "WeatherScreen",
                "VM wants page $targetPage, which is out of bounds for list size $currentListSize. No scroll action."
            )
            if (programmaticScrollInProgress.value) {
                programmaticScrollInProgress.value = false
                Log.d(
                    "WeatherScreen",
                    "Resetting programmatic scroll flag due to out-of-bounds target."
                )
            }
        }
    }

    // Эффект для синхронизации PagerState -> ViewModel (когда пользователь свайпает или программный скролл завершается)
    // Реагируем только на изменение pagerState.settledPage
    LaunchedEffect(pagerState.settledPage) {
        val settledPage = pagerState.settledPage
        // Получаем актуальный индекс из ViewModel для сравнения
        val vmCurrentIndex = viewModel.currentPagerIndex.value

        Log.d(
            "WeatherScreen",
            "Pager settled at $settledPage. VM index: $vmCurrentIndex. Programmatic scroll flag: ${programmaticScrollInProgress.value}"
        )

        // Если флаг programmaticScrollInProgress.value == true, это означает, что
        // ViewModel -> PagerState эффект все еще считает, что он управляет скроллом.
        // Мы НЕ должны вызывать onPageChanged, так как это может быть промежуточное settledPage
        // во время анимации, инициированной ViewModel, или состояние сразу после отмены.
        // Флаг будет сброшен в finally того эффекта.
        if (programmaticScrollInProgress.value) {
            Log.d(
                "WeatherScreen",
                "Programmatic scroll is (or was just) in progress. Ignoring this settledPage ($settledPage) change to avoid loop with VM index $vmCurrentIndex."
            )
            // Если settledPage совпадает с тем, куда мы программно скроллили (vmCurrentIndex),
            // и флаг все еще true, это нормально, он скоро сбросится.
            // Если не совпадает, значит скролл был прерван/не удался, флаг тоже скоро сбросится.
            // В любом случае, ждем сброса флага, прежде чем реагировать на settledPage.
            return@LaunchedEffect
        }

        // Если programmaticScrollInProgress.value == false, значит:
        // 1. Программного скролла не было, и это действие пользователя.
        // 2. Программный скролл был, но он уже полностью завершился (включая блок finally и сброс флага).
        //    Теперь мы можем безопасно обработать settledPage.
        if (pagerItemsList.isNotEmpty() && settledPage >= 0 && settledPage < pagerItemsList.size) {
            if (settledPage != vmCurrentIndex) {
                Log.d(
                    "WeatherScreen",
                    "User action OR post-programmatic settle: Pager settled at $settledPage (VM index $vmCurrentIndex). Informing ViewModel."
                )
                viewModel.onPageChanged(settledPage)
            } else {
                Log.d(
                    "WeatherScreen",
                    "Settled page $settledPage matches VM index ($vmCurrentIndex) and programmatic scroll flag is false. No action to ViewModel needed."
                )
            }
        } else if (pagerItemsList.isEmpty() && settledPage == 0) {
            Log.d(
                "WeatherScreen",
                "Pager settled at page 0, list is empty (and programmatic scroll flag is false). No action to VM."
            )
        } else if (settledPage >= pagerItemsList.size && pagerItemsList.isNotEmpty()) {
            Log.w(
                "WeatherScreen",
                "Settled page $settledPage is out of bounds (${pagerItemsList.size}) (and programmatic scroll flag is false). This shouldn't happen."
            )
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsState() // Стан для Pull-to-Refresh

    // Стан для PullRefresh
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshCurrentPageWeather() }
    )

    Scaffold(
        topBar = {
            val currentActiveItem by viewModel.currentActivePagerItem.collectAsState()
            val weatherDataMapValue by viewModel.weatherDataStateMap.collectAsState()

            val displayTitle = currentActiveItem?.displayName ?: "WeatherML"
            val currentItemId = currentActiveItem?.id
            val weatherStateForCurrentPage =
                currentItemId?.let { weatherDataMapValue[it] } // Безпечне отримання

            // TopAppBar показується ТІЛЬКИ ЯКЩО:
            // 1. НЕ показується UI помилки дозволів (showPermissionRationale == false)
            // 2. Є активна сторінка пейджера (currentActiveItem != null)
            // 3. Стан погоди для цієї активної сторінки - Resource.Success
            val canShowTopBar = !showPermissionRationale &&
                    currentActiveItem != null &&
                    weatherStateForCurrentPage is Resource.Success<*>
            // Додатково можна перевірити, чи геолокація не в стані isLoadingDetails,
            // якщо ти не хочеш TopAppBar під час завантаження назви міста для геолокації:
            // && !(currentActiveItem is PagerItem.GeolocationPage && (currentActiveItem as PagerItem.GeolocationPage).isLoadingDetails)


            if (canShowTopBar) {
                Surface(
                    shadowElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                ) {
                    TopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) { // Обгортаємо в Column
                                Text(
                                    displayTitle, // displayTitle вже враховує currentActiveItem
                                    style = MaterialTheme.typography.headlineSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                // Показуємо індикатор тільки якщо є що показувати і дані завантажені
                                if (pagerItemsList.size > 1 && weatherStateForCurrentPage is Resource.Success<*>) {
                                    PageIndicator(
                                        numberOfPages = pagerItemsList.size,
                                        currentPage = pagerState.currentPage,
                                        modifier = Modifier.padding(top = 8.dp), // Мінімальний відступ
                                        indicatorSize = 6.dp, // Дуже маленький розмір
                                        spacing = 4.dp,       // Дуже маленькі відступи
                                        activeColor = MaterialTheme.colorScheme.onPrimaryContainer, // Або інший колір, що добре видно на фоні TopAppBar
                                        inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.6f
                                        )
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = onNavigateToManageCities) {
                                Image(
                                    painter = painterResource(id = R.drawable.list_option),
                                    contentDescription = "Manage Locations",
                                    modifier = Modifier.size(32.dp),
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                                )
                            }
                            // --- НОВА ІКОНКА ТА ДІЯ ДЛЯ НАЛАШТУВАНЬ ---
                            IconButton(onClick = onNavigateToSettings) { // <--- ВИКЛИКАЄМО НОВУ ЛЯМБДУ
                                Image(
                                    painter = painterResource(id = R.drawable.settings),
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(25.dp), // Можна зробити трохи меншою або такою ж
                                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                                )
                            }
                            // --- КІНЕЦЬ НОВОЇ ІКОНКИ ---
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { scaffoldPaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPaddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            // Визначаємо, чи потрібно показувати UI помилки дозволів
            val geoPageId = PagerItem.GeolocationPage().id
            val permissionErrorMessageFromState =
                (weatherDataMap[geoPageId] as? Resource.Error)?.message
                    ?: (weatherDataMap["initial_perm_error"] as? Resource.Error)?.message
                    // Додай сюди ключ, який використовується в viewModel.setPermissionError, якщо він інший
                    ?: (weatherDataMap["permission_denied_key"] as? Resource.Error)?.message


            if (showPermissionRationale && permissionErrorMessageFromState?.contains(
                    "permission",
                    ignoreCase = true
                ) == true
            ) {
                PermissionErrorUI(
                    message = permissionErrorMessageFromState, // Це вже String
                    onGrantPermission = {
                        // permissionsRequestedThisSession = false // Можна прибрати, якщо логіка в onResult лаунчера працює
                        locationPermissionLauncher.launch(locationPermissions)
                    },
                    onOpenSettings = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", context.packageName, null)
                        intent.data = uri
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("WeatherScreen", "Could not open app settings", e)
                        }
                        showPermissionRationale = false // Скидаємо після переходу в налаштування
                    },
                    context = context,
                    isPermanentlyDenied = permissionErrorMessageFromState.contains(
                        "permanently denied",
                        ignoreCase = true
                    )
                )
            } else if (pagerItemsList.isEmpty()) {
                // Дозволи є (бо showPermissionRationale = false), але список сторінок порожній
                // Це означає, що геолокація ще завантажується або сталася інша помилка
                val loadingOrOtherErrorState = weatherDataMap[geoPageId]
                    ?: Resource.Loading(message = "Initializing location...")

                if (loadingOrOtherErrorState is Resource.Loading || (loadingOrOtherErrorState is Resource.Error && loadingOrOtherErrorState.message?.contains(
                        "permission"
                    ) != true)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            PixelatedSunLoader()
                            Text(
                                (loadingOrOtherErrorState as? Resource.Loading)?.message
                                    ?: "Fetching your location...",
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (loadingOrOtherErrorState is Resource.Error) { // Інша помилка
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            loadingOrOtherErrorState.message ?: "Failed to load location.",
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                // Дозволи є, showPermissionRationale = false, і є сторінки для пейджера
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val pagerItem = pagerItemsList.getOrNull(pageIndex)
                    if (pagerItem != null) {
                        val weatherStateForThisPage by remember(pagerItem.id, weatherDataMap) {
                            derivedStateOf { weatherDataMap[pagerItem.id] ?: Resource.Loading() }
                        }
                        WeatherPageContent(
                            weatherState = weatherStateForThisPage,
                            temperatureUnit = userPreferences.temperatureUnit, // <--- ЗІБРАНИЙ СТАН
                            isGeolocationPageAndLoadingDetails = (pagerItem is PagerItem.GeolocationPage && pagerItem.isLoadingDetails)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) { Text("Loading page data...") }
                    }
                }

                // --- Page Indicator ---
                val currentActivePagerItemForIndicator by viewModel.currentActivePagerItem.collectAsState()
                val weatherStateForActivePage =
                    currentActivePagerItemForIndicator?.id?.let { weatherDataMap[it] }

                // Індикатор Pull-to-Refresh, розміщуємо його зверху по центру
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun WeatherPageContent(
    weatherState: Resource<WeatherDataBundle>,
    temperatureUnit: TemperatureUnit,
    isGeolocationPageAndLoadingDetails: Boolean = false
) {
    if (isGeolocationPageAndLoadingDetails) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PixelatedSunLoader()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Fetching location details...",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    when (weatherState) { // ВИПРАВЛЕНО: використовуємо weatherState
        is Resource.Loading<*> -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PixelatedSunLoader(); Spacer(modifier = Modifier.height(16.dp))
                    weatherState.message?.let { messageText -> // ВИПРАВЛЕНО
                        Text(
                            text = messageText,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center, // Змінено на Center
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
        }

        is Resource.Error<*> -> {
            // Тут не потрібно перевіряти showPermissionRationale, оскільки PermissionErrorUI обробляється окремо
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Error: ${weatherState.message ?: "Unknown error"}", // ВИПРАВЛЕНО
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Justify, // Змінено на Center
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        is Resource.Success<*> -> {
            val bundle = weatherState.data // ВИПРАВЛЕНО
            if (bundle != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CurrentWeatherMainSection(
                        currentWeather = bundle.currentWeather,
                        temperatureUnit = temperatureUnit
                    ) // <--- ПЕРЕДАЄМО
                    // --- Hourly Forecast Section ---
                    PixelArtCard(
                        modifier = Modifier.fillMaxWidth(),
                        internalPadding = 8.dp,
                        borderWidth = 2.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Hourly Forecast (Next 24 Hours)",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                            )
                            if (bundle.hourlyForecasts.isEmpty()) {
                                Text(
                                    "Hourly forecast data is currently unavailable.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                LazyRow {
                                    items(bundle.hourlyForecasts) {
                                        SimpleHourlyForecastItemView(
                                            it,
                                            temperatureUnit
                                        )
                                    }
                                }
                            }
                        }
                    }
                    PixelArtCard(
                        modifier = Modifier.fillMaxWidth(),
                        internalPadding = 8.dp,
                        borderWidth = 2.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Daily Forecast")
                            if (bundle.dailyForecasts.isEmpty()) {
                                Text(
                                    "Daily forecast data is currently unavailable.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                                )
                            } else {
                                // ВИПРАВЛЕНО: Використовуємо Column з forEach для денного прогнозу
                                Column {
                                    bundle.dailyForecasts.forEachIndexed { index, dailyItem ->
                                        SimplifiedDailyForecastItemView(
                                            dailyItem,
                                            temperatureUnit
                                        ) // <--- ПЕРЕДАЄМО
                                        if (index < bundle.dailyForecasts.size - 1) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outline.copy(
                                                    alpha = 0.5f
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // --- Current Weather Details Section ---
                    PixelArtCard(
                        modifier = Modifier.fillMaxWidth(),
                        internalPadding = 16.dp,
                        borderWidth = 2.dp
                    ) {
                        CurrentWeatherDetailsSection(
                            currentWeather = bundle.currentWeather,
                            temperatureUnit = temperatureUnit
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Weather data is currently unavailable.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionErrorUI(
    message: String,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    context: Context,
    isPermanentlyDenied: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (isPermanentlyDenied) {
                        onOpenSettings()
                    } else {
                        onGrantPermission()
                    }
                },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(
                    if (isPermanentlyDenied) "Open Settings" else "Grant Permission",
                    fontFamily = DefaultPixelFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 19.sp, // Adjusted for pixel font
                    lineHeight = 28.sp
                )
            }
        }
    }
}
