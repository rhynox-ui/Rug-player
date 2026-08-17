# Add project specific ProGuard rules here.
-dontwarn org.checkerframework.**
-keepattributes *Annotation*

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }

# Room entities
-keep class com.rugplayer.app.data.db.** { *; }

# Kotlinx serialization
-keepattributes InnerClasses
-keep,includedescriptorclasses class com.rugplayer.app.**$$serializer { *; }
-keepclassmembers class com.rugplayer.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.rugplayer.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
