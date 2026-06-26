-keep class com.rzzisan.carrental.data.network.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
