# WalletQ ProGuard / R8 Rules
# Keep Room schema and entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# Keep Hilt generated classes
-keep class * extends dagger.hilt.internal.GeneratedComponent
-keep class * implements dagger.hilt.internal.GeneratedComponent
