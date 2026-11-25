# Add project specific ProGuard rules here.
# Keep Room entities and DAOs
-keep class com.iiab.networkinspector.data.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
