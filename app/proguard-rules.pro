# General Android rules often recommended
-dontobfuscate # If you don’t want obfuscation during development/testing phase of release,
               # but for the final release to Play Market, it's better to enable it (comment out this line)
               # When enabled, class, method, and field names are changed to short ones (a, b, c),
               # which complicates reverse engineering, but also makes crash reports harder to analyze.
               # If you keep -dontobfuscate, then isMinifyEnabled = true will mostly perform shrinking (code removal).

-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/* # Example of disabling aggressive optimizations if they cause issues

-keepattributes Signature # Keeps generic types for reflection
-keepattributes InnerClasses # Keeps info about inner and anonymous classes
-keepattributes *Annotation* # Keeps annotations (important for Hilt, Room, Kotlinx Serialization, Retrofit)

# --- Kotlin Standard Library and Coroutines ---
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; } # Needed for Kotlin reflection
-keepclassmembers class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    <fields>;
    <init>(...); # Keep constructors
}
-keep class kotlin.coroutines.jvm.internal.DebugProbesKt { *; }
-keepclassmembers class kotlin.Result { *; } # Keep Result class
-keepclassmembers class kotlin.DeepRecursiveScope { *; } # For deep recursion
-keepclassmembers class kotlin.DeepRecursiveFunction { *; }

# For kotlinx.coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# --- Kotlinx Serialization ---
# These rules are critical if you're using Kotlinx Serialization
-keepclassmembers class kotlinx.serialization.internal.* {
    *;
}
-keep class **$$serializer { *; } # Keeps generated serializers
-keepnames class * implements kotlinx.serialization.KSerializer
-keepnames class * { @kotlinx.serialization.Serializable <fields>; } # Keeps fields of classes annotated with @Serializable
-if class ** { @kotlinx.serialization.Serializable *; } -keep class <1>$$serializer { *; }

# For enum classes annotated with @Serializable
-keepclassmembers enum * {
    @kotlinx.serialization.SerialName <fields>;
    <methods>; # Keep methods, including values() and valueOf()
}

# --- Retrofit & OkHttp ---
# If your DTOs (Data Transfer Objects) are properly annotated with @Serializable,
# the rules for Kotlinx Serialization above should cover them.
# If not, or if you're using other converters (like Gson), other rules are needed.

# Keep DTOs if not covered by serialization rules
-keep class com.artemzarubin.weatherml.data.remote.dto.** { *; }
-keep interface com.artemzarubin.weatherml.data.remote.dto.** { *; }

# For Retrofit (usually not much is needed if DTOs are kept)
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepattributes Exceptions # For Retrofit methods that throw exceptions

# For OkHttp (usually not needed unless using reflection with it)
-dontwarn okhttp3.**
-dontwarn okio.**

# --- Hilt (Dependency Injection) ---
# Hilt usually auto-generates the needed rules. These are backup or for resolving issues.
-keep class dagger.hilt.android.internal.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ActivityRetainedComponentManager$LifecycleModule { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedEntryPoint { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { <init>(); }
-keep @dagger.hilt.android.AndroidEntryPoint class * { <init>(); }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { <init>(...); } # Keep ViewModel constructors
-keepclassmembers class * { @javax.inject.Inject <init>(...); } # Keep constructors annotated with @Inject
-keepclassmembers class * { @dagger.hilt.android.AndroidEntryPoint *; }
-keepclassmembers class * { @dagger.hilt.android.lifecycle.HiltViewModel *; }
-keepclassmembers @dagger.Module class * { <methods>; } # Keep methods in Hilt modules
-keepclassmembers @dagger.Provides class * { <methods>; } # Keep methods annotated with @Provides
-keepclassmembers @dagger.Binds class * { <methods>; }   # Keep methods annotated with @Binds

# --- Room Persistence Library ---
# KSP for Room generates code that usually doesn’t need special ProGuard rules,
# unless you use reflection with Entity or Dao.
# But for backup you can add:
-keep class androidx.room.** { *; }
-keep class com.artemzarubin.weatherml.data.local.** { *; } # Keep your Entity, Dao, Database
-keep interface com.artemzarubin.weatherml.data.local.** { *; }

# --- Jetpack Compose ---
# Compose rules are usually included by the Android Gradle Plugin automatically.
# If you encounter issues related to Compose in release builds,
# you might need to add specific rules, but it’s rare.
# -keepclassmembers class * { @androidx.compose.runtime.Composable <methods>; }
# -keepclassmembers class * { @androidx.compose.runtime.Composable <fields>; }

# --- TensorFlow Lite ---
# If you just load a .tflite file and use the standard Interpreter,
# special rules are usually not required.
# If you use the TensorFlow Lite Support Library for specific models or pre/post-processing,
# rules might be required to keep those classes.
# -keep class org.tensorflow.lite.support.** { *; } # Example if using Support Library

# --- Your own data/model classes that may be used for serialization or reflection ---
# If your domain models are not annotated with @Serializable and not used directly by Retrofit/Room,
# but are serialized or reflected otherwise, you also need to keep them.
-keep class com.artemzarubin.weatherml.domain.model.** { *; }
-keep interface com.artemzarubin.weatherml.domain.model.** { *; }

# --- Rules for AndroidX Core, Lifecycle, Activity (usually not needed but for safety) ---
-keep class androidx.core.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.activity.** { *; }

# --- Rules for Google Play Services Location (FusedLocationProviderClient) ---
# Usually doesn’t need special rules if used in standard way.
-keep class com.google.android.gms.location.** { *; }

# --- DataStore Preferences ---
# If you use standard keys and types, special rules are usually not needed.
# If you use custom serializers for DataStore, you need to keep them.
-keep class androidx.datastore.preferences.core.** { *; }

# --- Don’t forget to add rules for any other libraries you use ---
# --- especially if they use reflection or you see crashes in release builds ---

# Example of ignoring warnings for certain libraries (use with caution):
# -dontwarn com.example.somelibrary.**