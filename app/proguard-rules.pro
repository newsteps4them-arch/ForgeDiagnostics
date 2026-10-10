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
# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep class com.forge.app.services.GitHubReleaseApiService { *; }
-keep class com.forge.app.services.AlldataApiService { *; }
-keep class com.forge.app.services.NexpartApiService { *; }
-keep class com.forge.app.services.JulesSourceApiService { *; }
-keep class com.forge.app.services.GroqApiService { *; }
-keep class com.forge.app.services.OpenRouterApiService { *; }
-keep class com.forge.app.services.CloudConnectorsApiService { *; }
-keep class com.forge.app.services.JulesRetrofitApi { *; }

-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class kotlinx.serialization.** { *; }
