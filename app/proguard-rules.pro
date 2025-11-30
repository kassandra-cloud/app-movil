# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Reglas de Seguridad para Tesis (Retrofit & Moshi) ---
# Evita que R8/ProGuard rompa los modelos de datos al ofuscar
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Protege tus modelos de datos (DTOs) para que Moshi pueda leerlos
-keep class com.example.proyecto.data.** { *; }

# Protege la conexión con la API
-keep class com.example.proyecto.api.** { *; }

# Reglas especificas para librerías usadas
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class retrofit2.** { *; }