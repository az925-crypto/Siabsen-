# Keep zxing embedded activity
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }
# kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class zaaaam.siabsen.com.**$$serializer { *; }
-keepclassmembers class zaaaam.siabsen.com.** { *** Companion; }
-keepclasseswithmembers class zaaaam.siabsen.com.** { kotlinx.serialization.KSerializer serializer(...); }
