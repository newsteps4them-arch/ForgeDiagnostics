# Conservative ProGuard/R8 rules for the ForgeDiagnostics release build.
# Correctness first: keep the app's own classes whole, let R8 shrink libraries.

# Keep all app code (models, services, UI). Shrinking still strips unused
# third-party library code, which is where the size win comes from.
-keep class com.forge.app.** { *; }

# kotlinx.serialization: keep generated serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp.
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, Exceptions

# AndroidX / Compose.
-keep class androidx.** { *; }
-dontwarn androidx.**

# Native method names must survive.
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep line numbers for crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
