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
