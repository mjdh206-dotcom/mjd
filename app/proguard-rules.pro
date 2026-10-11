# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.mjd.videoforge.data.model.** { *; }
-keep class com.mjd.videoforge.providers.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# FFmpeg Kit
-keep class com.arthenica.ffmpegkit.** { *; }
