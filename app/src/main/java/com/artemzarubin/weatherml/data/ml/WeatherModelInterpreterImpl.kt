@file:Suppress("ConstPropertyName")

package com.artemzarubin.weatherml.data.ml

import android.content.Context
import android.icu.util.Calendar
import android.util.Log
import com.artemzarubin.weatherml.domain.ml.ModelInput
import com.artemzarubin.weatherml.domain.ml.ModelOutput
import com.artemzarubin.weatherml.domain.ml.WeatherModelInterpreter
import com.artemzarubin.weatherml.domain.model.CurrentWeather
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Suppress("SameParameterValue")
@Singleton
class WeatherModelInterpreterImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WeatherModelInterpreter {

    private var interpreter: Interpreter? = null
    private var isInitialized = false
    private val modelFileName = "feels_like_final_model.tflite" // UPDATED
    private val featureNamesFileName = "feels_like_final_feature_names.txt" // UPDATED

    // --- ЗАМІНИ ЦІ РЯДКИ НА ТІ, ЩО ОТРИМАВ З PYTHON ---
    private val scalerXMean: FloatArray =
        floatArrayOf(11.944444f, 0.7347206f, 10.816724f, 10.358285f, 1002.91815f)
    private val scalerXScale: FloatArray =
        floatArrayOf(9.539306f, 0.19536608f, 6.926191f, 4.188632f, 118.219505f)

    private val numericFeatureNamesForXScaling: List<String> = listOf(
        "Temperature (C)",
        "Humidity",
        "Wind Speed (km/h)",
        "Visibility (km)",
        "Pressure (millibars)"
    )

    private companion object {
        private const val scalerYMean: Float = 10.870106f // ЗАМІНИ НА ЗНАЧЕННЯ З PYTHON
        private const val scalerYScale: Float = 10.6818f // ЗАМІНИ НА ЗНАЧЕННЯ З PYTHON
    }
    // --- КІНЕЦЬ НОВИХ ПАРАМЕТРІВ СКЕЙЛЕРА ---

    private var fullFeatureOrderFromTraining: List<String> = emptyList()
    private var numTotalFeaturesExpectedByModel: Int = -1

    private val numBytesPerFloat = 4

    override fun initialize(): Boolean {
        if (isInitialized) return true
        return try {
            val modelBuffer = loadModelFile(modelFileName)
            if (modelBuffer == null) {
                Log.e("ModelInt", "Model file '$modelFileName' not loaded."); return false
            }

            fullFeatureOrderFromTraining = loadFeatureNames(featureNamesFileName)
            if (fullFeatureOrderFromTraining.isEmpty()) {
                Log.e(
                    "ModelInt",
                    "Feature names file '$featureNamesFileName' not loaded or empty."
                ); return false
            }
            numTotalFeaturesExpectedByModel = fullFeatureOrderFromTraining.size
            Log.i(
                "ModelInt",
                "Model expects $numTotalFeaturesExpectedByModel features from '$featureNamesFileName'"
            )

            if (scalerXMean.size != numericFeatureNamesForXScaling.size || scalerXScale.size != numericFeatureNamesForXScaling.size) {
                Log.e(
                    "ModelInt",
                    "Scaler X params size mismatch! Expected ${numericFeatureNamesForXScaling.size}, Mean: ${scalerXMean.size}, Scale: ${scalerXScale.size}"
                )
                return false
            }
            val missingNumericFeatures = numericFeatureNamesForXScaling.filterNot { simpleName ->
                fullFeatureOrderFromTraining.any { modelFeatureName ->
                    // Проверяем, заканчивается ли полное имя фичи из модели на "простое" имя
                    // ИЛИ проверяем прямое совпадение (на случай если префикса нет)
                    modelFeatureName.endsWith(
                        simpleName,
                        ignoreCase = false
                    ) || modelFeatureName == simpleName
                }
            }
            if (missingNumericFeatures.isNotEmpty()) {
                Log.e(
                    "ModelInt",
                    "Critical Error: Numeric features for scaling NOT in full feature list (after considering prefixes): $missingNumericFeatures. Full list: $fullFeatureOrderFromTraining"
                )
                // return false // Пока закомментируем, чтобы увидеть другие ошибки, если есть
            }

            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)
            isInitialized = true
            Log.i(
                "ModelInt",
                "'Feels Like' Model Interpreter initialized. Total Features: $numTotalFeaturesExpectedByModel, Scaled X Features: ${numericFeatureNamesForXScaling.size}"
            )
            true
        } catch (e: Exception) {
            Log.e("ModelInt", "Error initializing 'Feels Like' Interpreter: ${e.message}", e)
            false
        }
    }

    private fun loadFeatureNames(fileName: String): List<String> {
        return try {
            context.assets.open(fileName).bufferedReader().useLines { lines ->
                lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
            }
        } catch (e: IOException) {
            Log.e("ModelInterpreter", "Error loading feature names from $fileName", e)
            emptyList()
        }
    }

    private fun loadModelFile(modelFilename: String): ByteBuffer? {
        return try {
            context.assets.openFd(modelFilename).use { fileDescriptor ->
                FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                    val fileChannel = inputStream.channel
                    val startOffset = fileDescriptor.startOffset
                    val declaredLength = fileDescriptor.declaredLength
                    fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
                        .apply { order(ByteOrder.nativeOrder()) }
                }
            }
        } catch (e: IOException) {
            Log.e("ModelInterpreter", "Error loading model '$modelFilename' from assets.", e)
            null
        }
    }

    override fun prepareAndScaleFeatures(currentWeather: CurrentWeather): FloatArray? {
        if (fullFeatureOrderFromTraining.isEmpty()) {
            Log.e("ModelInt", "Feature order not loaded, cannot prepare features.")
            return null
        }
        if (numTotalFeaturesExpectedByModel == -1) {
            Log.e("ModelInt", "Model not initialized, cannot determine expected feature count.")
            return null
        }

        val featureMap = mutableMapOf<String, Float>()

        // 1. Числовые признаки - получаем сырые значения
        val rawTemperature = currentWeather.temperatureCelsius.toFloat()
        val rawHumidity = currentWeather.humidityPercent.toFloat()
        // Важно: в Python было 0.73 для влажности, а API дает 0-100.
        // Если скейлер обучался на влажности 0-1, нужно делить rawHumidity / 100.0f
        // Судя по scalerXMean[1] = 0.73472059f, ваша модель ОЖИДАЕТ ВЛАЖНОСТЬ В ДИАПАЗОНЕ 0-1.
        val rawWindSpeedKmh = (currentWeather.windSpeedMps * 3.6).toFloat()
        val rawVisibilityKm = currentWeather.visibilityMeters.toFloat() / 1000.0f
        val rawPressureHpa = currentWeather.pressureHpa.toFloat()

        val rawNumericValues = listOf(
            rawTemperature,
            rawHumidity / 100.0f,
            rawWindSpeedKmh,
            rawVisibilityKm,
            rawPressureHpa
        ) // ДЕЛИМ ВЛАЖНОСТЬ!

        // Масштабируем числовые признаки
        for (i in numericFeatureNamesForXScaling.indices) { // Используем правильное имя
            val simpleName = numericFeatureNamesForXScaling[i] // Используем правильное имя
            val fullNameInModel = fullFeatureOrderFromTraining.find { modelFeatureName ->
                modelFeatureName.endsWith(
                    simpleName,
                    ignoreCase = false
                ) || modelFeatureName == simpleName
            }
            if (fullNameInModel != null) {
                val rawValue = rawNumericValues[i]
                val mean = scalerXMean[i]
                val scale =
                    if (scalerXScale[i] == 0.0f || scalerXScale[i].isNaN()) 1.0f else scalerXScale[i] // Защита от деления на ноль/NaN
                featureMap[fullNameInModel] = (rawValue - mean) / scale
                Log.d(
                    "ModelIntScale",
                    "Scaling $simpleName: ($rawValue - $mean) / $scale = ${featureMap[fullNameInModel]}"
                )
            } else {
                Log.w(
                    "ModelIntScale",
                    "Numeric feature to scale '$simpleName' not found in full model feature list."
                )
            }
        }


        // 2. Часовые признаки (Sin/Cos)
        val (localHour, localMonth, dayOfYear) = getLocalTimeFeatures(
            currentWeather.dateTimeMillis,
            currentWeather.timezoneOffsetSeconds
        )
        featureMap["remainder__HourSin"] = sin(2 * PI * localHour.toDouble() / 24.0).toFloat()
        featureMap["remainder__HourCos"] = cos(2 * PI * localHour.toDouble() / 24.0).toFloat()
        featureMap["remainder__MonthSin"] = sin(2 * PI * localMonth.toDouble() / 12.0).toFloat()
        featureMap["remainder__MonthCos"] = cos(2 * PI * localMonth.toDouble() / 12.0).toFloat()
        featureMap["remainder__DayOfYearSin"] = sin(2 * PI * dayOfYear.toDouble() / 365.0).toFloat()
        featureMap["remainder__DayOfYearCos"] = cos(2 * PI * dayOfYear.toDouble() / 365.0).toFloat()

        // 3. Напрямок вітру (Sin/Cos)
        val windBearingRad = Math.toRadians(currentWeather.windDirectionDegrees.toDouble())
        featureMap["remainder__WindBearingSin"] = sin(windBearingRad).toFloat()
        featureMap["remainder__WindBearingCos"] = cos(windBearingRad).toFloat()

        // 4. One-Hot Encoded 'Precip Type'
        val currentPrecipType = currentWeather.weatherCondition
        featureMap["cat__Precip Type_rain"] =
            if (currentPrecipType.contains("rain", ignoreCase = true)) 1.0f else 0.0f
        featureMap["cat__Precip Type_snow"] =
            if (currentPrecipType.contains("snow", ignoreCase = true)) 1.0f else 0.0f

        // Формируем финальный массив в правильном порядке
        val featuresArray = FloatArray(numTotalFeaturesExpectedByModel)
        for (i in fullFeatureOrderFromTraining.indices) {
            val featureName = fullFeatureOrderFromTraining[i]
            featuresArray[i] = featureMap[featureName]
                ?: run {
                    Log.e(
                        "ModelIntPrep",
                        "CRITICAL ERROR: Feature '$featureName' was NOT found in featureMap! Using 0.0f."
                    )
                    0.0f // Или вернуть null, если это критично
                }
        }
        Log.d(
            "ModelIntPrep",
            "'Feels Like' Model - Prepared & Scaled ${featuresArray.size} features. Values: ${featuresArray.joinToString()}"
        )
        return featuresArray
    }

    // Вспомогательная функция, если ее нет
    private fun getLocalTimeFeatures(
        dateTimeMillisUTC: Long,
        timezoneOffsetSeconds: Int
    ): Triple<Int, Int, Int> {
        val calendar =
            Calendar.getInstance(android.icu.util.TimeZone.getTimeZone("UTC")) // <--- ИСПРАВЛЕНО
        calendar.timeInMillis = dateTimeMillisUTC
        calendar.add(Calendar.SECOND, timezoneOffsetSeconds)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val month = calendar.get(Calendar.MONTH) + 1 // 1-12
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR) // 1-365/366
        return Triple(hour, month, dayOfYear)
    }

    override fun getPrediction(input: ModelInput): ModelOutput? {
        if (!isInitialized && !initialize()) { // Убедимся, что инициализация прошла
            Log.e("ModelInt", "Interpreter not initialized, cannot get prediction.")
            return null
        }
        if (interpreter == null) {
            Log.e("ModelInt", "Interpreter is null after initialization check.")
            return null
        }
        if (input.features.size != numTotalFeaturesExpectedByModel) {
            Log.e(
                "ModelInt",
                "Feature size mismatch. Input: ${input.features.size}, Expected: $numTotalFeaturesExpectedByModel"
            )
            return null
        }

        return try {
            // val processedFeatures = input.features.clone() // Клонирование не нужно, если prepareAndScaleFeatures возвращает новый массив
            // --- СТАРЫЙ КОД МАСШТАБИРОВАНИЯ X ЗДЕСЬ УДАЛЯЕТСЯ ---

            val inputBuffer =
                ByteBuffer.allocateDirect(1 * numTotalFeaturesExpectedByModel * numBytesPerFloat)
                    .apply {
                        order(ByteOrder.nativeOrder()); rewind(); asFloatBuffer().put(input.features) // Используем напрямую input.features
                    }
            val outputArray = Array(1) { FloatArray(1) }
            interpreter?.run(inputBuffer, outputArray)

            val predictedFeelsLikeScaled = outputArray[0][0]
            val predictedFeelsLikeActual = (predictedFeelsLikeScaled * scalerYScale) + scalerYMean

            Log.d(
                "ModelInt",
                "'Feels Like' Model - Input to TF Lite (first 5): ${
                    input.features.take(5).joinToString()
                }, Scaled Pred: $predictedFeelsLikeScaled, Actual Pred: $predictedFeelsLikeActual"
            )
            ModelOutput(predictedFeelsLikeTemp = predictedFeelsLikeActual)
        } catch (e: Exception) {
            Log.e("ModelInt", "Error during 'Feels Like' model inference: ${e.message}", e)
            null
        }
    }

    override fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
        Log.d("ModelInterpreter", "Interpreter closed.")
    }
}