# Glance instantiates ActionCallback implementations by class name (actionRunCallback<T>()).
-keep class * extends androidx.glance.appwidget.action.ActionCallback { <init>(); }

# Room schema JSON in app/schemas is the source of truth for migrations; entities are
# accessed through generated DAOs, so nothing else needs keeping.

# kotlinx.serialization: keep generated serializers for the seed models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.worddrop.app.**$$serializer { *; }
-keepclassmembers class com.worddrop.app.** { *** Companion; }
-keepclasseswithmembers class com.worddrop.app.** { kotlinx.serialization.KSerializer serializer(...); }
