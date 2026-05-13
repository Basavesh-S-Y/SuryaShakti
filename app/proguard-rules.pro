# Add project specific ProGuard rules here.
# https://www.guardsquare.com/manual/configuration/usage

# Keep Room entities
-keep class com.suryashakti.solarmonitor.data.** { *; }

# Keep ViewModels
-keep class com.suryashakti.solarmonitor.viewmodel.** { *; }

# Keep Kotlin data classes
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# WorkManager
-keep class androidx.work.** { *; }
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Navigation
-keepnames class androidx.navigation.fragment.NavHostFragment

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# General Android
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
