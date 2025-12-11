# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/jules/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.example.julesapp.api.** { *; }
