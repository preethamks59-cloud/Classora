# Classora ProGuard Rules

# Prevent R8 from renaming fields in data classes used for JSON serialization (Gson)
# This is critical for maintaining data integrity in offline storage (DataStore)
-keep class com.example.classora.data.** { *; }

# Keep Gson specific classes
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# WorkManager ProGuard Rules
# Fixes java.lang.NoSuchMethodException: androidx.work.impl.WorkDatabase_Impl.<init>
-keep class androidx.work.impl.** { *; }
-keep class * extends androidx.work.impl.WorkDatabase {
    <init>(...);
}

# Room ProGuard Rules (used by WorkManager)
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Maintain line numbers for easier debugging of release builds (optional but recommended)
-keepattributes SourceFile,LineNumberTable

# Standard Android optimization rules are included by default from proguard-android-optimize.txt
