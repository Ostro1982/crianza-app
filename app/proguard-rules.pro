# Preservar líneas para stack traces en Crashlytics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Anotaciones
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-dontwarn androidx.room.paging.**

# Entidades del proyecto (reflection via Room)
-keep class com.tudominio.crianza.** { *; }

# ── Firebase / Google Services ──────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception

# ── ML Kit ──────────────────────────────────────────────────────────────────
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.mlkit.**

# ── JavaMail ────────────────────────────────────────────────────────────────
-keep class javax.mail.** { *; }
-keep class com.sun.mail.** { *; }
-keep class javax.activation.** { *; }
-dontwarn javax.mail.**
-dontwarn com.sun.mail.**
-dontwarn javax.activation.**
-dontwarn myjava.awt.datatransfer.**

# ── OkHttp / Okio ───────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ── Kotlin Coroutines ───────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ── Glance Widget ───────────────────────────────────────────────────────────
-keep class androidx.glance.appwidget.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }

# ── Credentials / Google Sign-In ────────────────────────────────────────────
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.** { *; }
-dontwarn androidx.credentials.playservices.**

# ── ZXing / QR ──────────────────────────────────────────────────────────────
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**

# ── Coil ────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── Biometric ───────────────────────────────────────────────────────────────
-keep class androidx.biometric.** { *; }

# ── Compose ─────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Reflection serializable ─────────────────────────────────────────────────
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
-keep @androidx.annotation.Keep class * { *; }
