# Vosk/JNA use native interfaces and reflective loading.
-keep class org.vosk.** { *; }
-keep interface org.vosk.** { *; }
-keep class com.sun.jna.** { *; }
-dontwarn com.sun.jna.**
