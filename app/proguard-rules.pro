# Optimization & Obfuscation Configuration
-repackageclasses ''
-allowaccessmodification

# Keep line numbers and file names for Play Console crash deobfuscation
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Room Database and Entities
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}

# Keep Data Models & Entities used in Local DB and JSON serialization
-keep class com.example.data.local.** { *; }
-keep class com.example.data.model.** { *; }
-keepclassmembers class com.example.data.local.** {
    <fields>;
    <init>(...);
}
-keepclassmembers class com.example.data.model.** {
    <fields>;
    <init>(...);
}

# Keep OkHttp & JSON
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*

# Keep Firebase Realtime Database and Google Services
-keepclassmembers class * {
    @com.google.firebase.database.PropertyName <fields>;
    @com.google.firebase.database.IgnoreExtraProperties <fields>;
    @com.google.firebase.database.Exclude <fields>;
}

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# Keep WebView JavaScript interfaces (for Adsterra / WebViews)
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class android.webkit.WebView {
   public *;
}
-dontwarn android.webkit.**

# Keep ViewModel constructors for by viewModels() reflection in Release R8 builds
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Keep Adsterra JsBridge explicitly
-keepclassmembers class com.example.adsterra.AdsterraManager$JsBridge {
    *;
}

# Compose Rules
-dontwarn androidx.compose.**

# Coil Image Loader
-dontwarn coil.**
