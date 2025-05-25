@file:Suppress("KotlinConstantConditions")

package com.artemzarubin.weatherml.ui.mainscreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
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
    ExperimentalMaterialApi::class
)
@Composable
fun WeatherScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToManageCities: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val pagerItemsList by viewModel.pagerItems.collectAsState()
    val currentPagerIndexFromVM by viewModel.currentPagerIndex.collectAsState()
    val weatherDataMap by viewModel.weatherDataStateMap.collectAsState()
    val userPreferences by viewModel.userPreferencesFlow.collectAsState()

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

    // --- НОВЫЕ СОСТОЯНИЯ ДЛЯ GPS ---
    var showGpsDisabledErrorScreen by rememberSaveable { mutableStateOf(false) }
    var isLoadingAfterGpsEnabled by rememberSaveable { mutableStateOf(false) }
    // --- КОНЕЦ НОВЫХ СОСТОЯНИЙ ---

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            permissionsRequestedThisSession = true

            if (fineLocationGranted || coarseLocationGranted) {
                showPermissionRationale = false
                // Проверяем GPS сразу после предоставления разрешений
                val locationManager =
                    context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val isGpsCurrentlyEnabled =
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                if (isGpsCurrentlyEnabled) {
                    showGpsDisabledErrorScreen = false
                    isLoadingAfterGpsEnabled = true // Показать загрузку перед получением данных
                    viewModel.handlePermissionAndGpsGranted() // Новый или обновленный метод в ViewModel
                } else {
                    // Разрешения есть, но GPS выключен
                    showGpsDisabledErrorScreen = true
                    isLoadingAfterGpsEnabled = false
                    // ViewModel должен установить ошибку "GPS is disabled" в weatherDataMap
                    viewModel.forceGpsDisabledError()
                }
            } else {
                val activity = context as? ComponentActivity
                val shouldShowRationaleAfterThisAttempt = locationPermissions.any { perm ->
                    activity?.shouldShowRequestPermissionRationale(perm) ?: false
                }
                val isPermanentlyDeniedNow = !shouldShowRationaleAfterThisAttempt
                Log.d(
                    "WeatherScreen",
                    "PermissionResult: Denied. ShouldShowRationaleAfter: $shouldShowRationaleAfterThisAttempt, IsPermanentlyDeniedNow: $isPermanentlyDeniedNow"
                )
                viewModel.setPermissionError(
                    if (isPermanentlyDeniedNow) "Location permission permanently denied. Please enable it in app settings."
                    else "Location permission denied. Click to try again."
                )
                showPermissionRationale = true
            }
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    val geoPageId = remember { PagerItem.GeolocationPage().id } // Запоминаем ID для геолокации

    // --- НОВЫЙ/ОБНОВЛЕННЫЙ LaunchedEffect для управления isLoadingAfterGpsEnabled и showGpsDisabledErrorScreen ---
    LaunchedEffect(
        weatherDataMap,
        geoPageId,
        permissionsRequestedThisSession
    ) { // Добавим permissionsRequestedThisSession для реакции
        val geoState = weatherDataMap[geoPageId]
        val currentPermissionsGranted = hasLocationPermission(context)
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsActuallyEnabled =
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (isLoadingAfterGpsEnabled) {
            // ... (логика для isLoadingAfterGpsEnabled остается)
            when (geoState) {
                is Resource.Success -> {
                    Log.d(
                        "WeatherScreen",
                        "GeoState is Success. isLoadingAfterGpsEnabled: true -> false."
                    )
                    isLoadingAfterGpsEnabled = false
                    showGpsDisabledErrorScreen = false
                }

                is Resource.Error -> {
                    Log.d(
                        "WeatherScreen",
                        "GeoState is Error ('${geoState.message}'). isLoadingAfterGpsEnabled: true -> false."
                    )
                    isLoadingAfterGpsEnabled = false
                    // Если ошибка "GPS disabled" и разрешения есть, показываем экран ошибки GPS
                    if (geoState.message?.contains(
                            "GPS is disabled",
                            ignoreCase = true
                        ) == true && currentPermissionsGranted
                    ) {
                        Log.d(
                            "WeatherScreen",
                            "Error is GPS Disabled with permissions. showGpsDisabledErrorScreen: false -> true."
                        )
                        showGpsDisabledErrorScreen = true
                    } else {
                        showGpsDisabledErrorScreen = false
                    }
                }

                is Resource.Loading -> {
                    Log.d(
                        "WeatherScreen",
                        "GeoState is still Loading. isLoadingAfterGpsEnabled remains true."
                    )
                }

                null -> {
                    Log.d(
                        "WeatherScreen",
                        "GeoState is null. isLoadingAfterGpsEnabled remains true if it was true."
                    )
                }
            }
        } else { // isLoadingAfterGpsEnabled == false
            if (currentPermissionsGranted && !isGpsActuallyEnabled) {
                // Если разрешения есть, но GPS ФАКТИЧЕСКИ выключен,
                // и ViewModel еще не успел выставить ошибку "GPS is disabled" или она была перезаписана,
                // принудительно показываем экран ошибки GPS.
                if (!showGpsDisabledErrorScreen) {
                    Log.w(
                        "WeatherScreen",
                        "Permissions granted, GPS OFF, but showGpsDisabledErrorScreen is false. Forcing true."
                    )
                    showGpsDisabledErrorScreen = true
                    viewModel.forceGpsDisabledError() // Попросим ViewModel также установить ошибку
                }
                if (showPermissionRationale) showPermissionRationale = false
            } else if (geoState is Resource.Error && geoState.message?.contains(
                    "GPS is disabled",
                    ignoreCase = true
                ) == true && currentPermissionsGranted
            ) {
                // Если ViewModel явно сообщает об ошибке GPS
                if (!showGpsDisabledErrorScreen) {
                    Log.d(
                        "WeatherScreen",
                        "ViewModel reports GPS Disabled error. showGpsDisabledErrorScreen: false -> true."
                    )
                    showGpsDisabledErrorScreen = true
                }
                if (showPermissionRationale) showPermissionRationale = false
            } else {
                // Во всех остальных случаях, если showGpsDisabledErrorScreen был true, но условия больше не выполняются, скрываем его.
                if (showGpsDisabledErrorScreen) {
                    // Скрываем только если GPS включен ИЛИ ошибка не "GPS disabled"
                    if (isGpsActuallyEnabled || !(geoState is Resource.Error && geoState.message?.contains(
                            "GPS is disabled",
                            ignoreCase = true
                        ) == true)
                    ) {
                        Log.d(
                            "WeatherScreen",
                            "Condition for GPS error no longer met. showGpsDisabledErrorScreen: true -> false."
                        )
                        showGpsDisabledErrorScreen = false
                    }
                }
            }
        }
    }
    // --- КОНЕЦ НОВОГО/ОБНОВЛЕННОГО LaunchedEffect ---


    DisposableEffect(
        lifecycleOwner,
        context
    ) { // Добавляем context как ключ, если он используется внутри
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                Log.d(
                    "WeatherScreen",
                    "ON_START. PermRequestedThisSession: $permissionsRequestedThisSession, Current ShowRationale: $showPermissionRationale, ShowGpsError: $showGpsDisabledErrorScreen"
                )
                val permissionsGranted = hasLocationPermission(context)
                val activity = context as? ComponentActivity

                if (permissionsGranted) {
                    Log.d("WeatherScreen", "Permissions GRANTED on ON_START.")
                    val locationManager =
                        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val isGpsCurrentlyEnabled =
                        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    Log.d("WeatherScreen", "ON_START: GPS Enabled: $isGpsCurrentlyEnabled")

                    if (isGpsCurrentlyEnabled) {
                        // GPS включен
                        if (showGpsDisabledErrorScreen) { // Если ранее был показан экран ошибки GPS
                            Log.d(
                                "WeatherScreen",
                                "ON_START: GPS was disabled, now enabled. Refreshing location."
                            )
                            showGpsDisabledErrorScreen = false
                            isLoadingAfterGpsEnabled = true // Показать экран загрузки
                            viewModel.handlePermissionAndGpsGranted() // Запросить данные
                        } else if (!pagerItemsList.any { it.id == geoPageId && weatherDataMap[geoPageId] is Resource.Success }) {
                            // Если экран ошибки GPS не показывался, но данных по геолокации нет (или они не успешны)
                            // и мы не в процессе загрузки после включения GPS
                            if (!isLoadingAfterGpsEnabled) {
                                Log.d(
                                    "WeatherScreen",
                                    "ON_START: GPS enabled, no GPS error screen, but geo data might be missing/stale. Triggering refresh."
                                )
                                isLoadingAfterGpsEnabled = true // Показать экран загрузки
                                viewModel.handlePermissionAndGpsGranted()
                            }
                        }
                        // Если разрешения были только что предоставлены из настроек и showPermissionRationale был true
                        if (showPermissionRationale) {
                            Log.d(
                                "WeatherScreen",
                                "Hiding permission rationale UI as permissions are now granted (and GPS checked)."
                            )
                            showPermissionRationale = false
                        }
                    } else {
                        // GPS выключен (а разрешения есть)
                        Log.d(
                            "WeatherScreen",
                            "ON_START: Permissions GRANTED, but GPS is DISABLED."
                        )
                        showGpsDisabledErrorScreen = true // Показать экран ошибки GPS
                        isLoadingAfterGpsEnabled = false
                        showPermissionRationale = false // Ошибка GPS приоритетнее
                        viewModel.forceGpsDisabledError() // Сообщить ViewModel, чтобы он выставил соответствующую ошибку
                    }
                } else { // Разрешения НЕ предоставлены
                    Log.d("WeatherScreen", "Permissions NOT GRANTED on ON_START.")
                    if (!permissionsRequestedThisSession) {
                        Log.d(
                            "WeatherScreen",
                            "ON_START: Requesting permissions for the first time this session."
                        )
                        locationPermissionLauncher.launch(locationPermissions)
                    } else {
                        val canStillRequestRationale = locationPermissions.any { perm ->
                            activity?.shouldShowRequestPermissionRationale(perm) ?: false
                        }
                        val isEffectivelyPermanentlyDenied = !canStillRequestRationale
                        Log.d(
                            "WeatherScreen",
                            "ON_START: Already requested this session. CanStillRequestRationale: $canStillRequestRationale, IsEffectivelyPermanentlyDenied: $isEffectivelyPermanentlyDenied"
                        )

                        if (isEffectivelyPermanentlyDenied) {
                            viewModel.setPermissionError("Location permission permanently denied. Please enable it in app settings.")
                        } else {
                            viewModel.setPermissionError("Location permission denied. Click to try again.")
                        }
                        showPermissionRationale = true
                        showGpsDisabledErrorScreen =
                            false // Ошибка разрешений приоритетнее, если GPS не проверялся
                        isLoadingAfterGpsEnabled = false
                    }
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

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.refreshCurrentPageWeather() }
    )

    Scaffold(
        topBar = {
            // ... (логика TopAppBar без изменений, но она будет скрыта, если showGpsDisabledErrorScreen = true или showPermissionRationale = true) ...
            val currentActiveItem by viewModel.currentActivePagerItem.collectAsState()
            val weatherDataMapValue by viewModel.weatherDataStateMap.collectAsState()
            val displayTitle = currentActiveItem?.displayName ?: "WeatherML"
            val currentItemId = currentActiveItem?.id
            val weatherStateForCurrentPage = currentItemId?.let { weatherDataMapValue[it] }

            val canShowTopBar =
                !showPermissionRationale && !showGpsDisabledErrorScreen && !isLoadingAfterGpsEnabled &&
                    currentActiveItem != null &&
                    weatherStateForCurrentPage is Resource.Success<*>

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
                .pullRefresh(pullRefreshState) // pullRefreshState должен быть здесь, если контент ниже может его использовать
        ) {
            val permissionErrorMessageFromState =
                (weatherDataMap[geoPageId] as? Resource.Error)?.message
                    ?: (weatherDataMap["initial_perm_error"] as? Resource.Error)?.message
                    ?: (weatherDataMap["permission_denied_key"] as? Resource.Error)?.message

            // --- ЛОГИКА ОТОБРАЖЕНИЯ КОНТЕНТА ---
            when {
                // 1. Экран ошибки GPS (если разрешения есть, но GPS выключен)
                showGpsDisabledErrorScreen && hasLocationPermission(context) -> {
                    GpsDisabledErrorUI(
                        onOpenLocationSettings = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                // Флаг showGpsDisabledErrorScreen будет сброшен в ON_START, если GPS включат
                            } catch (e: Exception) {
                                Log.e(
                                    "WeatherScreen",
                                    "Could not open location settings from GpsDisabledErrorUI",
                                    e
                                )
                            }
                        }
                    )
                }
                // 2. Экран загрузки после включения GPS (или при первоначальной загрузке геолокации)
                isLoadingAfterGpsEnabled -> { // Главное условие для этого экрана
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            PixelatedSunLoader()
                            Text(
                                // Сообщение можно брать из geoState, если он Loading, или общее
                                (weatherDataMap[geoPageId] as? Resource.Loading)?.message
                                    ?: "Loading your location...",
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                // 3. Экран ошибки разрешений
                showPermissionRationale && permissionErrorMessageFromState?.contains(
                    "permission",
                    ignoreCase = true
                ) == true -> {
                    PermissionErrorUI(
                        message = permissionErrorMessageFromState,
                        onGrantPermission = {
                            permissionsRequestedThisSession = false
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
                            showPermissionRationale = false
                        },
                        isPermanentlyDenied = permissionErrorMessageFromState.contains(
                            "permanently denied",
                            ignoreCase = true
                        )
                    )
                }
                // 4. Список страниц пуст (не из-за GPS или разрешений, а, например, другие ошибки или начальная загрузка городов)
                pagerItemsList.isEmpty() -> {
                    val currentGeoState = weatherDataMap[geoPageId]
                    if (currentGeoState is Resource.Loading && hasLocationPermission(context) && !showGpsDisabledErrorScreen && !showPermissionRationale) {
                        // Это случай начальной загрузки, когда isLoadingAfterGpsEnabled еще не успел стать true,
                        // но ViewModel уже начал загрузку геолокации.
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                PixelatedSunLoader()
                                Text(
                                    currentGeoState.message ?: "Fetching your location...",
                                    modifier = Modifier.padding(top = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (currentGeoState is Resource.Error && !currentGeoState.message.isNullOrEmpty() &&
                        !showGpsDisabledErrorScreen && !showPermissionRationale
                    ) {
                        // Показываем другую ошибку, если она не связана с GPS/разрешениями
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                currentGeoState.message,
                                textAlign = TextAlign.Justify,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else if (!hasLocationPermission(context) && !showPermissionRationale) {
                        // Если нет разрешений и не показывается диалог запроса (редкий случай, может быть начальное состояние)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Waiting for location permission...", textAlign = TextAlign.Center)
                        }
                    } else { // Общий случай для пустого списка без явных ошибок/загрузки
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No content available.",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                // 5. Отображение пейджера с погодой
                else -> {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        val pagerItem = pagerItemsList.getOrNull(pageIndex)
                        if (pagerItem != null) {
                            // Сбрасываем isLoadingAfterGpsEnabled, если мы успешно дошли до отображения пейджера
                            // и текущая страница - геолокация, и она успешно загружена или не является ошибкой GPS
                            val weatherStateForThisPageReal by remember(
                                pagerItem.id,
                                weatherDataMap
                            ) {
                                derivedStateOf {
                                    weatherDataMap[pagerItem.id] ?: Resource.Loading()
                                }
                            }
                            if (pagerItem.id == geoPageId && isLoadingAfterGpsEnabled) {
                                if (weatherStateForThisPageReal !is Resource.Loading &&
                                    !(weatherStateForThisPageReal is Resource.Error && weatherStateForThisPageReal.message?.contains(
                                        "GPS is disabled",
                                        ignoreCase = true
                                    ) == true)
                                ) {
                                    LaunchedEffect(Unit) { // Используем LaunchedEffect для изменения состояния в композиции
                                        isLoadingAfterGpsEnabled = false
                                    }
                                }
                            }

                            WeatherPageContent(
                                weatherState = weatherStateForThisPageReal,
                                temperatureUnit = userPreferences.temperatureUnit,
                                isGeolocationPageAndLoadingDetails = (pagerItem is PagerItem.GeolocationPage && pagerItem.isLoadingDetails),
                                pagerItemId = pagerItem.id,
                                onOpenLocationSettings = { // Передаем колбэк дальше
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                    } catch (e: Exception) {
                                        Log.e(
                                            "WeatherScreen",
                                            "Could not open location settings from WeatherPageContent",
                                            e
                                        )
                                    }
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { Text("Loading page data...") }
                        }
                    }
                }
            }

            // Индикатор Pull-to-Refresh (должен быть видим поверх контента, если он есть)
            if (!showGpsDisabledErrorScreen && !showPermissionRationale && (pagerItemsList.isNotEmpty() || isLoadingAfterGpsEnabled)) {
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
    isGeolocationPageAndLoadingDetails: Boolean = false,
    pagerItemId: String,
    onOpenLocationSettings: () -> Unit
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
            val errorMessage = weatherState.message ?: "Unknown error"
            // Проверяем, является ли это ошибкой отключенного GPS для страницы геолокации
            if (pagerItemId == PagerItem.GeolocationPage().id && errorMessage.contains(
                    "GPS is disabled",
                    ignoreCase = true
                )
            ) {
                // Используем GpsDisabledErrorUI из WeatherScreen, но здесь можно передать специфичные параметры, если нужно
                // Однако, основная логика отображения GpsDisabledErrorUI теперь в WeatherScreen
                // Здесь можно показать упрощенное сообщение или специфичный UI для контекста пейджера,
                // но лучше, если WeatherScreen полностью управляет этим экраном ошибки.
                // Для согласованности, если WeatherScreen уже показывает GpsDisabledErrorUI, здесь можно ничего не делать или показать заглушку.
                // Но если мы дошли сюда, значит showGpsDisabledErrorScreen в WeatherScreen = false, что странно.
                // Логичнее, чтобы GpsDisabledErrorUI отображался на уровне WeatherScreen, покрывая все.
                // Поэтому, если мы здесь, и это ошибка GPS, то это неожиданно.
                // Однако, для подстраховки, если WeatherScreen не справился:
                GpsDisabledErrorUI(onOpenLocationSettings = onOpenLocationSettings)
            } else {
                // Обычное отображение ошибки
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Error:",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            errorMessage,
                            textAlign = TextAlign.Justify,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        // Можно добавить кнопку "Повторить" для других типов ошибок
                        // if (!errorMessage.contains("permission", ignoreCase = true)) {
                        //     Button(onClick = { /* viewModel.retryLastFetch() */ }) { Text("Retry") }
                        // }
                    }
                }
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
fun GpsDisabledErrorUI(onOpenLocationSettings: () -> Unit) { // Убираем context, принимаем лямбду
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background), // Добавим фон для перекрытия
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_weather_placeholder), // Замените на свою иконку
            contentDescription = "GPS Disabled Icon",
            modifier = Modifier.size(108.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.gps_disabled_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontFamily = DefaultPixelFontFamily),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.gps_disabled_message),
            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = DefaultPixelFontFamily),
            textAlign = TextAlign.Justify,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onOpenLocationSettings, // Используем переданный колбэк
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(
                stringResource(R.string.open_location_settings),
                fontFamily = DefaultPixelFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun PermissionErrorUI(
    message: String,
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
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
