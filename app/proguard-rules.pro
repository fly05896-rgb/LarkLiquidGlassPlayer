-keepattributes *Annotation*,SourceFile,LineNumberTable
-keep class com.kyant.backdrop.** { *; }
-keep class com.kyant.shapes.** { *; }
-keep class com.larkliquid.player.engine.EgNativeEngine { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class androidx.media3.** { *; }
-dontwarn kotlin.**
