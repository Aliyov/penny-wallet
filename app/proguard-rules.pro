# Penny Wallet R8 rules.
# kotlinx.serialization: keep generated serializers for backup/settings models (the library's own
# rules cover most cases; these make sure nothing used by Backup/UserPrefs is stripped).
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.penny.wallet.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.penny.wallet.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.penny.wallet.data.db.entities.LocalDateSerializer { *; }
# Enum names are persisted (Room converters, backup JSON, settings JSON): never rename them.
-keepclassmembers enum com.penny.wallet.** { *; }

# ML Kit text recognition (receipt scan). Its internals are wired together by reflection
# (component registrars, lazy instance maps, logging/transport); with R8 full mode
# InputImage.fromBitmap crashed with an NPE in release builds only. Keep it all intact.
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
-keep class com.google.android.gms.common.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-keep class com.google.android.datatransport.** { *; }
-keep class com.google.firebase.components.** { *; }
-keep class com.google.firebase.encoders.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**
